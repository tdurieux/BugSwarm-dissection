/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.machine.reference.ReferencePnpJobProcessor.JobPlacement.Status;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.FiducialLocator;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.PnpJobProcessor;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.FiniteStateMachine;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.Utils2D;
import org.simpleframework.xml.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Root
public class ReferencePnpJobProcessor implements PnpJobProcessor {
    enum State {
        Uninitialized,
        PreFlight,
        FiducialCheck,
        Plan,
        ChangeNozzleTip,
        Feed,
        Pick,
        Align,
        Place,
        Cleanup,
        Stopped
    }

    enum Message {
        Initialize,
        Next,
        Complete,
        Abort,
        Skip,
        Reset
    }

    public static class JobPlacement {
        public enum Status {
            Pending,
            Processing,
            Skipped,
            Complete
        }

        public final BoardLocation boardLocation;
        public final Placement placement;
        public Status status = Status.Pending;

        public JobPlacement(BoardLocation boardLocation, Placement placement) {
            this.boardLocation = boardLocation;
            this.placement = placement;
        }

        public double getPartHeight() {
            return placement.getPart().getHeight().convertToUnits(LengthUnit.Millimeters)
                    .getValue();
        }

        @Override
        public String toString() {
            return placement.getId();
        }
    }

    public static class PlannedJobPlacement {
        public final JobPlacement jobPlacement;
        public final Nozzle nozzle;
        public Feeder feeder;
        public Location alignmentOffsets;
        public boolean stepComplete;

        public PlannedJobPlacement(Nozzle nozzle, JobPlacement jobPlacement) {
            this.nozzle = nozzle;
            this.jobPlacement = jobPlacement;
        }

        @Override
        public String toString() {
            return nozzle.getName() + " -> " + jobPlacement.toString();
        }
    }

    public static interface Retryable {
        void action() throws Exception;
    }

    private static final Logger logger = LoggerFactory.getLogger(ReferencePnpJobProcessor.class);

    private FiniteStateMachine<State, Message> fsm = new FiniteStateMachine<>(State.Uninitialized);

    protected Job job;

    protected Machine machine;

    protected Head head;

    protected List<JobPlacement> jobPlacements = new ArrayList<>();

    protected List<PlannedJobPlacement> plannedJobPlacements = new ArrayList<>();

    protected Map<BoardLocation, Location> boardLocationFiducialOverrides = new HashMap<>();

    public ReferencePnpJobProcessor() {
        fsm.add(State.Uninitialized, Message.Initialize, State.PreFlight, this::doInitialize);

        fsm.add(State.PreFlight, Message.Next, State.FiducialCheck, this::doPreFlight,
                Message.Next);
        fsm.add(State.PreFlight, Message.Abort, State.Cleanup, Message.Next);

        fsm.add(State.FiducialCheck, Message.Next, State.Plan, this::doFiducialCheck, Message.Next);
        fsm.add(State.FiducialCheck, Message.Skip, State.Plan, Message.Next);
        fsm.add(State.FiducialCheck, Message.Abort, State.Cleanup, Message.Next);

        fsm.add(State.Plan, Message.Next, State.ChangeNozzleTip, this::doPlan, Message.Next);
        fsm.add(State.Plan, Message.Abort, State.Cleanup, Message.Next);
        fsm.add(State.Plan, Message.Complete, State.Cleanup, Message.Next);

        fsm.add(State.ChangeNozzleTip, Message.Next, State.Feed, this::doChangeNozzleTip,
                Message.Next);
        fsm.add(State.ChangeNozzleTip, Message.Skip, State.ChangeNozzleTip, this::doSkip,
                Message.Next);
        fsm.add(State.ChangeNozzleTip, Message.Abort, State.Cleanup, Message.Next);

        fsm.add(State.Feed, Message.Next, State.Pick, this::doFeed, Message.Next);
        fsm.add(State.Feed, Message.Skip, State.Feed, this::doSkip, Message.Next);
        fsm.add(State.Feed, Message.Abort, State.Cleanup, Message.Next);

        fsm.add(State.Pick, Message.Next, State.Align, this::doPick, Message.Next);
        fsm.add(State.Pick, Message.Skip, State.Pick, this::doSkip, Message.Next);
        fsm.add(State.Pick, Message.Abort, State.Cleanup, Message.Next);

        fsm.add(State.Align, Message.Next, State.Place, this::doAlign, Message.Next);
        fsm.add(State.Align, Message.Skip, State.Align, this::doSkip, Message.Next);
        fsm.add(State.Align, Message.Abort, State.Cleanup, Message.Next);

        fsm.add(State.Place, Message.Next, State.Plan, this::doPlace);
        fsm.add(State.Place, Message.Skip, State.Place, this::doSkip, Message.Next);
        fsm.add(State.Place, Message.Abort, State.Cleanup, Message.Next);

        fsm.add(State.Cleanup, Message.Next, State.Stopped, this::doCleanup, Message.Reset);

        fsm.add(State.Stopped, Message.Reset, State.Uninitialized, this::doReset);
    }

    public synchronized void initialize(Job job) throws Exception {
        this.job = job;
        fsm.send(Message.Initialize);
    }

    public synchronized boolean next() throws Exception {
        fsm.send(Message.Next);

        if (fsm.getState() == State.Stopped) {
            /*
             * If we've reached the Stopped state the process is complete. We reset the FSM and
             * return false to indicate that we're finished.
             */
            fsm.send(Message.Reset);
            return false;
        }
        else if (fsm.getState() == State.Plan && isJobComplete()) {
            /*
             * If we've reached the Plan state and there are no more placements to work on the job
             * is complete. We send the Complete Message to start the cleanup process.
             */
            fsm.send(Message.Complete);
            return false;
        }

        return true;
    }

    public synchronized void abort() throws Exception {
        fsm.send(Message.Abort);
    }

    public synchronized void skip() throws Exception {
        fsm.send(Message.Skip);
    }

    public boolean canSkip() {
        return fsm.canSend(Message.Skip);
    }

    /**
     * Validate that there is a job set before allowing it to start.
     * 
     * @throws Exception
     */
    protected void doInitialize() throws Exception {
        if (job == null) {
            throw new Exception("Can't initialize with a null Job.");
        }
    }

    /**
     * Create some internal shortcuts to various buried objects.
     * 
     * Check for obvious setup errors in the job: Feeders are available and enabled, Placements all
     * have valid parts, Parts all have height values set, Each part has at least one compatible
     * nozzle tip.
     * 
     * Populate the jobPlacements list with all the placements that we'll perform for the entire
     * job.
     * 
     * Safe-Z the machine, discard any currently picked parts.
     * 
     * @throws Exception
     */
    protected void doPreFlight() throws Exception {
        // Create some shortcuts for things that won't change during the run
        this.machine = Configuration.get().getMachine();
        this.head = this.machine.getDefaultHead();
        this.jobPlacements.clear();
        this.boardLocationFiducialOverrides.clear();

        for (BoardLocation boardLocation : job.getBoardLocations()) {
            // Only check enabled boards
            if (!boardLocation.isEnabled()) {
                continue;
            }
            for (Placement placement : boardLocation.getBoard().getPlacements()) {
                // Ignore placements that aren't set to be placed
                if (placement.getType() != Placement.Type.Place) {
                    continue;
                }

                // Ignore placements that aren't on the side of the board we're processing.
                if (placement.getSide() != boardLocation.getSide()) {
                    continue;
                }

                JobPlacement jobPlacement = new JobPlacement(boardLocation, placement);

                // Make sure the part is not null
                if (placement.getPart() == null) {
                    throw new Exception(String.format("Part not found for board %s, placement %s.",
                            boardLocation.getBoard().getName(), placement.getId()));
                }

                // Verify that the part height is greater than zero. Catches a common configuration
                // error.
                if (placement.getPart().getHeight().getValue() <= 0D) {
                    throw new Exception(String.format("Part height for %s must be greater than 0.",
                            placement.getPart().getId()));
                }

                // Make sure there is at least one compatible nozzle tip available
                findNozzleTip(placement.getPart());

                // Make sure there is at least one compatible and enabled feeder available
                findFeeder(placement.getPart());

                jobPlacements.add(jobPlacement);
            }
        }

        // Everything looks good, so prepare the machine.

        // Safe Z the machine
        head.moveToSafeZ();
        // Discard any currently picked parts
        discardAll(head);
    }

    protected void doFiducialCheck() throws Exception {
        FiducialLocator locator = Configuration.get().getMachine().getFiducialLocator();
        for (BoardLocation boardLocation : job.getBoardLocations()) {
            if (!boardLocation.isEnabled()) {
                continue;
            }
            if (!boardLocation.isCheckFiducials()) {
                continue;
            }
            Location location = locator.locateBoard(boardLocation);
            boardLocationFiducialOverrides.put(boardLocation, location);
            logger.info("Fiducial check for {}", boardLocation);
        }
    }

    /**
     * Description of the planner:
     * 
     * 1. Create a List<List<JobPlacement>> where each List<JobPlacement> is a List of
     * JobPlacements that the corresponding (in order) Nozzle can handle in Nozzle order.
     * 
     * In addition, each List<JobPlacement> contains one instance of null which represents a
     * solution where that Nozzle does not perform a placement.
     * 
     * 2. Create the Cartesian product of all of the List<JobPlacement>. The resulting List<List
     * <JobPlacement>> represents possible solutions for a single cycle with each JobPlacement
     * corresponding to a Nozzle.
     * 
     * 3. Filter out any solutions where the same JobPlacement is represented more than once. We
     * don't want more than one Nozzle trying to place the same Placement.
     * 
     * 4. Sort the solutions by fewest nulls followed by fewest nozzle changes. The result is that
     * we prefer solutions that use more nozzles in a cycle and require fewer nozzle changes.
     * 
     * Note: TODO: Originally planned to have this sort by part height but that went out the
     * window during development. Need to think about how to best combine the height requirement
     * with the want to fill all nozzles and perform minimal nozzle changes. Based on IRC
     * discussion, the part height thing might be a red herring - most machines will have enough
     * Z to place all parts regardless of height order.
     */
    protected void doPlan() throws Exception {
        plannedJobPlacements.clear();

        // Get the list of unfinished placements and sort them by part height.
        List<JobPlacement> jobPlacements = getPendingJobPlacements().stream()
                .sorted(Comparator.comparing(JobPlacement::getPartHeight))
                .collect(Collectors.toList());

        if (jobPlacements.isEmpty()) {
            return;
        }

        // Create a List of Lists of JobPlacements that each Nozzle can handle, including
        // one instance of null per Nozzle. The null indicates a possible "no solution"
        // for that Nozzle.
        List<List<JobPlacement>> solutions = head.getNozzles().stream().map(nozzle ->
        {
            return Stream.concat(jobPlacements.stream().filter(jobPlacement ->
            {
                return nozzleCanHandle(nozzle, jobPlacement.placement.getPart());
            }), Stream.of((JobPlacement) null)).collect(Collectors.toList());
        }).collect(Collectors.toList());

        // Get the cartesian product of those Lists
        List<JobPlacement> result = cartesianProduct(solutions).stream()
                // Filter out any results that contains the same JobPlacement more than once
                .filter(list ->
                {
                    return new HashSet<JobPlacement>(list).size() == list.size();
                })
                // Sort by the solutions that contain the fewest nulls followed by the
                // solutions that require the fewest nozzle changes.
                .sorted(byFewestNulls.thenComparing(byFewestNozzleChanges))
                // And return the top result.
                .findFirst().orElse(null);

        // Now we have a solution, so apply it to the nozzles and plan the placements.
        for (Nozzle nozzle : head.getNozzles()) {
            // The solution is in Nozzle order, so grab the next one.
            JobPlacement jobPlacement = result.remove(0);
            if (jobPlacement == null) {
                continue;
            }
            jobPlacement.status = Status.Processing;
            plannedJobPlacements.add(new PlannedJobPlacement(nozzle, jobPlacement));
        }

        logger.debug("Planned placements {}", plannedJobPlacements);
    }

    protected void doChangeNozzleTip() throws Exception {
        for (PlannedJobPlacement plannedPlacement : plannedJobPlacements) {
            if (plannedPlacement.stepComplete) {
                continue;
            }

            Nozzle nozzle = plannedPlacement.nozzle;
            JobPlacement jobPlacement = plannedPlacement.jobPlacement;
            Placement placement = jobPlacement.placement;
            Part part = placement.getPart();

            // If the currently loaded NozzleTip can handle the Part we're good.
            if (nozzle.getNozzleTip() != null && nozzle.getNozzleTip().canHandle(part)) {
                logger.debug("No nozzle change needed for nozzle {}", nozzle.getName());
                plannedPlacement.stepComplete = true;
                continue;
            }

            // Otherwise find a compatible tip and load it
            NozzleTip nozzleTip = findNozzleTip(nozzle, part);
            nozzle.unloadNozzleTip();
            nozzle.loadNozzleTip(nozzleTip);

            // Mark this step as complete
            plannedPlacement.stepComplete = true;
        }

        clearStepComplete();
    }

    protected void doFeed() throws Exception {
        for (PlannedJobPlacement plannedPlacement : plannedJobPlacements) {
            if (plannedPlacement.stepComplete) {
                continue;
            }
            Nozzle nozzle = plannedPlacement.nozzle;
            JobPlacement jobPlacement = plannedPlacement.jobPlacement;
            Placement placement = jobPlacement.placement;
            Part part = placement.getPart();

            while (true) {
                // Find a compatible, enabled feeder
                Feeder feeder = findFeeder(part);
                plannedPlacement.feeder = feeder;

                // Feed the part
                try {
                    // Try to feed the part. If it fails, retry the specified number of times before
                    // giving up.
                    retry(1 + feeder.getRetryCount(), () ->
                    {
                        logger.info("Attempt Feed {} from {} with {}.",
                                new Object[] {part.getId(), feeder.getName(), nozzle.getName()});

                        feeder.feed(nozzle);

                        logger.info("Fed {} from {} with {}.",
                                new Object[] {part.getId(), feeder.getName(), nozzle.getName()});
                    });

                    break;
                }
                catch (Exception e) {
                    logger.info("Feed {} from {} with {} failed!",
                            new Object[] {part.getId(), feeder.getName(), nozzle.getName()});
                    // If the feed fails, disable the feeder and continue. If there are no
                    // more valid feeders the findFeeder() call above will throw and exit the
                    // loop.
                    feeder.setEnabled(false);
                }
            }
            plannedPlacement.stepComplete = true;
        }

        clearStepComplete();
    }

    protected void doPick() throws Exception {
        for (PlannedJobPlacement plannedPlacement : plannedJobPlacements) {
            if (plannedPlacement.stepComplete) {
                continue;
            }
            Nozzle nozzle = plannedPlacement.nozzle;
            JobPlacement jobPlacement = plannedPlacement.jobPlacement;
            Placement placement = jobPlacement.placement;
            Part part = placement.getPart();

            // Get the feeder that was used to feed
            Feeder feeder = plannedPlacement.feeder;

            // Move to the pick location
            MovableUtils.moveToLocationAtSafeZ(nozzle, feeder.getPickLocation());

            // Pick
            nozzle.pick(part);

            // Retract
            nozzle.moveToSafeZ();

            logger.info("Pick {} from {} with {}",
                    new Object[] {part.getId(), feeder.getName(), nozzle.getName()});

            plannedPlacement.stepComplete = true;
        }

        clearStepComplete();
    }

    protected void doAlign() throws Exception {
        for (PlannedJobPlacement plannedPlacement : plannedJobPlacements) {
            if (plannedPlacement.stepComplete) {
                continue;
            }
            Nozzle nozzle = plannedPlacement.nozzle;
            JobPlacement jobPlacement = plannedPlacement.jobPlacement;
            Placement placement = jobPlacement.placement;
            Part part = placement.getPart();
            
            Location alignmentOffsets = machine.getPartAlignment().findOffsets(part, nozzle);
            plannedPlacement.alignmentOffsets = alignmentOffsets;

            logger.info("Align {} with {}", part.getId(), nozzle.getName());

            plannedPlacement.stepComplete = true;
        }

        clearStepComplete();
    }

    protected void doPlace() throws Exception {
        for (PlannedJobPlacement plannedPlacement : plannedJobPlacements) {
            if (plannedPlacement.stepComplete) {
                continue;
            }
            Nozzle nozzle = plannedPlacement.nozzle;
            JobPlacement jobPlacement = plannedPlacement.jobPlacement;
            Placement placement = jobPlacement.placement;
            Part part = placement.getPart();
            BoardLocation boardLocation = plannedPlacement.jobPlacement.boardLocation;

            // Check if there is a fiducial override for the board location and if so, use it.
            if (boardLocationFiducialOverrides.containsKey(boardLocation)) {
                BoardLocation boardLocation2 = new BoardLocation(boardLocation.getBoard());
                boardLocation2.setSide(boardLocation.getSide());
                boardLocation2.setLocation(boardLocationFiducialOverrides.get(boardLocation));
                boardLocation = boardLocation2;
            }
            Location placementLocation =
                    Utils2D.calculateBoardPlacementLocation(boardLocation, placement.getLocation());

            // If there are alignment offsets update the placement location with them
            if (plannedPlacement.alignmentOffsets != null) {
                Location alignmentOffsets = plannedPlacement.alignmentOffsets;
                // Rotate the point 0,0 using the alignment offsets as a center point by the angle
                // that is
                // the difference between the alignment angle and the calculated global
                // placement angle.
                Location location =
                        new Location(LengthUnit.Millimeters).rotateXyCenterPoint(alignmentOffsets,
                                placementLocation.getRotation() - alignmentOffsets.getRotation());

                // Set the angle to the difference mentioned above, aligning the part to the
                // same angle as
                // the placement.
                location = location.derive(null, null, null,
                        placementLocation.getRotation() - alignmentOffsets.getRotation());

                // Add the placement final location to move our local coordinate into global
                // space
                location = location.add(placementLocation);

                // Subtract the alignment offsets to move the part to the final location,
                // instead of
                // the nozzle.
                location = location.subtract(alignmentOffsets);

                placementLocation = location;
            }

            // Add the part's height to the placement location
            placementLocation = placementLocation.add(new Location(part.getHeight().getUnits(), 0,
                    0, part.getHeight().getValue(), 0));

            // Move to the placement location
            MovableUtils.moveToLocationAtSafeZ(nozzle, placementLocation);

            // Place the part
            nozzle.place();

            // Retract
            nozzle.moveToSafeZ();

            // Mark the placement as finished
            jobPlacement.status = Status.Complete;

            plannedPlacement.stepComplete = true;

            logger.info("Place {} with {}", part.getId(), nozzle.getName());
        }

        clearStepComplete();
    }

    protected void doCleanup() throws Exception {
        // Safe Z the machine
        head.moveToSafeZ();
        // Discard any currently picked parts
        discardAll(head);
        // Home the machine
        // TODO: Move to park position instead.
        // https://github.com/openpnp/openpnp/issues/76
        machine.home();
    }

    protected void doReset() throws Exception {
        this.job = null;
    }

    /**
     * Discard the picked part, if any. Remove the currently processing PlannedJobPlacement from the
     * list and mark the JobPlacement as Skipped.
     * 
     * @throws Exception
     */
    protected void doSkip() throws Exception {
        if (plannedJobPlacements.size() > 0) {
            PlannedJobPlacement plannedJobPlacement = plannedJobPlacements.remove(0);
            JobPlacement jobPlacement = plannedJobPlacement.jobPlacement;
            Nozzle nozzle = plannedJobPlacement.nozzle;
            discard(nozzle);
            jobPlacement.status = Status.Skipped;
            logger.debug("Skipped {}", jobPlacement.placement);
        }
    }

    protected void clearStepComplete() {
        for (PlannedJobPlacement plannedPlacement : plannedJobPlacements) {
            plannedPlacement.stepComplete = false;
        }
    }

    protected List<JobPlacement> getPendingJobPlacements() {
        return this.jobPlacements.stream().filter((jobPlacement) ->
        {
            return jobPlacement.status == Status.Pending;
        }).collect(Collectors.toList());
    }

    protected boolean isJobComplete() {
        return getPendingJobPlacements().isEmpty();
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName();
    }

    /**
     * Find the first NozzleTip that is able to handle the given Part.
     * 
     * @param part
     * @return
     * @throws Exception If no compatible NozzleTip can be found.
     */
    protected NozzleTip findNozzleTip(Part part) throws Exception {
        for (Nozzle nozzle : head.getNozzles()) {
            try {
                return findNozzleTip(nozzle, part);
            }
            catch (Exception e) {
            }
        }
        throw new Exception(
                "No compatible nozzle tip on any nozzle found for part " + part.getId());
    }

    /**
     * Find the first enabled Feeder is that is able to feed the given Part.
     * 
     * @param part
     * @return
     * @throws Exception If no Feeder is found that is both enabled and is serving the Part.
     */
    protected Feeder findFeeder(Part part) throws Exception {
        for (Feeder feeder : machine.getFeeders()) {
            if (feeder.getPart() == part && feeder.isEnabled()) {
                return feeder;
            }
        }
        throw new Exception("No compatible, enabled feeder found for part " + part.getId());
    }

    // Sort a List<JobPlacement> by the number of nulls it contains in ascending order.
    Comparator<List<JobPlacement>> byFewestNulls = (a, b) ->
    {
        return Collections.frequency(a, null) - Collections.frequency(b, null);
    };

    // Sort a List<JobPlacement> by the number of nozzle changes it will require in
    // descending order.
    Comparator<List<JobPlacement>> byFewestNozzleChanges = (a, b) ->
    {
        int countA = 0, countB = 0;
        for (int i = 0; i < head.getNozzles().size(); i++) {
            Nozzle nozzle = head.getNozzles().get(i);
            JobPlacement jpA = a.get(i);
            JobPlacement jpB = b.get(i);
            if (nozzle.getNozzleTip() == null) {
                countA++;
                countB++;
                continue;
            }
            if (jpA != null && !nozzle.getNozzleTip().canHandle(jpA.placement.getPart())) {
                countA++;
            }
            if (jpB != null && !nozzle.getNozzleTip().canHandle(jpB.placement.getPart())) {
                countB++;
            }
        }
        return countA - countB;
    };

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return null;
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        return null;
    }

    /**
     * Call the Retryable's action method until it either does not throw an Exception or it is
     * called maxTries number of times. If the method throws an Exception each time then this method
     * will throw the final Exception.
     * 
     * @param maxTries
     * @param r
     * @throws Exception
     */
    public static void retry(int maxTries, Retryable r) throws Exception {
        for (int i = 0; i < maxTries; i++) {
            try {
                r.action();
                break;
            }
            catch (Exception e) {
                if (i == maxTries - 1) {
                    throw e;
                }
            }
        }
    }

    public static void discardAll(Head head) throws Exception {
        for (Nozzle nozzle : head.getNozzles()) {
            discard(nozzle);
        }
    }

    /**
     * Discard the Part, if any, on the given Nozzle. the Nozzle is returned to Safe Z at the end of
     * the operation.
     * 
     * @param nozzle
     * @throws Exception
     */
    public static void discard(Nozzle nozzle) throws Exception {
        if (nozzle.getPart() == null) {
            return;
        }
        logger.debug("Discard {} from {}", nozzle.getPart(), nozzle);
        // move to the discard location
        MovableUtils.moveToLocationAtSafeZ(nozzle,
                Configuration.get().getMachine().getDiscardLocation());
        // discard the part
        nozzle.place();
        nozzle.moveToSafeZ();
    }

    public static NozzleTip findNozzleTip(Nozzle nozzle, Part part) throws Exception {
        for (NozzleTip nozzleTip : nozzle.getNozzleTips()) {
            if (nozzleTip.canHandle(part)) {
                return nozzleTip;
            }
        }
        throw new Exception("No compatible nozzle tip on nozzle " + nozzle.getName()
                + " found for part " + part.getId());
    }

    public static boolean nozzleCanHandle(Nozzle nozzle, Part part) {
        for (NozzleTip nozzleTip : nozzle.getNozzleTips()) {
            if (nozzleTip.canHandle(part)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create the cartesian product of a list of lists. The results will contain every possible
     * distinct combination of the elements of the input lists.
     * 
     * Example: cartesianProduct(Arrays.asList(Arrays.asList("A", "B"), Arrays.asList("1", "2")))
     * [[A, 1], [A, 2], [B, 1], [B, 2]]
     * 
     * This method specifically allows for nulls in the input elements. Multiple nulls will be
     * counted multiple times.
     * 
     * @param lists
     * @return
     */
    public static <T> List<List<T>> cartesianProduct(List<List<T>> lists) {
        List<List<T>> results = new ArrayList<>();
        int[] indexes = new int[lists.size()];
        while (indexes[0] < lists.get(0).size()) {
            // Scan across the columns, adding the current element from each list to the current
            // row and then add the row to the results.
            List<T> result = new ArrayList<>();
            for (int column = 0; column < lists.size(); column++) {
                result.add(lists.get(column).get(indexes[column]));
            }
            results.add(result);
            // Increment the column indexes starting from the right. If a column has reached it's
            // limit, reset it to zero and increment the next one to the left, carrying to
            // the beginning as needed.
            for (int i = indexes.length - 1; i >= 0; i--) {
                indexes[i]++;
                if (indexes[i] < lists.get(i).size() || i == 0) {
                    break;
                }
                indexes[i] = 0;
            }
        }
        return results;
    }
}
