package com.kiko.app;

import java.util.ArrayList;
import java.util.List;

public final class SpanishPetMemoryResponses {
    private SpanishPetMemoryResponses() {
    }

    public static String updateResponse(PetMemoryCommand command) {
        switch (command.getType()) {
            case REGISTER:
                if (command.getOwnerName() == null) {
                    return "Recordaré que " + command.getPetName() + " es "
                            + command.getKind().getIndefinitePhrase() + ".";
                }
                return "Recordaré que " + command.getPetName() + " es "
                        + command.getKind().getDefinitePhrase() + " de "
                        + command.getOwnerName() + ".";
            case SET_FAVORITE_FOOD:
                return "¡A mí también me gusta " + command.getTextValue() + "!";
            case ADD_LIKE:
                return "Recordaré que a " + command.getPetName()
                        + " le gusta " + command.getTextValue() + ".";
            case SET_AGE:
                return "Recordaré que " + command.getPetName() + " tiene "
                        + command.getNumberValue() + " años.";
            default:
                throw new IllegalArgumentException("Command is not a pet update");
        }
    }

    public static String queryResponse(
            PetMemoryCommand command,
            PetMemoryRecord record,
            List<PetMemoryRecord> ownerPets
    ) {
        switch (command.getType()) {
            case QUERY_FAVORITE_FOOD:
                if (record == null || record.getFavoriteFood() == null) {
                    return "No sé cuál es la comida favorita de "
                            + command.getPetName() + ".";
                }
                return capitalize(record.getFavoriteFood()) + ".";
            case QUERY_LIKES:
                List<String> likes = combinedLikes(record);
                if (likes.isEmpty()) {
                    return "No sé qué le gusta a " + command.getPetName()
                            + " todavía.";
                }
                return "A " + record.getDisplayName() + " le gusta "
                        + join(likes) + ".";
            case QUERY_SUMMARY:
                List<String> facts = factClauses(record);
                if (facts.isEmpty()) {
                    return "No sé nada de " + command.getPetName()
                            + " todavía.";
                }
                return "Sé que " + record.getDisplayName() + " "
                        + join(facts) + ".";
            case QUERY_OWNER_PETS:
                if (ownerPets == null || ownerPets.isEmpty()) {
                    return "No sé qué mascotas tiene " + command.getOwnerName()
                            + " todavía.";
                }
                List<String> pets = new ArrayList<>();
                for (PetMemoryRecord pet : ownerPets) {
                    pets.add(pet.getKind().getIndefinitePhrase()
                            + " " + pet.getKind().getCalledWord() + " "
                            + pet.getDisplayName());
                }
                return command.getOwnerName() + " tiene " + join(pets) + ".";
            default:
                throw new IllegalArgumentException("Command is not a pet query");
        }
    }

    public static String inspectableSummary(PetMemoryRecord record) {
        List<String> facts = factClauses(record);
        return capitalize(record.getDisplayName() + " " + join(facts)) + ".";
    }

    private static List<String> combinedLikes(PetMemoryRecord record) {
        List<String> values = new ArrayList<>();
        if (record == null) {
            return values;
        }
        if (record.getFavoriteFood() != null) {
            values.add(record.getFavoriteFood());
        }
        for (String like : record.getLikes()) {
            String normalized = PetMemoryRecord.normalizeFact(like);
            boolean duplicate = values.stream().anyMatch(value ->
                    PetMemoryRecord.normalizeFact(value).equals(normalized));
            if (!duplicate) {
                values.add(like);
            }
        }
        return values;
    }

    private static List<String> factClauses(PetMemoryRecord record) {
        List<String> facts = new ArrayList<>();
        if (record == null) {
            return facts;
        }
        String identity = "es " + record.getKind().getDefinitePhrase();
        if (record.getDisplayOwnerName() != null) {
            identity += " de " + record.getDisplayOwnerName();
        }
        facts.add(identity);
        if (record.getFavoriteFood() != null) {
            facts.add("le gusta " + record.getFavoriteFood());
        }
        for (String like : record.getLikes()) {
            if (record.getFavoriteFood() == null
                    || !PetMemoryRecord.normalizeFact(like).equals(
                            PetMemoryRecord.normalizeFact(record.getFavoriteFood())
                    )) {
                facts.add("le gusta " + like);
            }
        }
        if (record.getAge() != null) {
            facts.add("tiene " + record.getAge() + " años");
        }
        return facts;
    }

    private static String join(List<String> values) {
        if (values.isEmpty()) {
            return "";
        }
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
