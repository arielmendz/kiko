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
    private final boolean requestedRunPending;
    private final State state;
    private final long stateChangedAtEpochMillis;
    private final long lastCompletedAtEpochMillis;
    private final int peopleVerified;
    private final int petsVerified;
    private final int facesVerified;
    private final int duplicateRecordsMerged;
    private final int duplicateLikesRemoved;

    SleepMaintenanceReport(
            boolean automaticEnabled,
            boolean requestedRunPending,
            State state,
            long stateChangedAtEpochMillis,
            long lastCompletedAtEpochMillis,
            int peopleVerified,
            int petsVerified,
            int facesVerified,
            int duplicateRecordsMerged,
            int duplicateLikesRemoved
    ) {
        this.automaticEnabled = automaticEnabled;
        this.requestedRunPending = requestedRunPending;
        this.state = state;
        this.stateChangedAtEpochMillis = stateChangedAtEpochMillis;
        this.lastCompletedAtEpochMillis = lastCompletedAtEpochMillis;
        this.peopleVerified = peopleVerified;
        this.petsVerified = petsVerified;
        this.facesVerified = facesVerified;
        this.duplicateRecordsMerged = duplicateRecordsMerged;
        this.duplicateLikesRemoved = duplicateLikesRemoved;
    }

    public boolean isAutomaticEnabled() {
        return automaticEnabled;
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
}
