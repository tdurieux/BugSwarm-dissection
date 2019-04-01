package de.thm.arsnova.entities.migration;

import de.thm.arsnova.entities.ChoiceQuestionContent;
import de.thm.arsnova.entities.UserProfile;
import de.thm.arsnova.entities.migration.v2.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ToV2Migrator {
	private void copyCommonProperties(final de.thm.arsnova.entities.Entity from, final Entity to) {
		to.setId(from.getId());
		to.setRevision(from.getRevision());
	}

	public LoggedIn migrateLoggedIn(final UserProfile from) {
		final LoggedIn to = new LoggedIn();
		copyCommonProperties(from, to);
		to.setUser(from.getLoginId());
		to.setTimestamp(from.getLastLoginTimestamp().getTime());
		to.setVisitedSessions(from.getRoomHistory().stream()
				.map(entry -> new VisitedRoom())
				.collect(Collectors.toList()));

		return to;
	}

	public MotdList migrateMotdList(final UserProfile from) {
		final MotdList to = new MotdList();
		copyCommonProperties(from, to);
		to.setUsername(from.getLoginId());
		to.setMotdkeys(String.join(",", from.getAcknowledgedMotds()));

		return to;
	}

	public Room migrate(final de.thm.arsnova.entities.Room from, final Optional<UserProfile> owner) {
		final Room to = new Room();
		copyCommonProperties(from, to);
		to.setKeyword(from.getShortId());
		if (owner.isPresent()) {
			to.setCreator(owner.get().getLoginId());
		}
		to.setName(from.getName());
		to.setShortName(from.getAbbreviation());
		to.setActive(!from.isClosed());

		return to;
	}

	public Room migrate(final de.thm.arsnova.entities.Room from) {
		return migrate(from, Optional.empty());
	}

	public RoomFeature migrate(final de.thm.arsnova.entities.Room.Settings settings) {
		RoomFeature feature = new RoomFeature();
		feature.setInterposed(settings.isCommentsEnabled());
		feature.setLecture(settings.isQuestionsEnabled());
		feature.setJitt(settings.isQuestionsEnabled());
		feature.setSlides(settings.isSlidesEnabled());
		feature.setFlashcard(settings.isFlashcardsEnabled());
		feature.setFeedback(settings.isQuickSurveyEnabled());
		feature.setPi(settings.isMultipleRoundsEnabled() || settings.isTimerEnabled());
		feature.setLearningProgress(settings.isScoreEnabled());

		int count = 0;
		/* Single-feature use cases can be migrated */
		if (settings.isCommentsEnabled()) {
			feature.setTwitterWall(true);
			count++;
		}
		if (settings.isFlashcardsEnabled()) {
			feature.setFlashcardFeature(true);
			count++;
		}
		if (settings.isQuickSurveyEnabled()) {
			feature.setLiveClicker(true);
			count++;
		}
		/* For the following features an exact migration is not possible, so custom is set */
		if (settings.isQuestionsEnabled()) {
			feature.setCustom(true);
			count++;
		}
		if (settings.isSlidesEnabled()) {
			feature.setCustom(true);
			count++;
		}
		if (settings.isMultipleRoundsEnabled() || settings.isTimerEnabled()) {
			feature.setCustom(true);
			count++;
		}
		if (settings.isScoreEnabled()) {
			feature.setCustom(true);
			count++;
		}

		if (count != 1) {
			/* Reset single-feature use-cases since multiple features were detected */
			feature.setTwitterWall(false);
			feature.setFlashcardFeature(false);
			feature.setLiveClicker(false);

			if (count == 7) {
				feature.setCustom(false);
				feature.setTotal(true);
			} else {
				feature.setCustom(true);
			}
		}

		return feature;
	}

	public Content migrate(final de.thm.arsnova.entities.Content from) {
		final Content to = new Content();
		copyCommonProperties(from, to);
		to.setSessionId(from.getRoomId());
		to.setSubject(from.getSubject());
		to.setText(from.getBody());
		to.setQuestionVariant(from.getGroup());

		if (from instanceof ChoiceQuestionContent) {
			final ChoiceQuestionContent fromChoiceQuestionContent = (ChoiceQuestionContent) from;
			switch (from.getFormat()) {
				case CHOICE:
					to.setQuestionType(fromChoiceQuestionContent.isMultiple() ? "mc" : "abcd");
					break;
				case BINARY:
					to.setQuestionType("yesno");
					break;
				case SCALE:
					to.setQuestionType("vote");
					break;
				case GRID:
					to.setQuestionType("grid");
					break;
			}
			final List<AnswerOption> toOptions = new ArrayList<>();
			to.setPossibleAnswers(toOptions);
			for (int i = 0; i < fromChoiceQuestionContent.getOptions().size(); i++) {
				AnswerOption option = new AnswerOption();
				option.setText(fromChoiceQuestionContent.getOptions().get(1).getLabel());
				option.setValue(fromChoiceQuestionContent.getOptions().get(1).getPoints());
				option.setCorrect(fromChoiceQuestionContent.getCorrectOptionIndexes().contains(i));
				toOptions.add(option);
			}
		} else {
			switch (from.getFormat()) {
				case NUMBER:
					to.setQuestionType("freetext");
					break;
				case TEXT:
					to.setQuestionType("freetext");
					break;
			}
		}

		return to;
	}

	public Answer migrate(final de.thm.arsnova.entities.ChoiceAnswer from,
			final de.thm.arsnova.entities.ChoiceQuestionContent content, final Optional<UserProfile> creator) {
		final Answer to = new Answer();
		copyCommonProperties(from, to);
		to.setQuestionId(from.getContentId());
		if (creator.isPresent()) {
			to.setUser(creator.get().getLoginId());
		}

		List<String> answers = new ArrayList<>();
		for (int i = 0; i < content.getOptions().size(); i++) {
			answers.add(from.getSelectedChoiceIndexes().contains(i) ? "1" : "0");
		}
		to.setAnswerText(answers.stream().collect(Collectors.joining()));

		return to;
	}

	public Answer migrate(final de.thm.arsnova.entities.ChoiceAnswer from,
			final de.thm.arsnova.entities.ChoiceQuestionContent content) {
		return migrate(from, content, Optional.empty());
	}

	public Answer migrate(final de.thm.arsnova.entities.TextAnswer from,
			final Optional<de.thm.arsnova.entities.Content> content, final Optional<UserProfile> creator) {
		final Answer to = new Answer();
		copyCommonProperties(from, to);
		to.setQuestionId(from.getContentId());
		if (creator.isPresent()) {
			to.setUser(creator.get().getLoginId());
		}

		to.setAnswerSubject(from.getSubject());
		to.setAnswerText(from.getBody());

		return to;
	}

	public Answer migrate(final de.thm.arsnova.entities.TextAnswer from) {
		return migrate(from, Optional.empty(), Optional.empty());
	}

	public Comment migrate(final de.thm.arsnova.entities.Comment from, final Optional<UserProfile> creator) {
		final Comment to = new Comment();
		copyCommonProperties(from, to);
		to.setSessionId(from.getRoomId());
		if (creator.isPresent()) {
			to.setCreator(creator.get().getLoginId());
		}
		to.setSubject(from.getSubject());
		to.setText(from.getBody());
		to.setTimestamp(from.getTimestamp().getTime());
		to.setRead(from.isRead());

		return to;
	}

	public Comment migrate(final de.thm.arsnova.entities.Comment from) {
		return migrate(from, Optional.empty());
	}

	public Motd migrate(final de.thm.arsnova.entities.Motd from) {
		final Motd to = new Motd();
		copyCommonProperties(from, to);
		to.setStartdate(from.getCreationTimestamp());
		to.setStartdate(from.getStartDate());
		to.setEnddate(from.getEndDate());
		switch (from.getAudience()) {
			case ALL:
				to.setAudience("all");
				break;
			case AUTHORS:
				to.setAudience("tutors");
				break;
			case PARTICIPANTS:
				to.setAudience("students");
				break;
			case ROOM:
				to.setAudience("session");
				break;
		}
		to.setTitle(from.getTitle());
		to.setText(from.getBody());
		to.setSessionId(from.getRoomId());

		return to;
	}
}
