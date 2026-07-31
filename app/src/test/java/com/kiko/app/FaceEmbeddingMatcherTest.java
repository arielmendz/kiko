package com.kiko.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public final class FaceEmbeddingMatcherTest {
    @Test
    public void returnsNameForClearConservativeMatch() {
        FaceEmbeddingMatcher.Match match = FaceEmbeddingMatcher.match(
                vector(1f, 0f, 0f),
                Arrays.asList(
                        identity("Ana", vector(1f, 0f, 0f)),
                        identity("Luis", vector(0f, 1f, 0f))
                )
        );

        assertTrue(match.isKnown());
        assertEquals("Ana", match.getName());
        assertEquals(1f, match.getScore(), 0.0001f);
    }

    @Test
    public void rejectsWeakMatch() {
        FaceEmbeddingMatcher.Match match = FaceEmbeddingMatcher.match(
                vector(1f, 0f, 0f),
                Arrays.asList(identity("Ana", vector(0f, 1f, 0f)))
        );

        assertFalse(match.isKnown());
    }

    @Test
    public void rejectsAmbiguousNames() {
        FaceEmbeddingMatcher.Match match = FaceEmbeddingMatcher.match(
                vector(1f, 0f, 0f),
                Arrays.asList(
                        identity("Ana", vector(0.80f, 0.60f, 0f)),
                        identity("Luis", vector(0.75f, 0.66f, 0f))
                )
        );

        assertFalse(match.isKnown());
    }

    @Test
    public void multipleSamplesOfSameNameDoNotCreateAmbiguity() {
        FaceEmbeddingMatcher.Match match = FaceEmbeddingMatcher.match(
                vector(1f, 0f, 0f),
                Arrays.asList(
                        identity("María", vector(1f, 0f, 0f)),
                        identity("maria", vector(0.98f, 0.20f, 0f))
                )
        );

        assertTrue(match.isKnown());
        assertEquals("María", match.getName());
    }

    private static FaceIdentityRecord identity(String name, float[] embedding) {
        return new FaceIdentityRecord(
                "id-" + name,
                "history-" + name,
                name,
                1L,
                FaceEmbeddingMatcher.normalize(embedding)
        );
    }

    private static float[] vector(float first, float second, float third) {
        float[] vector = new float[FaceIdentityRecord.EMBEDDING_SIZE];
        vector[0] = first;
        vector[1] = second;
        vector[2] = third;
        return FaceEmbeddingMatcher.normalize(vector);
    }
}
