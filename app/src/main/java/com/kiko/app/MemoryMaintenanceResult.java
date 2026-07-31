package com.kiko.app;

final class MemoryMaintenanceResult {
    private final boolean successful;
    private final int recordsBefore;
    private final int recordsAfter;
    private final int duplicateRecordsMerged;
    private final int duplicateLikesRemoved;

    private MemoryMaintenanceResult(
            boolean successful,
            int recordsBefore,
            int recordsAfter,
            int duplicateRecordsMerged,
            int duplicateLikesRemoved
    ) {
        this.successful = successful;
        this.recordsBefore = recordsBefore;
        this.recordsAfter = recordsAfter;
        this.duplicateRecordsMerged = duplicateRecordsMerged;
        this.duplicateLikesRemoved = duplicateLikesRemoved;
    }

    static MemoryMaintenanceResult success(
            int recordsBefore,
            int recordsAfter,
            int duplicateRecordsMerged,
            int duplicateLikesRemoved
    ) {
        return new MemoryMaintenanceResult(
                true,
                recordsBefore,
                recordsAfter,
                duplicateRecordsMerged,
                duplicateLikesRemoved
        );
    }

    static MemoryMaintenanceResult failure() {
        return new MemoryMaintenanceResult(false, 0, 0, 0, 0);
    }

    boolean isSuccessful() {
        return successful;
    }

    int getRecordsBefore() {
        return recordsBefore;
    }

    int getRecordsAfter() {
        return recordsAfter;
    }

    int getDuplicateRecordsMerged() {
        return duplicateRecordsMerged;
    }

    int getDuplicateLikesRemoved() {
        return duplicateLikesRemoved;
    }
}
