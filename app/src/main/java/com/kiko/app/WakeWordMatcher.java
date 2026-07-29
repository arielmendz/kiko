package com.kiko.app;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class WakeWordMatcher {
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{L}\\p{N}]+");

    private WakeWordMatcher() {
    }

    public static boolean containsKiko(List<String> hypotheses) {
        if (hypotheses == null) {
            return false;
        }

        for (String hypothesis : hypotheses) {
            if (containsToken(hypothesis, "kiko")) {
                return true;
            }
        }
        return false;
    }

    static boolean containsToken(String text, String expectedToken) {
        if (text == null || expectedToken == null) {
            return false;
        }

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        normalized = DIACRITICS.matcher(normalized).replaceAll("");
        normalized = normalized.toLowerCase(Locale.ROOT);

        for (String token : NON_ALPHANUMERIC.split(normalized)) {
            if (expectedToken.equals(token)) {
                return true;
            }
        }
        return false;
    }
}
