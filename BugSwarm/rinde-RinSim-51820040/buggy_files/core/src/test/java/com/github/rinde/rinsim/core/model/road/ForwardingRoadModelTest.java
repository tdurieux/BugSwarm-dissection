/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.core.model.road;

import java.util.Arrays;
import java.util.Collection;

import javax.measure.Measure;
import javax.measure.unit.SI;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.github.rinde.rinsim.core.model.road.ForwardingRoadModel;
import com.github.rinde.rinsim.core.model.road.GenericRoadModel;
import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.TestMultimapGraph;

/**
 * @author Rinde van Lon 
 * 
 */
@RunWith(Parameterized.class)
public class ForwardingRoadModelTest extends
    AbstractRoadModelTest<GenericRoadModel> {
  @Parameters
  public static Collection<Object[]> configs() {
    return Arrays.asList(new Object[][] //
        { { new Creator() {
          @Override
          public GenericRoadModel create(ForwardingRoadModelTest testClass) {
            return new ForwardingRoadModel(new PlaneRoadModel(new Point(0, 0),
                new Point(10, 10), SI.METER, Measure.valueOf(10d,
                    SI.METERS_PER_SECOND)));
          }
        } }, { new Creator() {
          @Override
          public GenericRoadModel create(ForwardingRoadModelTest testClass) {
            return new ForwardingRoadModel(new GraphRoadModel(testClass
                .createGraph(), SI.METER, SI.METERS_PER_SECOND));
          }
        } }, { new Creator() {
          @Override
          public GenericRoadModel create(ForwardingRoadModelTest testClass) {
            return new ForwardingRoadModel(new ForwardingRoadModel(
                new ForwardingRoadModel(new GraphRoadModel(testClass
                    .createGraph(), SI.METER, SI.METERS_PER_SECOND))));
          }
        } } });
  }

  Graph<?> createGraph() {
    final Graph<?> g = new TestMultimapGraph();
    g.addConnection(SW, SE);
    g.addConnection(SE, NE);
    g.addConnection(NE, NW);
    return g;
  }

  interface Creator {
    GenericRoadModel create(ForwardingRoadModelTest testClass);
  }

  final Creator creator;

  public ForwardingRoadModelTest(Creator c) {
    creator = c;
  }

  @Override
  public void setUp() throws Exception {
    model = creator.create(this);
  }
}
