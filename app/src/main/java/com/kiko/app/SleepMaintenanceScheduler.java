package com.kiko.app;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

final class SleepMaintenanceScheduler {
    private static final String PERIODIC_WORK_NAME = "kiko_sleep_periodic";
    private static final String ONE_TIME_WORK_NAME = "kiko_sleep_once";

    private SleepMaintenanceScheduler() {
    }

    static boolean setAutomaticEnabled(Context context, boolean enabled) {
        SleepMaintenanceReportStore reportStore =
                new SleepMaintenanceReportStore(context);
        boolean previous = reportStore.load().isAutomaticEnabled();
        if (!reportStore.setAutomaticEnabled(enabled)) {
            return false;
        }
        try {
            WorkManager manager = WorkManager.getInstance(
                    context.getApplicationContext()
            );
            if (!enabled) {
                manager.cancelUniqueWork(PERIODIC_WORK_NAME);
                return true;
            }
            PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                    SleepMaintenanceWorker.class,
                    24,
                    TimeUnit.HOURS,
                    2,
                    TimeUnit.HOURS
            )
                    .setConstraints(sleepConstraints())
                    .addTag(PERIODIC_WORK_NAME)
                    .build();
            manager.enqueueUniquePeriodicWork(
                    PERIODIC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
            );
            return true;
        } catch (RuntimeException error) {
            reportStore.setAutomaticEnabled(previous);
            return false;
        }
    }

    static void requestOnce(Context context) {
        SleepMaintenanceReportStore reportStore =
                new SleepMaintenanceReportStore(context);
        reportStore.markRequestedQueued();
        try {
            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(
                    SleepMaintenanceWorker.class
            )
                    .setInputData(new Data.Builder()
                            .putBoolean(
                                    SleepMaintenanceWorker.INPUT_REQUESTED_RUN,
                                    true
                            )
                            .build())
                    .setConstraints(sleepConstraints())
                    .addTag(ONE_TIME_WORK_NAME)
                    .build();
            WorkManager.getInstance(context.getApplicationContext())
                    .enqueueUniqueWork(
                            ONE_TIME_WORK_NAME,
                            ExistingWorkPolicy.REPLACE,
                            request
                    );
        } catch (RuntimeException error) {
            reportStore.markRequestedFailed();
            throw error;
        }
    }

    static void cancelPendingOnce(Context context) {
        WorkManager.getInstance(context.getApplicationContext())
                .cancelUniqueWork(ONE_TIME_WORK_NAME);
        new SleepMaintenanceReportStore(context).markRequestedCancelled();
    }

    private static Constraints sleepConstraints() {
        return new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresCharging(true)
                .setRequiresDeviceIdle(true)
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .build();
    }
}
