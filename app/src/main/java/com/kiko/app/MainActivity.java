package com.kiko.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;

public final class MainActivity extends Activity implements RecognitionListener {
    private static final int MICROPHONE_PERMISSION_REQUEST = 100;
    private static final long RESTART_DELAY_MS = 350L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable restartListening = this::startListening;

    private TextView statusView;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private boolean activityStarted;
    private boolean listening;
    private boolean wakeWordDetected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        statusView = createStatusView();
        setContentView(statusView);

        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es")
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
    }

    @Override
    protected void onStart() {
        super.onStart();
        activityStarted = true;
        ensurePermissionAndListen();
    }

    @Override
    protected void onStop() {
        activityStarted = false;
        handler.removeCallbacks(restartListening);
        listening = false;
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != MICROPHONE_PERMISSION_REQUEST) {
            return;
        }

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeRecognizerAndListen();
        } else {
            showStatus(R.string.status_permission, false);
        }
    }

    private TextView createStatusView() {
        TextView view = new TextView(this);
        view.setText(R.string.status_listening);
        view.setTextColor(getColor(R.color.kiko_text));
        view.setTextSize(36);
        view.setGravity(Gravity.CENTER);
        view.setPadding(32, 32, 32, 32);
        view.setBackgroundColor(getColor(R.color.kiko_background));
        view.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        return view;
    }

    private void ensurePermissionAndListen() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            initializeRecognizerAndListen();
            return;
        }

        showStatus(R.string.status_permission, false);
        requestPermissions(
                new String[]{Manifest.permission.RECORD_AUDIO},
                MICROPHONE_PERMISSION_REQUEST
        );
    }

    private void initializeRecognizerAndListen() {
        if (!activityStarted || speechRecognizer != null) {
            startListening();
            return;
        }

        if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
            showStatus(R.string.status_unavailable, false);
            return;
        }

        speechRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(this);
        startListening();
    }

    private void startListening() {
        handler.removeCallbacks(restartListening);
        if (!activityStarted || listening || speechRecognizer == null) {
            return;
        }

        if (!wakeWordDetected) {
            showStatus(R.string.status_listening, false);
        }
        listening = true;
        speechRecognizer.startListening(recognizerIntent);
    }

    private void scheduleRestart() {
        listening = false;
        if (activityStarted && speechRecognizer != null) {
            handler.removeCallbacks(restartListening);
            handler.postDelayed(restartListening, RESTART_DELAY_MS);
        }
    }

    private void inspectResults(Bundle results) {
        ArrayList<String> hypotheses =
                results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (WakeWordMatcher.containsKiko(hypotheses)) {
            wakeWordDetected = true;
            showStatus(R.string.status_detected, true);
        }
    }

    private void showStatus(int stringResource, boolean highlighted) {
        statusView.setText(stringResource);
        statusView.setTextColor(getColor(
                highlighted ? R.color.kiko_accent : R.color.kiko_text
        ));
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    @Override
    public void onRmsChanged(float rmsdB) {
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
    }

    @Override
    public void onEndOfSpeech() {
        listening = false;
    }

    @Override
    public void onError(int error) {
        scheduleRestart();
    }

    @Override
    public void onResults(Bundle results) {
        inspectResults(results);
        scheduleRestart();
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        inspectResults(partialResults);
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
    }
}
