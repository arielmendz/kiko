package com.kiko.app;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public final class FaceIdentityCodecTest {
    @Test
    public void roundTripsEncryptedRegistryPayload() throws Exception {
        float[] embedding = embedding(0.25f);
        FaceIdentityRecord source = new FaceIdentityRecord(
                "identity-id",
                "history-id",
                "María José",
                1_785_443_200_123L,
                embedding
        );

        List<FaceIdentityRecord> decoded = FaceIdentityCodec.decode(
                FaceIdentityCodec.encode(Arrays.asList(source))
        );

        assertEquals(1, decoded.size());
        FaceIdentityRecord result = decoded.get(0);
        assertEquals("identity-id", result.getId());
        assertEquals("history-id", result.getSourceHistoryId());
        assertEquals("María José", result.getName());
        assertEquals(1_785_443_200_123L, result.getEnrolledAtEpochMillis());
        assertArrayEquals(embedding, result.getEmbedding(), 0f);
    }

    @Test
    public void rejectsTrailingOrMalformedData() throws Exception {
        byte[] valid = FaceIdentityCodec.encode(Arrays.asList(
                new FaceIdentityRecord(
                        "identity-id",
                        "history-id",
                        "Ana",
                        1L,
                        embedding(0.5f)
                )
        ));
        byte[] trailing = Arrays.copyOf(valid, valid.length + 1);

        assertThrows(
                IOException.class,
                () -> FaceIdentityCodec.decode(trailing)
        );
        assertThrows(
                IOException.class,
                () -> FaceIdentityCodec.decode(new byte[]{0, 1, 2, 3})
        );
    }

    private static float[] embedding(float firstValue) {
        float[] embedding = new float[FaceIdentityRecord.EMBEDDING_SIZE];
        embedding[0] = firstValue;
        return embedding;
    }
}
