package com.kiko.app;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SpanishPetMemoryParser {
    private static final int MAX_AGE = 40;
    private static final String NAME_CAPTURE =
            "([\\p{L}]+(?:[-'][\\p{L}]+)?"
                    + "(?:\\s+[\\p{L}]+(?:[-'][\\p{L}]+)?){0,2})";
    private static final String DEFINITE_KIND =
            "(el gato|la gata|el perro|la perra)";
    private static final String INDEFINITE_KIND =
            "(un gato|una gata|un perro|una perra)";
    private static final Pattern WAKE_PREFIX = Pattern.compile(
            "(?iu)^(?:kiko|quico|quiko)\\b[\\s,;:.-]*"
    );
    private static final Pattern FAVORITE_FOOD_QUERY = Pattern.compile(
            "(?iu)^(?:cu[aá]l es )?la comida favorita de "
                    + DEFINITE_KIND + " " + NAME_CAPTURE + "$"
    );
    private static final Pattern LIKES_QUERY = Pattern.compile(
            "(?iu)^qu[eé](?: cosas?)? le gustan? a "
                    + DEFINITE_KIND + " " + NAME_CAPTURE + "$"
    );
    private static final Pattern SUMMARY_QUERY = Pattern.compile(
            "(?iu)^qu[eé] sabes de " + DEFINITE_KIND + " "
                    + NAME_CAPTURE + "$"
    );
    private static final Pattern OWNER_PETS_QUERY = Pattern.compile(
            "(?iu)^(?:qu[eé] mascotas? tiene|cu[aá]les son las mascotas de) "
                    + NAME_CAPTURE + "$"
    );
    private static final Pattern FAVORITE_FOOD_UPDATE = Pattern.compile(
            "(?iu)^la comida favorita de " + DEFINITE_KIND + " "
                    + NAME_CAPTURE + " es (.+?)$"
    );
    private static final Pattern LIKE_UPDATE = Pattern.compile(
            "(?iu)^a " + DEFINITE_KIND + " " + NAME_CAPTURE
                    + " le gustan? (.+?)$"
    );
    private static final Pattern AGE_UPDATE = Pattern.compile(
            "(?iu)^" + DEFINITE_KIND + " " + NAME_CAPTURE
                    + " tiene (\\d{1,2}) a[nñ]os$"
    );
    private static final Pattern OWNER_HAS_PET = Pattern.compile(
            "(?iu)^" + NAME_CAPTURE + " tiene " + INDEFINITE_KIND
                    + " (?:que )?se llama " + NAME_CAPTURE + "$"
    );
    private static final Pattern PET_OF_OWNER = Pattern.compile(
            "(?iu)^" + NAME_CAPTURE + " es " + DEFINITE_KIND + " de "
                    + NAME_CAPTURE + "$"
    );
    private static final Pattern SIMPLE_PET = Pattern.compile(
            "(?iu)^" + NAME_CAPTURE + " es " + INDEFINITE_KIND + "$"
    );
    private static final Pattern MY_PET = Pattern.compile(
            "(?iu)^mi (gato|gata|perro|perra) se llama "
                    + NAME_CAPTURE + "$"
    );
    private static final Pattern WORD = Pattern.compile(
            "(?iu)^[\\p{L}]+(?:[-'][\\p{L}]+)?$"
    );

    private SpanishPetMemoryParser() {
    }

    public static PetMemoryCommand parse(List<String> hypotheses) {
        if (hypotheses == null) {
            return null;
        }
        for (String hypothesis : hypotheses) {
            PetMemoryCommand command = parse(hypothesis);
            if (command != null) {
                return command;
            }
        }
        return null;
    }

    static PetMemoryCommand parse(String hypothesis) {
        if (hypothesis == null) {
            return null;
        }
        String utterance = WAKE_PREFIX.matcher(clean(hypothesis))
                .replaceFirst("");
        if (utterance.isEmpty()) {
            return null;
        }

        Matcher matcher = FAVORITE_FOOD_QUERY.matcher(utterance);
        if (matcher.matches()) {
            return petQuery(
                    PetMemoryCommand.Type.QUERY_FAVORITE_FOOD,
                    matcher.group(1),
                    matcher.group(2)
            );
        }
        matcher = LIKES_QUERY.matcher(utterance);
        if (matcher.matches()) {
            return petQuery(
                    PetMemoryCommand.Type.QUERY_LIKES,
                    matcher.group(1),
                    matcher.group(2)
            );
        }
        matcher = SUMMARY_QUERY.matcher(utterance);
        if (matcher.matches()) {
            return petQuery(
                    PetMemoryCommand.Type.QUERY_SUMMARY,
                    matcher.group(1),
                    matcher.group(2)
            );
        }
        matcher = OWNER_PETS_QUERY.matcher(utterance);
        if (matcher.matches()) {
            String owner = validName(matcher.group(1));
            return owner == null ? null : PetMemoryCommand.ownerQuery(owner);
        }
        matcher = FAVORITE_FOOD_UPDATE.matcher(utterance);
        if (matcher.matches()) {
            return petTextUpdate(
                    PetMemoryCommand.Type.SET_FAVORITE_FOOD,
                    matcher.group(1),
                    matcher.group(2),
                    matcher.group(3)
            );
        }
        matcher = LIKE_UPDATE.matcher(utterance);
        if (matcher.matches()) {
            return petTextUpdate(
                    PetMemoryCommand.Type.ADD_LIKE,
                    matcher.group(1),
                    matcher.group(2),
                    matcher.group(3)
            );
        }
        matcher = AGE_UPDATE.matcher(utterance);
        if (matcher.matches()) {
            String name = validName(matcher.group(2));
            PetMemoryCommand.Kind kind = kind(matcher.group(1));
            int age;
            try {
                age = Integer.parseInt(matcher.group(3));
            } catch (NumberFormatException error) {
                return null;
            }
            return name == null || kind == null || age <= 0 || age > MAX_AGE
                    ? null : PetMemoryCommand.ageUpdate(name, kind, age);
        }
        matcher = OWNER_HAS_PET.matcher(utterance);
        if (matcher.matches()) {
            return register(matcher.group(3), matcher.group(2), matcher.group(1));
        }
        matcher = PET_OF_OWNER.matcher(utterance);
        if (matcher.matches()) {
            return register(matcher.group(1), matcher.group(2), matcher.group(3));
        }
        matcher = SIMPLE_PET.matcher(utterance);
        if (matcher.matches()) {
            return register(matcher.group(1), matcher.group(2), null);
        }
        matcher = MY_PET.matcher(utterance);
        if (matcher.matches()) {
            return register(matcher.group(2), matcher.group(1), null);
        }
        return null;
    }

    private static PetMemoryCommand petQuery(
            PetMemoryCommand.Type type,
            String kindCandidate,
            String nameCandidate
    ) {
        String name = validName(nameCandidate);
        PetMemoryCommand.Kind kind = kind(kindCandidate);
        return name == null || kind == null
                ? null : PetMemoryCommand.petQuery(type, name, kind);
    }

    private static PetMemoryCommand petTextUpdate(
            PetMemoryCommand.Type type,
            String kindCandidate,
            String nameCandidate,
            String valueCandidate
    ) {
        String name = validName(nameCandidate);
        String value = validFact(valueCandidate);
        PetMemoryCommand.Kind kind = kind(kindCandidate);
        return name == null || value == null || kind == null
                ? null : PetMemoryCommand.textUpdate(type, name, kind, value);
    }

    private static PetMemoryCommand register(
            String petCandidate,
            String kindCandidate,
            String ownerCandidate
    ) {
        String pet = validName(petCandidate);
        String owner = ownerCandidate == null ? null : validName(ownerCandidate);
        PetMemoryCommand.Kind kind = kind(kindCandidate);
        return pet == null || kind == null
                || (ownerCandidate != null && owner == null)
                ? null : PetMemoryCommand.register(pet, kind, owner);
    }

    private static PetMemoryCommand.Kind kind(String candidate) {
        return PetMemoryCommand.Kind.fromDescriptor(candidate);
    }

    private static String clean(String value) {
        return value.trim()
                .replaceAll("^[¿?¡!.,;:]+", "")
                .replaceAll("[¿?¡!.,;:]+$", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String validName(String candidate) {
        String cleaned = clean(candidate);
        String[] words = cleaned.split(" ");
        if (words.length < 1 || words.length > 3) {
            return null;
        }
        StringBuilder name = new StringBuilder();
        for (String word : words) {
            if (!WORD.matcher(word).matches()) {
                return null;
            }
            if (name.length() > 0) {
                name.append(' ');
            }
            name.append(titleCase(word));
        }
        return name.toString();
    }

    private static String validFact(String candidate) {
        String cleaned = clean(candidate).toLowerCase(new Locale("es"));
        String[] words = cleaned.split(" ");
        if (words.length < 1 || words.length > 5) {
            return null;
        }
        for (String word : words) {
            if (!WORD.matcher(word).matches()) {
                return null;
            }
        }
        return cleaned;
    }

    private static String titleCase(String word) {
        Locale spanish = new Locale("es");
        return word.substring(0, 1).toUpperCase(spanish)
                + word.substring(1).toLowerCase(spanish);
    }
}
