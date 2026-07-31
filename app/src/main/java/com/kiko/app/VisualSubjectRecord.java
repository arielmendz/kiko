package com.kiko.app;

import java.util.Objects;

final class VisualSubjectRecord {
    private final String historyRecordId;
    private final VisualHistoryRecord.SubjectKind kind;
    private final String name;

    VisualSubjectRecord(
            String historyRecordId,
            VisualHistoryRecord.SubjectKind kind,
            String name
    ) {
        this.historyRecordId = Objects.requireNonNull(historyRecordId);
        this.kind = Objects.requireNonNull(kind);
        this.name = Objects.requireNonNull(name);
    }

    String getHistoryRecordId() {
        return historyRecordId;
    }

    VisualHistoryRecord.SubjectKind getKind() {
        return kind;
    }

    String getName() {
        return name;
    }
}
