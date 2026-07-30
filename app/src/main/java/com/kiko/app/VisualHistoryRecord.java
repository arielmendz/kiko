package com.kiko.app;

import java.io.File;
import java.util.Objects;

public final class VisualHistoryRecord {
    private final String id;
    private final long capturedAtEpochMillis;
    private final String description;
    private final File imageFile;

    VisualHistoryRecord(
            String id,
            long capturedAtEpochMillis,
            String description,
            File imageFile
    ) {
        this.id = Objects.requireNonNull(id);
        this.capturedAtEpochMillis = capturedAtEpochMillis;
        this.description = Objects.requireNonNull(description);
        this.imageFile = Objects.requireNonNull(imageFile);
    }

    public String getId() {
        return id;
    }

    public long getCapturedAtEpochMillis() {
        return capturedAtEpochMillis;
    }

    public String getDescription() {
        return description;
    }

    public File getImageFile() {
        return imageFile;
    }
}
