package com.kiko.app;

import java.util.Arrays;
import java.util.Objects;

public final class FaceIdentityRecord {
    public static final int EMBEDDING_SIZE = 128;

    private final String id;
    private final String sourceHistoryId;
    private final String name;
    private final long enrolledAtEpochMillis;
    private final float[] embedding;

    FaceIdentityRecord(
            String id,
            String sourceHistoryId,
            String name,
            long enrolledAtEpochMillis,
            float[] embedding
    ) {
        this.id = Objects.requireNonNull(id);
        this.sourceHistoryId = Objects.requireNonNull(sourceHistoryId);
        this.name = Objects.requireNonNull(name);
        this.enrolledAtEpochMillis = enrolledAtEpochMillis;
        if (embedding == null || embedding.length != EMBEDDING_SIZE) {
            throw new IllegalArgumentException("Unexpected face embedding size");
        }
        this.embedding = Arrays.copyOf(embedding, embedding.length);
    }

    public String getId() {
        return id;
    }

    public String getSourceHistoryId() {
        return sourceHistoryId;
    }

    public String getName() {
        return name;
    }

    public long getEnrolledAtEpochMillis() {
        return enrolledAtEpochMillis;
    }

    public float[] getEmbedding() {
        return Arrays.copyOf(embedding, embedding.length);
    }
}
