package com.kiko.app;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SpanishPersonNameExtractor {
    private static final int MAX_NAME_LENGTH = 40;
    private static final int MAX_NAME_WORDS = 3;
    private static final Pattern NAME_PREFIX = Pattern.compile(
            "^(?:(?:ella|él|el)\\s+se\\s+llama|se\\s+llama|"
                    + "su\\s+nombre\\s+es|(?:ella|él|el)\\s+es|es|soy)\\s+",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern INVALID_CHARACTER = Pattern.compile(
            "[^\\p{L}\\p{M}'’\\- ]"
    );
    private static final Pattern DIGIT = Pattern.compile("\\p{N}");
    private static final Pattern EDGE_SEPARATOR = Pattern.compile(
            "^[^\\p{L}]+|[^\\p{L}]+$"
    );

    private SpanishPersonNameExtractor() {
    }

    public static String extract(List<String> hypotheses) {
        if (hypotheses == null) {
            return null;
        }
        for (String hypothesis : hypotheses) {
            String extracted = extractOne(hypothesis);
            if (extracted != null) {
                return extracted;
            }
        }
        return null;
    }

    public static boolean containsCancel(List<String> hypotheses) {
        if (hypotheses == null) {
            return false;
        }
        for (String hypothesis : hypotheses) {
            String normalized = normalizeForComparison(hypothesis);
            if ("no".equals(normalized)
                    || "no gracias".equals(normalized)
                    || "cancelar".equals(normalized)
                    || "cancela".equals(normalized)
                    || "no se".equals(normalized)
                    || "no lo se".equals(normalized)
                    || normalized.startsWith("no se ")
                    || normalized.startsWith("no lo se ")
                    || "nadie".equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static String extractOne(String hypothesis) {
        if (hypothesis == null || hypothesis.trim().isEmpty()) {
            return null;
        }
        String candidate = NAME_PREFIX.matcher(hypothesis.trim()).replaceFirst("");
        if (DIGIT.matcher(candidate).find()) {
            return null;
        }
        candidate = EDGE_SEPARATOR.matcher(candidate).replaceAll("");
        candidate = candidate.replaceAll("\\s+", " ").trim();
        if (candidate.isEmpty()
                || candidate.length() > MAX_NAME_LENGTH
                || INVALID_CHARACTER.matcher(candidate).find()
                || candidate.split(" ").length > MAX_NAME_WORDS
                || containsCancel(List.of(candidate))) {
            return null;
        }
        return toDisplayName(candidate);
    }

    private static String toDisplayName(String candidate) {
        String[] words = candidate.toLowerCase(Locale.forLanguageTag("es"))
                .split(" ");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (formatted.length() > 0) {
                formatted.append(' ');
            }
            int firstCodePoint = word.codePointAt(0);
            formatted.appendCodePoint(Character.toTitleCase(firstCodePoint));
            formatted.append(word.substring(Character.charCount(firstCodePoint)));
        }
        return formatted.toString();
    }

    private static String normalizeForComparison(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L} ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
