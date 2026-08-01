package com.kiko.app;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class SpanishNumberParser {
    private static final Map<String, Integer> NUMBER_WORDS;

    static {
        Map<String, Integer> words = new HashMap<>();
        words.put("un", 1);
        words.put("uno", 1);
        words.put("una", 1);
        words.put("dos", 2);
        words.put("tres", 3);
        words.put("cuatro", 4);
        words.put("cinco", 5);
        words.put("seis", 6);
        words.put("siete", 7);
        words.put("ocho", 8);
        words.put("nueve", 9);
        words.put("diez", 10);
        NUMBER_WORDS = Collections.unmodifiableMap(words);
    }

    private SpanishNumberParser() {
    }

    static Integer parse(String token) {
        if (token == null) {
            return null;
        }
        String normalized = token.trim().toLowerCase(Locale.ROOT);
        Integer wordValue = NUMBER_WORDS.get(normalized);
        if (wordValue != null) {
            return wordValue;
        }
        if (!normalized.matches("[0-9]+")) {
            return null;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException error) {
            return null;
        }
    }
}
