package com.kiko.app;

import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public final class SleepMaintenanceWorker extends Worker {
    static final String INPUT_REQUESTED_RUN = "requested_run";

    public SleepMaintenanceWorker(
            @NonNull Context context,
            @NonNull WorkerParameters parameters
    ) {
        super(context, parameters);
    }

    @NonNull
    @Override
    public Result doWork() {
        SleepMaintenanceReportStore reportStore =
                new SleepMaintenanceReportStore(getApplicationContext());
        boolean requestedRun = getInputData().getBoolean(
                INPUT_REQUESTED_RUN,
                false
        );
        if (isThermallyUnsafe()) {
            return Result.retry();
        }
        if (isStopped()) {
            preserveCancellationOrQueue(reportStore, requestedRun);
            return Result.failure();
        }
        if (requestedRun) {
            reportStore.markRequestedStarted();
        }
        reportStore.markState(SleepMaintenanceReport.State.RUNNING);

        MemoryMaintenanceResult people = new PersonMemoryStore(
                getApplicationContext()
        ).maintain();
        if (isStopped()) {
            preserveCancellationOrQueue(reportStore, requestedRun);
            return Result.failure();
        }
        MemoryMaintenanceResult pets = new PetMemoryStore(
                getApplicationContext()
        ).maintain();
        if (isStopped()) {
            preserveCancellationOrQueue(reportStore, requestedRun);
            return Result.failure();
        }
        int faces = new FaceIdentityStore(
                getApplicationContext()
        ).validateForMaintenance();
        if (isStopped()) {
            preserveCancellationOrQueue(reportStore, requestedRun);
            return Result.failure();
        }

        if (!people.isSuccessful() || !pets.isSuccessful() || faces < 0) {
            reportStore.markState(SleepMaintenanceReport.State.FAILED);
            return Result.failure();
        }
        reportStore.markSuccess(
                people.getRecordsAfter(),
                pets.getRecordsAfter(),
                faces,
                people.getDuplicateRecordsMerged()
                        + pets.getDuplicateRecordsMerged(),
                people.getDuplicateLikesRemoved()
                        + pets.getDuplicateLikesRemoved()
        );
        return Result.success();
    }

    private void preserveCancellationOrQueue(
            SleepMaintenanceReportStore reportStore,
            boolean requestedRun
    ) {
        if (reportStore.load().getState()
                != SleepMaintenanceReport.State.CANCELLED) {
            if (requestedRun) {
                reportStore.markRequestedQueued();
            } else {
                reportStore.markState(SleepMaintenanceReport.State.QUEUED);
            }
        }
    }

    private boolean isThermallyUnsafe() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return false;
        }
        PowerManager powerManager = (PowerManager) getApplicationContext()
                .getSystemService(Context.POWER_SERVICE);
        return powerManager != null
                && powerManager.getCurrentThermalStatus()
                >= PowerManager.THERMAL_STATUS_SEVERE;
    }
}
