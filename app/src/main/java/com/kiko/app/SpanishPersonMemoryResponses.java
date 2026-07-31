package com.kiko.app;

import java.util.ArrayList;
import java.util.List;

public final class SpanishPersonMemoryResponses {
    private SpanishPersonMemoryResponses() {
    }

    public static String updateResponse(PersonMemoryCommand command) {
        switch (command.getType()) {
            case SET_FAVORITE_FOOD:
                return "¡A mí también me gusta " + command.getTextValue() + "!";
            case ADD_LIKE:
                return "Recordaré que a " + command.getPersonName()
                        + " le gusta " + command.getTextValue() + ".";
            case SET_AGE:
                return "Recordaré que " + command.getPersonName() + " tiene "
                        + command.getNumberValue() + " años.";
            default:
                throw new IllegalArgumentException("Command is not an update");
        }
    }

    public static String queryResponse(
            PersonMemoryCommand command,
            PersonMemoryRecord record
    ) {
        switch (command.getType()) {
            case QUERY_FAVORITE_FOOD:
                if (record == null || record.getFavoriteFood() == null) {
                    return "No sé cuál es la comida favorita de "
                            + command.getPersonName() + ".";
                }
                return capitalize(record.getFavoriteFood()) + ".";
            case QUERY_LIKES:
                List<String> likes = combinedLikes(record);
                if (likes.isEmpty()) {
                    return "No sé qué le gusta a " + command.getPersonName()
                            + " todavía.";
                }
                return "A " + record.getDisplayName() + " le gusta "
                        + join(likes) + ".";
            case QUERY_SUMMARY:
                List<String> facts = factClauses(record);
                if (facts.isEmpty()) {
                    return "No sé nada de " + command.getPersonName()
                            + " todavía.";
                }
                return "Sé que " + join(facts) + ".";
            default:
                throw new IllegalArgumentException("Command is not a query");
        }
    }

    public static String inspectableSummary(PersonMemoryRecord record) {
        List<String> facts = factClauses(record);
        return facts.isEmpty() ? "Sin datos." : capitalize(join(facts)) + ".";
    }

    private static List<String> combinedLikes(PersonMemoryRecord record) {
        List<String> values = new ArrayList<>();
        if (record == null) {
            return values;
        }
        if (record.getFavoriteFood() != null) {
            values.add(record.getFavoriteFood());
        }
        for (String like : record.getLikes()) {
            String normalized = PersonMemoryRecord.normalizeFact(like);
            boolean duplicate = values.stream().anyMatch(value ->
                    PersonMemoryRecord.normalizeFact(value).equals(normalized));
            if (!duplicate) {
                values.add(like);
            }
        }
        return values;
    }

    private static List<String> factClauses(PersonMemoryRecord record) {
        List<String> facts = new ArrayList<>();
        if (record == null) {
            return facts;
        }
        if (record.getFavoriteFood() != null) {
            facts.add("le gusta " + record.getFavoriteFood());
        }
        for (String like : record.getLikes()) {
            if (record.getFavoriteFood() == null
                    || !PersonMemoryRecord.normalizeFact(like).equals(
                            PersonMemoryRecord.normalizeFact(
                                    record.getFavoriteFood()
                            ))) {
                facts.add("le gusta " + like);
            }
        }
        if (record.getAge() != null) {
            facts.add("tiene " + record.getAge() + " años");
        }
        return facts;
    }

    private static String join(List<String> values) {
        if (values.size() == 1) {
            return values.get(0);
        }
        if (values.size() == 2) {
            return values.get(0) + " y " + values.get(1);
        }
        return String.join(", ", values.subList(0, values.size() - 1))
                + " y " + values.get(values.size() - 1);
    }

    private static String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
