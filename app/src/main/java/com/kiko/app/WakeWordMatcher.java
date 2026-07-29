package com.kiko.app;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class WakeWordMatcher {
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Set<String> KIKO_TRANSCRIPTIONS = new HashSet<>(
            Arrays.asList("kiko", "quico", "quiko")
    );

    private WakeWordMatcher() {
    }

    public static boolean containsKiko(List<String> hypotheses) {
        if (hypotheses == null) {
            return false;
        }

        for (String hypothesis : hypotheses) {
            if (containsAnyToken(hypothesis, KIKO_TRANSCRIPTIONS)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAnyToken(String text, Set<String> expectedTokens) {
        if (text == null) {
            return false;
        }

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        normalized = DIACRITICS.matcher(normalized).replaceAll("");
        normalized = normalized.toLowerCase(Locale.ROOT);

        for (String token : NON_ALPHANUMERIC.split(normalized)) {
            if (expectedTokens.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
