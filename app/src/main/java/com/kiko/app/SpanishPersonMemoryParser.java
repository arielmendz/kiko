package com.kiko.app;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SpanishPersonMemoryParser {
    private static final int MAX_AGE = 130;
    private static final String NAME_CAPTURE =
            "([\\p{L}]+(?:[-'][\\p{L}]+)?"
                    + "(?:\\s+[\\p{L}]+(?:[-'][\\p{L}]+)?){0,2})";
    private static final Pattern WAKE_PREFIX = Pattern.compile(
            "(?iu)^(?:kiko|quico|quiko)\\b[\\s,;:.-]*"
    );
    private static final Pattern FAVORITE_FOOD_QUERY = Pattern.compile(
            "(?iu)^(?:cu[aá]l es )?la comida favorita de "
                    + NAME_CAPTURE + "$"
    );
    private static final Pattern LIKES_QUERY = Pattern.compile(
            "(?iu)^qu[eé](?: cosas?)? le gustan? a " + NAME_CAPTURE + "$"
    );
    private static final Pattern SUMMARY_QUERY = Pattern.compile(
            "(?iu)^qu[eé] sabes de " + NAME_CAPTURE + "$"
    );
    private static final Pattern FAVORITE_FOOD_UPDATE = Pattern.compile(
            "(?iu)^la comida favorita de " + NAME_CAPTURE + " es (.+?)$"
    );
    private static final Pattern LIKE_UPDATE = Pattern.compile(
            "(?iu)^(?:a )?" + NAME_CAPTURE + " le gustan? (.+?)$"
    );
    private static final Pattern AGE_UPDATE = Pattern.compile(
            "(?iu)^" + NAME_CAPTURE + " tiene (\\d{1,3}) a[nñ]os$"
    );
    private static final Pattern WORD = Pattern.compile(
            "(?iu)^[\\p{L}]+(?:[-'][\\p{L}]+)?$"
    );

    private SpanishPersonMemoryParser() {
    }

    public static PersonMemoryCommand parse(List<String> hypotheses) {
        if (hypotheses == null) {
            return null;
        }
        for (String hypothesis : hypotheses) {
            PersonMemoryCommand command = parse(hypothesis);
            if (command != null) {
                return command;
            }
        }
        return null;
    }

    static PersonMemoryCommand parse(String hypothesis) {
        if (hypothesis == null) {
            return null;
        }
        String utterance = clean(hypothesis);
        utterance = WAKE_PREFIX.matcher(utterance).replaceFirst("");
        if (utterance.isEmpty()) {
            return null;
        }

        Matcher matcher = FAVORITE_FOOD_QUERY.matcher(utterance);
        if (matcher.matches()) {
            String name = validName(matcher.group(1));
            return name == null ? null : PersonMemoryCommand.query(
                    PersonMemoryCommand.Type.QUERY_FAVORITE_FOOD,
                    name
            );
        }
        matcher = LIKES_QUERY.matcher(utterance);
        if (matcher.matches()) {
            String name = validName(matcher.group(1));
            return name == null ? null : PersonMemoryCommand.query(
                    PersonMemoryCommand.Type.QUERY_LIKES,
                    name
            );
        }
        matcher = SUMMARY_QUERY.matcher(utterance);
        if (matcher.matches()) {
            String name = validName(matcher.group(1));
            return name == null ? null : PersonMemoryCommand.query(
                    PersonMemoryCommand.Type.QUERY_SUMMARY,
                    name
            );
        }
        matcher = FAVORITE_FOOD_UPDATE.matcher(utterance);
        if (matcher.matches()) {
            String name = validName(matcher.group(1));
            String value = validFact(matcher.group(2));
            return name == null || value == null ? null
                    : PersonMemoryCommand.textUpdate(
                            PersonMemoryCommand.Type.SET_FAVORITE_FOOD,
                            name,
                            value
                    );
        }
        matcher = AGE_UPDATE.matcher(utterance);
        if (matcher.matches()) {
            String name = validName(matcher.group(1));
            int age;
            try {
                age = Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException error) {
                return null;
            }
            return name == null || age <= 0 || age > MAX_AGE ? null
                    : PersonMemoryCommand.ageUpdate(name, age);
        }
        matcher = LIKE_UPDATE.matcher(utterance);
        if (matcher.matches()) {
            String name = validName(matcher.group(1));
            String value = validFact(matcher.group(2));
            return name == null || value == null ? null
                    : PersonMemoryCommand.textUpdate(
                            PersonMemoryCommand.Type.ADD_LIKE,
                            name,
                            value
                    );
        }
        return null;
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
        if (word.isEmpty()) {
            return word;
        }
        Locale spanish = new Locale("es");
        return word.substring(0, 1).toUpperCase(spanish)
                + word.substring(1).toLowerCase(spanish);
    }
}
