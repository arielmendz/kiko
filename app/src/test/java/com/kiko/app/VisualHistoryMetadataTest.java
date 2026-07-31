package com.kiko.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
        assertNull(decoded.getPersonName());
    }

    @Test
    public void roundTripsConfirmedPersonName() {
        VisualHistoryMetadata.Decoded decoded = VisualHistoryMetadata.decode(
                VisualHistoryMetadata.encode(
                        1_785_443_200_123L,
                        "Veo una persona, ¿quién es?",
                        "María José"
                )
        );

        assertEquals("María José", decoded.getPersonName());
    }

    @Test
    public void readsLegacyRecordsWithoutPersonName() {
        VisualHistoryMetadata.Decoded decoded = VisualHistoryMetadata.decode(
                "kiko-visual-history-v1\n123\nVmVvIHVuYSBwZXJzb25hLg=="
        );

        assertEquals("Veo una persona.", decoded.getDescription());
        assertNull(decoded.getPersonName());
    }

    @Test
    public void readsVersionTwoPersonName() {
        VisualHistoryMetadata.Decoded decoded = VisualHistoryMetadata.decode(
                "kiko-visual-history-v2\n123\nVmVvIHVuYSBwZXJzb25hLg==\nUGVkcm8="
        );

        assertEquals("Pedro", decoded.getPersonName());
        assertEquals(
                VisualHistoryRecord.RecognitionStatus.UNKNOWN,
                decoded.getRecognitionStatus()
        );
    }

    @Test
    public void roundTripsConclusiveRecognitionStatusWithoutAName() {
        VisualHistoryMetadata.Decoded recognized = VisualHistoryMetadata.decode(
                VisualHistoryMetadata.encodeRecognition(
                        1_785_443_200_123L,
                        "Veo un perro.",
                        VisualHistoryRecord.RecognitionStatus.RECOGNIZED
                )
        );
        VisualHistoryMetadata.Decoded unrecognized = VisualHistoryMetadata.decode(
                VisualHistoryMetadata.encodeRecognition(
                        1_785_443_200_124L,
                        "No logro distinguir qué hay delante de mí.",
                        VisualHistoryRecord.RecognitionStatus.UNRECOGNIZED
                )
        );

        assertEquals(
                VisualHistoryRecord.RecognitionStatus.RECOGNIZED,
                recognized.getRecognitionStatus()
        );
        assertEquals(
                VisualHistoryRecord.RecognitionStatus.UNRECOGNIZED,
                unrecognized.getRecognitionStatus()
        );
        assertNull(recognized.getPersonName());
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
        assertThrows(
                IllegalArgumentException.class,
                () -> VisualHistoryMetadata.encodeRecognition(
                        123L,
                        "resultado",
                        VisualHistoryRecord.RecognitionStatus.UNKNOWN
                )
        );
    }
}
