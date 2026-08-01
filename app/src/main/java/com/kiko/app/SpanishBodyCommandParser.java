package com.kiko.app;

import java.text.Normalizer;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SpanishBodyCommandParser {
    enum Issue {
        MISSING_STEP_COUNT,
        INVALID_STEP_COUNT
    }

    static final class Result {
        private final BodyActionRequest action;
        private final Issue issue;

        private Result(BodyActionRequest action, Issue issue) {
            this.action = action;
            this.issue = issue;
        }

        static Result action(BodyActionRequest action) {
            return new Result(action, null);
        }

        static Result clarification(Issue issue) {
            return new Result(null, issue);
        }

        BodyActionRequest getAction() {
            return action;
        }

        Issue getIssue() {
            return issue;
        }

        boolean hasAction() {
            return action != null;
        }
    }

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Pattern WAKE_PREFIX = Pattern.compile("^(?:kiko|quico|quiko)\\s+");
    private static final Pattern STOP = Pattern.compile(
            "^(?:para|parate|detente|deten el movimiento|deten la simulacion)$"
    );
    private static final Pattern DANCE = Pattern.compile(
            "^(?:baila|haz un baile|haz el baile)$"
    );
    private static final Pattern MOVE = Pattern.compile(
            "^(?:da|dame|camina|avanza)\\s+([\\p{L}0-9]+)\\s+pasos?$"
    );
    private static final Pattern MOVE_PREFIX = Pattern.compile(
            "^(?:da|dame|camina|avanza)(?:\\s+.*)?$"
    );

    private SpanishBodyCommandParser() {
    }

    static Result parse(String text) {
        return parse(text == null ? null : Collections.singletonList(text));
    }

    static Result parse(List<String> hypotheses) {
        if (hypotheses == null) {
            return null;
        }

        Result clarification = null;
        for (String hypothesis : hypotheses) {
            if (hypothesis == null) {
                continue;
            }
            String normalized = stripWakeWord(normalize(hypothesis));
            if (STOP.matcher(normalized).matches()) {
                return Result.action(BodyActionRequest.stop());
            }
            if (DANCE.matcher(normalized).matches()) {
                return Result.action(BodyActionRequest.dance());
            }

            Matcher move = MOVE.matcher(normalized);
            if (move.matches()) {
                Integer count = SpanishNumberParser.parse(move.group(1));
                if (count != null) {
                    return Result.action(BodyActionRequest.moveSteps(count));
                }
                clarification = Result.clarification(Issue.INVALID_STEP_COUNT);
                continue;
            }

            if (MOVE_PREFIX.matcher(normalized).matches()
                    && (normalized.contains("paso")
                    || normalized.equals("camina")
                    || normalized.equals("avanza"))) {
                Issue issue = normalized.matches(".*\\d.*")
                        ? Issue.INVALID_STEP_COUNT
                        : Issue.MISSING_STEP_COUNT;
                clarification = Result.clarification(issue);
            }
        }
        return clarification;
    }

    static boolean containsEmergencyStop(List<String> hypotheses) {
        if (hypotheses == null) {
            return false;
        }
        for (String hypothesis : hypotheses) {
            Result result = parse(hypothesis);
            if (result != null
                    && result.hasAction()
                    && result.getAction().getType() == BodyActionRequest.Type.STOP) {
                return true;
            }
        }
        return false;
    }

    private static String stripWakeWord(String normalized) {
        return WAKE_PREFIX.matcher(normalized).replaceFirst("").trim();
    }

    private static String normalize(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        normalized = DIACRITICS.matcher(normalized).replaceAll("");
        normalized = normalized.toLowerCase(Locale.ROOT);
        normalized = NON_ALPHANUMERIC.matcher(normalized).replaceAll(" ");
        return normalized.trim();
    }
}
