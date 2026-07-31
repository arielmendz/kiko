package com.kiko.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class VisualSubjectLoadResult {
    private final boolean successful;
    private final List<VisualSubjectRecord> records;

    private VisualSubjectLoadResult(
            boolean successful,
            List<VisualSubjectRecord> records
    ) {
        this.successful = successful;
        this.records = Collections.unmodifiableList(new ArrayList<>(records));
    }

    static VisualSubjectLoadResult success(List<VisualSubjectRecord> records) {
        return new VisualSubjectLoadResult(true, records);
    }

    static VisualSubjectLoadResult failure() {
        return new VisualSubjectLoadResult(false, Collections.emptyList());
    }

    boolean isSuccessful() {
        return successful;
    }

    List<VisualSubjectRecord> getRecords() {
        return records;
    }
}
