package com.kiko.app;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class VisualHistoryMetadata {
    private static final String VERSION_1 = "kiko-visual-history-v1";
    private static final String VERSION_2 = "kiko-visual-history-v2";

    private VisualHistoryMetadata() {
    }

    static String encode(long capturedAtEpochMillis, String description) {
        return encode(capturedAtEpochMillis, description, null);
    }

    static String encode(
            long capturedAtEpochMillis,
            String description,
            String personName
    ) {
        if (capturedAtEpochMillis <= 0L) {
            throw new IllegalArgumentException("Invalid capture time");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required");
        }
        String encodedDescription = Base64.getEncoder().encodeToString(
                description.getBytes(StandardCharsets.UTF_8)
        );
        String encodedPersonName = personName == null
                ? ""
                : Base64.getEncoder().encodeToString(
                        personName.getBytes(StandardCharsets.UTF_8)
                );
        return VERSION_2
                + "\n"
                + capturedAtEpochMillis
                + "\n"
                + encodedDescription
                + "\n"
                + encodedPersonName;
    }

    static Decoded decode(String encoded) {
        if (encoded == null) {
            throw new IllegalArgumentException("Metadata is required");
        }
        String[] lines = encoded.split("\n", -1);
        boolean legacy = lines.length == 3 && VERSION_1.equals(lines[0]);
        boolean current = lines.length == 4 && VERSION_2.equals(lines[0]);
        if (!legacy && !current) {
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

        String personName = null;
        if (current && !lines[3].isEmpty()) {
            try {
                personName = new String(
                        Base64.getDecoder().decode(lines[3]),
                        StandardCharsets.UTF_8
                );
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException("Invalid person name", error);
            }
            if (personName.trim().isEmpty()) {
                throw new IllegalArgumentException("Person name is required");
            }
        }
        return new Decoded(capturedAtEpochMillis, description, personName);
    }

    static final class Decoded {
        private final long capturedAtEpochMillis;
        private final String description;
        private final String personName;

        private Decoded(
                long capturedAtEpochMillis,
                String description,
                String personName
        ) {
            this.capturedAtEpochMillis = capturedAtEpochMillis;
            this.description = description;
            this.personName = personName;
        }

        long getCapturedAtEpochMillis() {
            return capturedAtEpochMillis;
        }

        String getDescription() {
            return description;
        }

        String getPersonName() {
            return personName;
        }
    }
}
