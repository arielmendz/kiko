package com.kiko.app;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class VisualHistoryMetadata {
    private static final String VERSION = "kiko-visual-history-v1";

    private VisualHistoryMetadata() {
    }

    static String encode(long capturedAtEpochMillis, String description) {
        if (capturedAtEpochMillis <= 0L) {
            throw new IllegalArgumentException("Invalid capture time");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required");
        }
        String encodedDescription = Base64.getEncoder().encodeToString(
                description.getBytes(StandardCharsets.UTF_8)
        );
        return VERSION + "\n" + capturedAtEpochMillis + "\n" + encodedDescription;
    }

    static Decoded decode(String encoded) {
        if (encoded == null) {
            throw new IllegalArgumentException("Metadata is required");
        }
        String[] lines = encoded.split("\n", -1);
        if (lines.length != 3 || !VERSION.equals(lines[0])) {
            throw new IllegalArgumentException("Unsupported visual history metadata");
        }

        long capturedAtEpochMillis;
        try {
            capturedAtEpochMillis = Long.parseLong(lines[1]);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Invalid capture time", error);
        }
        if (capturedAtEpochMillis <= 0L) {
            throw new IllegalArgumentException("Invalid capture time");
        }

        String description;
        try {
            description = new String(
                    Base64.getDecoder().decode(lines[2]),
                    StandardCharsets.UTF_8
            );
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Invalid description", error);
        }
        if (description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required");
        }
        return new Decoded(capturedAtEpochMillis, description);
    }

    static final class Decoded {
        private final long capturedAtEpochMillis;
        private final String description;

        private Decoded(long capturedAtEpochMillis, String description) {
            this.capturedAtEpochMillis = capturedAtEpochMillis;
            this.description = description;
        }

        long getCapturedAtEpochMillis() {
            return capturedAtEpochMillis;
        }

        String getDescription() {
            return description;
        }
    }
}
