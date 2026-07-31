package com.kiko.app;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class FaceEmbeddingMatcher {
    static final float MATCH_THRESHOLD = 0.50f;
    static final float AMBIGUITY_MARGIN = 0.08f;

    private FaceEmbeddingMatcher() {
    }

    static Match match(float[] candidate, List<FaceIdentityRecord> identities) {
        if (candidate == null
                || candidate.length != FaceIdentityRecord.EMBEDDING_SIZE
                || identities == null
                || identities.isEmpty()) {
            return Match.unknown();
        }

        Map<String, NamedScore> bestByName = new HashMap<>();
        for (FaceIdentityRecord identity : identities) {
            float score = cosine(candidate, identity.getEmbedding());
            if (!Float.isFinite(score)) {
                continue;
            }
            String key = normalizeName(identity.getName());
            NamedScore existing = bestByName.get(key);
            if (existing == null || score > existing.score) {
                bestByName.put(key, new NamedScore(identity.getName(), score));
            }
        }

        NamedScore best = null;
        NamedScore second = null;
        for (NamedScore score : bestByName.values()) {
            if (best == null || score.score > best.score) {
                second = best;
                best = score;
            } else if (second == null || score.score > second.score) {
                second = score;
            }
        }
        if (best == null || best.score < MATCH_THRESHOLD) {
            return Match.unknown();
        }
        if (second != null && best.score - second.score < AMBIGUITY_MARGIN) {
            return Match.unknown();
        }
        return Match.known(best.name, best.score);
    }

    static float[] normalize(float[] embedding) {
        if (embedding == null
                || embedding.length != FaceIdentityRecord.EMBEDDING_SIZE) {
            throw new IllegalArgumentException("Unexpected face embedding size");
        }
        double squaredMagnitude = 0d;
        for (float value : embedding) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("Invalid face embedding");
            }
            squaredMagnitude += value * value;
        }
        double magnitude = Math.sqrt(squaredMagnitude);
        if (magnitude < 1e-12d) {
            throw new IllegalArgumentException("Empty face embedding");
        }
        float[] normalized = new float[embedding.length];
        for (int index = 0; index < embedding.length; index++) {
            normalized[index] = (float) (embedding[index] / magnitude);
        }
        return normalized;
    }

    private static float cosine(float[] left, float[] right) {
        if (right == null || left.length != right.length) {
            return Float.NaN;
        }
        double dot = 0d;
        double leftMagnitude = 0d;
        double rightMagnitude = 0d;
        for (int index = 0; index < left.length; index++) {
            float leftValue = left[index];
            float rightValue = right[index];
            if (!Float.isFinite(leftValue) || !Float.isFinite(rightValue)) {
                return Float.NaN;
            }
            dot += leftValue * rightValue;
            leftMagnitude += leftValue * leftValue;
            rightMagnitude += rightValue * rightValue;
        }
        if (leftMagnitude < 1e-12d || rightMagnitude < 1e-12d) {
            return Float.NaN;
        }
        return (float) (dot / Math.sqrt(leftMagnitude * rightMagnitude));
    }

    private static String normalizeName(String name) {
        return Normalizer.normalize(name.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
    }

    static final class Match {
        private final String name;
        private final float score;

        private Match(String name, float score) {
            this.name = name;
            this.score = score;
        }

        static Match known(String name, float score) {
            return new Match(name, score);
        }

        static Match unknown() {
            return new Match(null, Float.NaN);
        }

        boolean isKnown() {
            return name != null;
        }

        String getName() {
            return name;
        }

        float getScore() {
            return score;
        }
    }

    private static final class NamedScore {
        private final String name;
        private final float score;

        private NamedScore(String name, float score) {
            this.name = name;
            this.score = score;
        }
    }
}
