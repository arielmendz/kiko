package com.kiko.app;

import java.io.File;
import java.util.Objects;

public final class VisualHistoryRecord {
    public enum SubjectKind {
        PERSON,
        PET
    }

    public enum RecognitionStatus {
        UNKNOWN,
        RECOGNIZED,
        UNRECOGNIZED
    }

    private final String id;
    private final long capturedAtEpochMillis;
    private final String description;
    private final RecognitionStatus recognitionStatus;
    private final SubjectKind subjectKind;
    private final String subjectName;
    private final File imageFile;

    VisualHistoryRecord(
            String id,
            long capturedAtEpochMillis,
            String description,
            SubjectKind subjectKind,
            String subjectName,
            File imageFile
    ) {
        this(
                id,
                capturedAtEpochMillis,
                description,
                RecognitionStatus.UNKNOWN,
                subjectKind,
                subjectName,
                imageFile
        );
    }

    VisualHistoryRecord(
            String id,
            long capturedAtEpochMillis,
            String description,
            RecognitionStatus recognitionStatus,
            SubjectKind subjectKind,
            String subjectName,
            File imageFile
    ) {
        this.id = Objects.requireNonNull(id);
        this.capturedAtEpochMillis = capturedAtEpochMillis;
        this.description = Objects.requireNonNull(description);
        this.recognitionStatus = Objects.requireNonNull(recognitionStatus);
        if ((subjectKind == null) != (subjectName == null)) {
            throw new IllegalArgumentException(
                    "Subject kind and name must both be present or absent"
            );
        }
        this.subjectKind = subjectKind;
        this.subjectName = subjectName;
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

    public RecognitionStatus getRecognitionStatus() {
        return recognitionStatus;
    }

    public String getPersonName() {
        return subjectKind == SubjectKind.PERSON ? subjectName : null;
    }

    public SubjectKind getSubjectKind() {
        return subjectKind;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public File getImageFile() {
        return imageFile;
    }
}
