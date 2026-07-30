package com.kiko.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public final class VisualHistoryMetadataTest {
    @Test
    public void roundTripsTimestampAndSpanishDescription() {
        String description = "Veo una persona y un sofá.\nPrueba local.";

        VisualHistoryMetadata.Decoded decoded = VisualHistoryMetadata.decode(
                VisualHistoryMetadata.encode(1_785_443_200_123L, description)
        );

        assertEquals(1_785_443_200_123L, decoded.getCapturedAtEpochMillis());
        assertEquals(description, decoded.getDescription());
    }

    @Test
    public void rejectsUnsupportedOrMalformedMetadata() {
        assertThrows(
                IllegalArgumentException.class,
                () -> VisualHistoryMetadata.decode("other-version\n123\nYQ==")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> VisualHistoryMetadata.decode(
                        "kiko-visual-history-v1\nnot-a-time\nYQ=="
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> VisualHistoryMetadata.decode(
                        "kiko-visual-history-v1\n123\nnot base64"
                )
        );
    }
}
