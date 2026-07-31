package com.kiko.app;

import android.content.Context;
import android.content.SharedPreferences;

final class SleepMaintenanceReportStore {
    private static final String PREFS_NAME = "sleep_maintenance";
    private static final String PREF_AUTOMATIC = "automatic_enabled";
    private static final String PREF_PHOTO_CLEANUP = "photo_cleanup_enabled";
    private static final String PREF_REQUESTED_PENDING = "requested_pending";
    private static final String PREF_STATE = "state";
    private static final String PREF_STATE_AT = "state_at";
    private static final String PREF_COMPLETED_AT = "completed_at";
    private static final String PREF_PEOPLE = "people_verified";
    private static final String PREF_PETS = "pets_verified";
    private static final String PREF_FACES = "faces_verified";
    private static final String PREF_RECORDS_MERGED = "records_merged";
    private static final String PREF_LIKES_REMOVED = "likes_removed";
    private static final String PREF_PHOTOS_RETAINED = "photos_retained";
    private static final String PREF_PHOTOS_DELETED = "photos_deleted";
    private static final String PREF_PHOTO_GROUPS = "photo_groups";

    private final SharedPreferences preferences;

    SleepMaintenanceReportStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );
    }

    synchronized SleepMaintenanceReport load() {
        SleepMaintenanceReport.State state;
        try {
            state = SleepMaintenanceReport.State.valueOf(preferences.getString(
                    PREF_STATE,
                    SleepMaintenanceReport.State.NEVER.name()
            ));
        } catch (IllegalArgumentException error) {
            state = SleepMaintenanceReport.State.FAILED;
        }
        return new SleepMaintenanceReport(
                preferences.getBoolean(PREF_AUTOMATIC, false),
                preferences.getBoolean(PREF_PHOTO_CLEANUP, false),
                preferences.getBoolean(PREF_REQUESTED_PENDING, false),
                state,
                preferences.getLong(PREF_STATE_AT, 0L),
                preferences.getLong(PREF_COMPLETED_AT, 0L),
                preferences.getInt(PREF_PEOPLE, 0),
                preferences.getInt(PREF_PETS, 0),
                preferences.getInt(PREF_FACES, 0),
                preferences.getInt(PREF_RECORDS_MERGED, 0),
                preferences.getInt(PREF_LIKES_REMOVED, 0),
                preferences.getInt(PREF_PHOTOS_RETAINED, 0),
                preferences.getInt(PREF_PHOTOS_DELETED, 0),
                preferences.getInt(PREF_PHOTO_GROUPS, 0)
        );
    }

    synchronized boolean setAutomaticEnabled(boolean enabled) {
        return preferences.edit()
                .putBoolean(PREF_AUTOMATIC, enabled)
                .commit();
    }

    synchronized boolean setPhotoCleanupEnabled(boolean enabled) {
        return preferences.edit()
                .putBoolean(PREF_PHOTO_CLEANUP, enabled)
                .commit();
    }

    synchronized void markState(SleepMaintenanceReport.State state) {
        preferences.edit()
                .putString(PREF_STATE, state.name())
                .putLong(PREF_STATE_AT, System.currentTimeMillis())
                .commit();
    }

    synchronized void markRequestedQueued() {
        preferences.edit()
                .putBoolean(PREF_REQUESTED_PENDING, true)
                .putString(
                        PREF_STATE,
                        SleepMaintenanceReport.State.QUEUED.name()
                )
                .putLong(PREF_STATE_AT, System.currentTimeMillis())
                .commit();
    }

    synchronized void markRequestedStarted() {
        preferences.edit()
                .putBoolean(PREF_REQUESTED_PENDING, false)
                .commit();
    }

    synchronized void markRequestedCancelled() {
        preferences.edit()
                .putBoolean(PREF_REQUESTED_PENDING, false)
                .putString(
                        PREF_STATE,
                        SleepMaintenanceReport.State.CANCELLED.name()
                )
                .putLong(PREF_STATE_AT, System.currentTimeMillis())
                .commit();
    }

    synchronized void markRequestedFailed() {
        preferences.edit()
                .putBoolean(PREF_REQUESTED_PENDING, false)
                .putString(
                        PREF_STATE,
                        SleepMaintenanceReport.State.FAILED.name()
                )
                .putLong(PREF_STATE_AT, System.currentTimeMillis())
                .commit();
    }

    synchronized void markSuccess(
            int people,
            int pets,
            int faces,
            int recordsMerged,
            int likesRemoved,
            int photosRetained,
            int photosDeleted,
            int photoGroups
    ) {
        long now = System.currentTimeMillis();
        preferences.edit()
                .putString(PREF_STATE, SleepMaintenanceReport.State.SUCCESS.name())
                .putLong(PREF_STATE_AT, now)
                .putLong(PREF_COMPLETED_AT, now)
                .putBoolean(PREF_REQUESTED_PENDING, false)
                .putInt(PREF_PEOPLE, people)
                .putInt(PREF_PETS, pets)
                .putInt(PREF_FACES, faces)
                .putInt(PREF_RECORDS_MERGED, recordsMerged)
                .putInt(PREF_LIKES_REMOVED, likesRemoved)
                .putInt(PREF_PHOTOS_RETAINED, photosRetained)
                .putInt(PREF_PHOTOS_DELETED, photosDeleted)
                .putInt(PREF_PHOTO_GROUPS, photoGroups)
                .commit();
    }
}
