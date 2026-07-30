package com.kiko.app;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SpanishCommandMatcher {
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Pattern DESCRIBE_SCENE = Pattern.compile(
            "(?:^| )(?:que ves|que puedes ver|que estas viendo)(?: |$)"
    );

    private SpanishCommandMatcher() {
    }

    public static boolean containsDescribeScene(List<String> hypotheses) {
        if (hypotheses == null) {
            return false;
        }

        for (String hypothesis : hypotheses) {
            if (hypothesis != null
                    && DESCRIBE_SCENE.matcher(normalize(hypothesis)).find()) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        normalized = DIACRITICS.matcher(normalized).replaceAll("");
        normalized = normalized.toLowerCase(Locale.ROOT);
        normalized = NON_ALPHANUMERIC.matcher(normalized).replaceAll(" ");
        return normalized.trim();
    }
}
