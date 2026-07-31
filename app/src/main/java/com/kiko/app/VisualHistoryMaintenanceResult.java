package com.kiko.app;

final class VisualHistoryMaintenanceResult {
    private final boolean successful;
    private final int photosRetained;
    private final int photosDeleted;
    private final int namedGroups;

    private VisualHistoryMaintenanceResult(
            boolean successful,
            int photosRetained,
            int photosDeleted,
            int namedGroups
    ) {
        this.successful = successful;
        this.photosRetained = photosRetained;
        this.photosDeleted = photosDeleted;
        this.namedGroups = namedGroups;
    }

    static VisualHistoryMaintenanceResult success(
            int photosRetained,
            int photosDeleted,
            int namedGroups
    ) {
        return new VisualHistoryMaintenanceResult(
                true,
                photosRetained,
                photosDeleted,
                namedGroups
        );
    }

    static VisualHistoryMaintenanceResult failure() {
        return new VisualHistoryMaintenanceResult(false, 0, 0, 0);
    }

    boolean isSuccessful() {
        return successful;
    }

    int getPhotosRetained() {
        return photosRetained;
    }

    int getPhotosDeleted() {
        return photosDeleted;
    }

    int getNamedGroups() {
        return namedGroups;
    }
}
