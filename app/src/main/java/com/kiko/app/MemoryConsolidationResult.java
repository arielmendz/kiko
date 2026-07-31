package com.kiko.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class MemoryConsolidationResult<T> {
    private final List<T> records;
    private final int recordsBefore;
    private final int duplicateRecordsMerged;
    private final int duplicateLikesRemoved;

    MemoryConsolidationResult(
            List<T> records,
            int recordsBefore,
            int duplicateRecordsMerged,
            int duplicateLikesRemoved
    ) {
        this.records = Collections.unmodifiableList(new ArrayList<>(records));
        this.recordsBefore = recordsBefore;
        this.duplicateRecordsMerged = duplicateRecordsMerged;
        this.duplicateLikesRemoved = duplicateLikesRemoved;
    }

    List<T> getRecords() {
        return records;
    }

    int getRecordsBefore() {
        return recordsBefore;
    }

    int getDuplicateRecordsMerged() {
        return duplicateRecordsMerged;
    }

    int getDuplicateLikesRemoved() {
        return duplicateLikesRemoved;
    }

    boolean changed() {
        return duplicateRecordsMerged > 0 || duplicateLikesRemoved > 0;
    }
}
