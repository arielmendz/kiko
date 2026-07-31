package com.kiko.app;

public final class SleepMaintenanceReport {
    public enum State {
        NEVER,
        QUEUED,
        RUNNING,
        SUCCESS,
        FAILED,
        CANCELLED
    }

    private final boolean automaticEnabled;
    private final boolean photoCleanupEnabled;
    private final boolean requestedRunPending;
    private final State state;
    private final long stateChangedAtEpochMillis;
    private final long lastCompletedAtEpochMillis;
    private final int peopleVerified;
    private final int petsVerified;
    private final int facesVerified;
    private final int duplicateRecordsMerged;
    private final int duplicateLikesRemoved;
    private final int photosRetained;
    private final int photosDeleted;
    private final int namedPhotoGroups;

    SleepMaintenanceReport(
            boolean automaticEnabled,
            boolean photoCleanupEnabled,
            boolean requestedRunPending,
            State state,
            long stateChangedAtEpochMillis,
            long lastCompletedAtEpochMillis,
            int peopleVerified,
            int petsVerified,
            int facesVerified,
            int duplicateRecordsMerged,
            int duplicateLikesRemoved,
            int photosRetained,
            int photosDeleted,
            int namedPhotoGroups
    ) {
        this.automaticEnabled = automaticEnabled;
        this.photoCleanupEnabled = photoCleanupEnabled;
        this.requestedRunPending = requestedRunPending;
        this.state = state;
        this.stateChangedAtEpochMillis = stateChangedAtEpochMillis;
        this.lastCompletedAtEpochMillis = lastCompletedAtEpochMillis;
        this.peopleVerified = peopleVerified;
        this.petsVerified = petsVerified;
        this.facesVerified = facesVerified;
        this.duplicateRecordsMerged = duplicateRecordsMerged;
        this.duplicateLikesRemoved = duplicateLikesRemoved;
        this.photosRetained = photosRetained;
        this.photosDeleted = photosDeleted;
        this.namedPhotoGroups = namedPhotoGroups;
    }

    public boolean isAutomaticEnabled() {
        return automaticEnabled;
    }

    public boolean isPhotoCleanupEnabled() {
        return photoCleanupEnabled;
    }

    public boolean isRequestedRunPending() {
        return requestedRunPending;
    }

    public State getState() {
        return state;
    }

    public long getStateChangedAtEpochMillis() {
        return stateChangedAtEpochMillis;
    }

    public long getLastCompletedAtEpochMillis() {
        return lastCompletedAtEpochMillis;
    }

    public int getPeopleVerified() {
        return peopleVerified;
    }

    public int getPetsVerified() {
        return petsVerified;
    }

    public int getFacesVerified() {
        return facesVerified;
    }

    public int getDuplicateRecordsMerged() {
        return duplicateRecordsMerged;
    }

    public int getDuplicateLikesRemoved() {
        return duplicateLikesRemoved;
    }

    public int getPhotosRetained() {
        return photosRetained;
    }

    public int getPhotosDeleted() {
        return photosDeleted;
    }

    public int getNamedPhotoGroups() {
        return namedPhotoGroups;
    }
}
