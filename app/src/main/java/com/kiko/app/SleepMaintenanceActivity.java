package com.kiko.app;

import android.app.Activity;
import android.app.KeyguardManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;

public final class SleepMaintenanceActivity extends Activity {
    private static final long REFRESH_INTERVAL_MS = 1_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshReport = new Runnable() {
        @Override
        public void run() {
            reload();
            handler.postDelayed(this, REFRESH_INTERVAL_MS);
        }
    };

    private SleepMaintenanceReportStore reportStore;
    private Switch automaticSwitch;
    private Switch photoCleanupSwitch;
    private Button cancelButton;
    private TextView currentState;
    private TextView lastReport;
    private boolean refreshingSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reportStore = new SleepMaintenanceReportStore(this);
        setContentView(createContentView());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!isDeviceUnlocked()) {
            Toast.makeText(
                    this,
                    R.string.sleep_unlock_required,
                    Toast.LENGTH_LONG
            ).show();
            finish();
            return;
        }
        reload();
        handler.postDelayed(refreshReport, REFRESH_INTERVAL_MS);
    }

    @Override
    protected void onStop() {
        handler.removeCallbacks(refreshReport);
        super.onStop();
    }

    private View createContentView() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.kiko_background));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(24));
        root.setBackgroundColor(getColor(R.color.kiko_background));

        Button back = new Button(this);
        back.setText(R.string.action_back);
        back.setOnClickListener(view -> finish());
        root.addView(back, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView title = textView(
                getString(R.string.sleep_title),
                30,
                R.color.kiko_text
        );
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(20), 0, dp(12));
        root.addView(title);

        TextView explanation = textView(
                getString(R.string.sleep_explanation),
                17,
                R.color.kiko_muted
        );
        explanation.setPadding(0, 0, 0, dp(16));
        root.addView(explanation);

        automaticSwitch = new Switch(this);
        automaticSwitch.setText(R.string.sleep_automatic_action);
        automaticSwitch.setTextColor(getColor(R.color.kiko_text));
        automaticSwitch.setTextSize(18);
        automaticSwitch.setOnCheckedChangeListener((button, enabled) -> {
            if (refreshingSwitch) {
                return;
            }
            try {
                if (!SleepMaintenanceScheduler.setAutomaticEnabled(
                        this,
                        enabled
                )) {
                    showSchedulingError();
                }
            } catch (RuntimeException error) {
                showSchedulingError();
            }
            reload();
        });
        root.addView(automaticSwitch);

        TextView constraints = textView(
                getString(R.string.sleep_constraints),
                15,
                R.color.kiko_muted
        );
        constraints.setPadding(0, dp(8), 0, dp(16));
        root.addView(constraints);

        photoCleanupSwitch = new Switch(this);
        photoCleanupSwitch.setText(R.string.sleep_photo_cleanup_action);
        photoCleanupSwitch.setTextColor(getColor(R.color.kiko_text));
        photoCleanupSwitch.setTextSize(18);
        photoCleanupSwitch.setOnCheckedChangeListener((button, enabled) -> {
            if (refreshingSwitch) {
                return;
            }
            if (!reportStore.setPhotoCleanupEnabled(enabled)) {
                showSchedulingError();
            }
            reload();
        });
        root.addView(photoCleanupSwitch);

        TextView photoCleanupExplanation = textView(
                getString(R.string.sleep_photo_cleanup_explanation),
                15,
                R.color.kiko_muted
        );
        photoCleanupExplanation.setPadding(0, dp(8), 0, dp(16));
        root.addView(photoCleanupExplanation);

        Button request = new Button(this);
        request.setText(R.string.sleep_request_action);
        request.setOnClickListener(view -> {
            try {
                SleepMaintenanceScheduler.requestOnce(this);
            } catch (RuntimeException error) {
                showSchedulingError();
            }
            reload();
        });
        root.addView(request);

        cancelButton = new Button(this);
        cancelButton.setText(R.string.sleep_cancel_action);
        cancelButton.setOnClickListener(view -> {
            try {
                SleepMaintenanceScheduler.cancelPendingOnce(this);
            } catch (RuntimeException error) {
                showSchedulingError();
            }
            reload();
        });
        root.addView(cancelButton);

        currentState = textView("", 18, R.color.kiko_accent);
        currentState.setPadding(0, dp(24), 0, dp(12));
        root.addView(currentState);

        lastReport = textView("", 17, R.color.kiko_text);
        root.addView(lastReport);

        TextView boundaries = textView(
                getString(R.string.sleep_boundaries),
                15,
                R.color.kiko_muted
        );
        boundaries.setPadding(0, dp(20), 0, 0);
        root.addView(boundaries);

        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return scroll;
    }

    private void reload() {
        if (automaticSwitch == null) {
            return;
        }
        SleepMaintenanceReport report = reportStore.load();
        refreshingSwitch = true;
        automaticSwitch.setChecked(report.isAutomaticEnabled());
        photoCleanupSwitch.setChecked(report.isPhotoCleanupEnabled());
        refreshingSwitch = false;
        cancelButton.setEnabled(report.isRequestedRunPending());
        currentState.setText(stateText(report));
        if (report.getLastCompletedAtEpochMillis() <= 0L) {
            lastReport.setText(R.string.sleep_no_report);
            return;
        }
        Date completed = new Date(report.getLastCompletedAtEpochMillis());
        String timestamp = DateFormat.getMediumDateFormat(this).format(completed)
                + " · " + DateFormat.getTimeFormat(this).format(completed);
        lastReport.setText(getString(
                R.string.sleep_last_report,
                timestamp,
                report.getPeopleVerified(),
                report.getPetsVerified(),
                report.getFacesVerified(),
                report.getDuplicateRecordsMerged(),
                report.getDuplicateLikesRemoved(),
                report.getPhotosRetained(),
                report.getPhotosDeleted(),
                report.getNamedPhotoGroups()
        ));
    }

    private String stateText(SleepMaintenanceReport report) {
        switch (report.getState()) {
            case QUEUED:
                return getString(R.string.sleep_state_queued);
            case RUNNING:
                return getString(R.string.sleep_state_running);
            case SUCCESS:
                return getString(R.string.sleep_state_success);
            case FAILED:
                return getString(R.string.sleep_state_failed);
            case CANCELLED:
                return getString(R.string.sleep_state_cancelled);
            case NEVER:
            default:
                return getString(R.string.sleep_state_never);
        }
    }

    private boolean isDeviceUnlocked() {
        KeyguardManager keyguardManager =
                (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        return keyguardManager == null || !keyguardManager.isDeviceLocked();
    }

    private void showSchedulingError() {
        Toast.makeText(
                this,
                R.string.sleep_scheduling_failed,
                Toast.LENGTH_LONG
        ).show();
    }

    private TextView textView(String text, int sizeSp, int colorResource) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sizeSp);
        view.setTextColor(getColor(colorResource));
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
