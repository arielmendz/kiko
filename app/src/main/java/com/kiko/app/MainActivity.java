package com.kiko.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionSupport;
import android.speech.RecognitionSupportCallback;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.camera.view.PreviewView;

import java.util.ArrayList;
import java.util.Arrays;

public final class MainActivity extends ComponentActivity implements RecognitionListener {
    private static final String TAG = "KikoSpeech";
    private static final int MICROPHONE_PERMISSION_REQUEST = 100;
    private static final int CAMERA_PERMISSION_REQUEST = 101;
    private static final long NORMAL_RESTART_DELAY_MS = 1_000L;
    private static final long BUSY_RESTART_DELAY_MS = 2_000L;
    private static final long COMMAND_WINDOW_MS = 10_000L;
    private static final long PERSON_NAME_WINDOW_MS = 12_000L;
    private static final long PERSON_NAME_RETRY_DELAY_MS = 600L;
    private static final int MAX_PERSON_NAME_ATTEMPTS = 2;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable restartListening = this::startListening;
    private final Runnable expireCommandWindow = this::expireCommandWindow;
    private final Runnable expirePersonNameWindow = this::expirePersonNameWindow;
    private final Runnable retryPersonNameListening = this::startPersonNameAttempt;

    private TextView statusView;
    private TextView detailView;
    private KikoEyesView eyesView;
    private PreviewView cameraPreview;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private SceneCameraCapture sceneCameraCapture;
    private LocalVisionEngine localVisionEngine;
    private FaceIdentityStore faceIdentityStore;
    private OfflineSpanishSpeaker offlineSpanishSpeaker;
    private String recognitionLanguage = SpeechLanguageSelector.PREFERRED_SPANISH;
    private boolean activityStarted;
    private boolean listening;
    private boolean wakeWordDetected;
    private boolean sceneRequestInProgress;
    private boolean cameraPermissionPending;
    private boolean supportCheckInProgress;
    private boolean modelDownloadRequested;
    private boolean awaitingPersonName;
    private int personNameAttempts;
    private String pendingPersonHistoryId;
    private float[] pendingPersonEmbedding;
    private AlertDialog personNameDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(createContentView());

        sceneCameraCapture = new SceneCameraCapture(this);
        localVisionEngine = new LocalVisionEngine(this);
        faceIdentityStore = new FaceIdentityStore(this);
        offlineSpanishSpeaker = new OfflineSpanishSpeaker(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, recognitionLanguage)
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
        handler.removeCallbacks(expireCommandWindow);
        handler.removeCallbacks(expirePersonNameWindow);
        handler.removeCallbacks(retryPersonNameListening);
        listening = false;
        sceneRequestInProgress = false;
        awaitingPersonName = false;
        pendingPersonHistoryId = null;
        pendingPersonEmbedding = null;
        cameraPermissionPending = false;
        supportCheckInProgress = false;
        if (personNameDialog != null) {
            personNameDialog.dismiss();
            personNameDialog = null;
        }
        eyesView.setMode(KikoEyeMotion.Mode.RESTING);
        sceneCameraCapture.cancel();
        hideCameraPreview();
        offlineSpanishSpeaker.stop();
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
        sceneCameraCapture.cancel();
        localVisionEngine.close();
        offlineSpanishSpeaker.close();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            cameraPermissionPending = false;
            if (!activityStarted || !sceneRequestInProgress) {
                return;
            }
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                captureAndDescribeScene();
            } else {
                respondToSceneRequest(
                        getString(R.string.scene_camera_permission_response),
                        R.string.detail_camera_permission_denied
                );
            }
            return;
        }

        if (requestCode != MICROPHONE_PERMISSION_REQUEST) {
            return;
        }

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeRecognizerAndListen();
        } else {
            showStatus(R.string.status_permission, false);
        }
    }

    private View createContentView() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.kiko_background));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));
        content.setBackgroundColor(getColor(R.color.kiko_background));

        statusView = new TextView(this);
        statusView.setText(R.string.status_listening);
        statusView.setTextColor(getColor(R.color.kiko_text));
        statusView.setTextSize(36);
        statusView.setGravity(Gravity.CENTER);

        detailView = new TextView(this);
        detailView.setText(R.string.detail_starting);
        detailView.setTextColor(getColor(R.color.kiko_muted));
        detailView.setTextSize(18);
        detailView.setGravity(Gravity.CENTER);
        detailView.setPadding(0, 32, 0, 0);

        eyesView = new KikoEyesView(this);

        cameraPreview = new PreviewView(this);
        cameraPreview.setBackgroundColor(getColor(R.color.kiko_surface));
        cameraPreview.setContentDescription(
                getString(R.string.camera_preview_description)
        );
        cameraPreview.setImplementationMode(
                PreviewView.ImplementationMode.COMPATIBLE
        );
        cameraPreview.setScaleType(PreviewView.ScaleType.FIT_CENTER);
        cameraPreview.setVisibility(View.GONE);

        Button modelsButton = new Button(this);
        modelsButton.setText(R.string.action_models);
        modelsButton.setOnClickListener(view -> startActivity(
                new Intent(this, ModelLibraryActivity.class)
        ));

        Button visualHistoryButton = new Button(this);
        visualHistoryButton.setText(R.string.action_visual_history);
        visualHistoryButton.setOnClickListener(view -> startActivity(
                new Intent(this, VisualHistoryActivity.class)
        ));

        content.addView(
                eyesView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(150)
                )
        );
        content.addView(
                cameraPreview,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(220)
                )
        );
        content.addView(statusView);
        content.addView(detailView);
        content.addView(modelsButton);
        content.addView(visualHistoryButton);
        scroll.addView(
                content,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );
        return scroll;
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
        if (!activityStarted) {
            return;
        }

        if (speechRecognizer == null) {
            if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
                showStatus(R.string.status_unavailable, false);
                showDetail(R.string.detail_local_only);
                return;
            }

            speechRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(this);
        }

        checkLanguageSupportAndListen();
    }

    private void checkLanguageSupportAndListen() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            startListening();
            return;
        }

        supportCheckInProgress = true;
        showStatus(R.string.status_checking_language, false);
        showDetail(R.string.detail_local_only);
        speechRecognizer.checkRecognitionSupport(
                recognizerIntent,
                getMainExecutor(),
                new RecognitionSupportCallback() {
                    @Override
                    public void onSupportResult(RecognitionSupport support) {
                        supportCheckInProgress = false;
                        if (!activityStarted) {
                            return;
                        }
                        handleLanguageSupport(support);
                    }

                    @Override
                    public void onError(int error) {
                        supportCheckInProgress = false;
                        Log.w(TAG, "Unable to check recognition support: " + error);
                        if (activityStarted) {
                            showDetail(getString(
                                    R.string.detail_support_check_failed,
                                    error
                            ));
                            startListening();
                        }
                    }
                }
        );
    }

    @SuppressLint("NewApi")
    private void handleLanguageSupport(RecognitionSupport support) {
        String installed = SpeechLanguageSelector.selectSpanish(
                support.getInstalledOnDeviceLanguages()
        );
        if (installed != null) {
            setRecognitionLanguage(installed);
            showDetail(getString(R.string.detail_language_ready, installed));
            startListening();
            return;
        }

        String pending = SpeechLanguageSelector.selectSpanish(
                support.getPendingOnDeviceLanguages()
        );
        if (pending != null) {
            showStatus(R.string.status_downloading_language, false);
            showDetail(getString(R.string.detail_language_pending, pending));
            return;
        }

        String downloadable = SpeechLanguageSelector.selectSpanish(
                support.getSupportedOnDeviceLanguages()
        );
        if (downloadable != null) {
            setRecognitionLanguage(downloadable);
            requestLanguageModelDownload(downloadable);
            return;
        }

        showStatus(R.string.status_language_unsupported, false);
        showDetail(R.string.detail_language_unsupported);
    }

    private void setRecognitionLanguage(String languageTag) {
        recognitionLanguage = languageTag;
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag);
    }

    private void requestLanguageModelDownload(String languageTag) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            showStatus(R.string.status_language_unsupported, false);
            showDetail(R.string.detail_install_language_manually);
            return;
        }

        showStatus(R.string.status_downloading_language, false);
        showDetail(getString(R.string.detail_language_download, languageTag));
        if (!modelDownloadRequested) {
            modelDownloadRequested = true;
            Log.i(TAG, "Requesting on-device model download for " + languageTag);
            speechRecognizer.triggerModelDownload(recognizerIntent);
        }
    }

    private void startListening() {
        handler.removeCallbacks(restartListening);
        if (!activityStarted
                || listening
                || sceneRequestInProgress
                || supportCheckInProgress
                || speechRecognizer == null) {
            return;
        }

        if (!wakeWordDetected) {
            showStatus(R.string.status_listening, false);
        }
        listening = true;
        eyesView.setMode(KikoEyeMotion.Mode.LISTENING);
        Log.d(TAG, "Starting on-device recognition in " + recognitionLanguage);
        speechRecognizer.startListening(recognizerIntent);
    }

    private void scheduleRestart(long delayMillis) {
        listening = false;
        if (!sceneRequestInProgress) {
            eyesView.setMode(KikoEyeMotion.Mode.RESTING);
        }
        if (activityStarted && speechRecognizer != null) {
            handler.removeCallbacks(restartListening);
            handler.postDelayed(restartListening, delayMillis);
        }
    }

    private void inspectResults(Bundle results) {
        ArrayList<String> hypotheses =
                results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (hypotheses == null || hypotheses.isEmpty()) {
            return;
        }

        Log.d(TAG, "Recognition hypotheses: " + hypotheses);
        showDetail(getString(R.string.detail_heard, hypotheses.get(0)));
        boolean containsWakeWord = WakeWordMatcher.containsKiko(hypotheses);
        if (containsWakeWord) {
            wakeWordDetected = true;
            handler.removeCallbacks(expireCommandWindow);
            handler.postDelayed(expireCommandWindow, COMMAND_WINDOW_MS);
            showStatus(R.string.status_detected, true);
        }
        if (!sceneRequestInProgress
                && (wakeWordDetected || containsWakeWord)
                && SpanishCommandMatcher.containsDescribeScene(hypotheses)) {
            beginSceneRequest();
        }
    }

    private void showStatus(int stringResource, boolean highlighted) {
        statusView.setText(stringResource);
        statusView.setTextColor(getColor(
                highlighted ? R.color.kiko_accent : R.color.kiko_text
        ));
    }

    private void showStatus(String text, boolean highlighted) {
        statusView.setText(text);
        statusView.setTextColor(getColor(
                highlighted ? R.color.kiko_accent : R.color.kiko_text
        ));
    }

    private void showDetail(int stringResource) {
        detailView.setText(stringResource);
    }

    private void showDetail(String text) {
        detailView.setText(text);
    }

    private void beginSceneRequest() {
        sceneRequestInProgress = true;
        eyesView.setMode(KikoEyeMotion.Mode.SQUINTING);
        handler.removeCallbacks(restartListening);
        handler.removeCallbacks(expireCommandWindow);
        listening = false;
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
        }

        if (!localVisionEngine.isModelReady()) {
            respondToSceneRequest(
                    getString(R.string.scene_vision_model_missing_response),
                    R.string.detail_vision_model_missing
            );
            return;
        }

        if (checkSelfPermission(Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            captureAndDescribeScene();
            return;
        }

        cameraPermissionPending = true;
        showStatus(R.string.status_camera_permission, false);
        showDetail(R.string.detail_camera_permission);
        requestPermissions(
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_REQUEST
        );
    }

    private void captureAndDescribeScene() {
        if (!activityStarted || !sceneRequestInProgress || cameraPermissionPending) {
            return;
        }

        showStatus(R.string.status_looking, true);
        showDetail(R.string.detail_camera_saving);
        showCameraPreview();
        sceneCameraCapture.capture(cameraPreview, new SceneCameraCapture.Callback() {
            @Override
            public void onCaptured(android.graphics.Bitmap bitmap, int rotationDegrees) {
                hideCameraPreview();
                if (!activityStarted || !sceneRequestInProgress) {
                    bitmap.recycle();
                    return;
                }
                showStatus(R.string.status_thinking, true);
                localVisionEngine.describe(
                        bitmap,
                        rotationDegrees,
                        System.currentTimeMillis(),
                        new LocalVisionEngine.Callback() {
                            @Override
                            public void onDescription(
                                    String description,
                                    boolean shouldAskPersonName,
                                    String historyRecordId,
                                    float[] enrollmentEmbedding
                            ) {
                                if (activityStarted && sceneRequestInProgress) {
                                    boolean historySaved =
                                            historyRecordId != null;
                                    showHistorySaveWarningIfNeeded(historySaved);
                                    if (shouldAskPersonName
                                            && historySaved
                                            && enrollmentEmbedding != null) {
                                        askForPersonName(
                                                description,
                                                historyRecordId,
                                                enrollmentEmbedding
                                        );
                                    } else {
                                        respondToSceneRequest(
                                                description,
                                                historySaved
                                                        ? R.string.detail_camera_saved
                                                        : R.string
                                                                .detail_visual_history_save_failed
                                        );
                                    }
                                }
                            }

                            @Override
                            public void onModelMissing(boolean historySaved) {
                                if (activityStarted && sceneRequestInProgress) {
                                    showHistorySaveWarningIfNeeded(historySaved);
                                    respondToSceneRequest(
                                            getString(
                                                    R.string
                                                            .scene_vision_model_missing_response
                                            ),
                                            historySaved
                                                    ? R.string
                                                            .detail_vision_model_missing_saved
                                                    : R.string
                                                            .detail_vision_model_missing
                                    );
                                }
                            }

                            @Override
                            public void onError(boolean historySaved) {
                                if (activityStarted && sceneRequestInProgress) {
                                    showHistorySaveWarningIfNeeded(historySaved);
                                    respondToSceneRequest(
                                            getString(R.string.scene_vision_error_response),
                                            historySaved
                                                    ? R.string.detail_vision_error_saved
                                                    : R.string.detail_vision_error
                                    );
                                }
                            }
                        }
                );
            }

            @Override
            public void onError() {
                hideCameraPreview();
                if (activityStarted && sceneRequestInProgress) {
                    respondToSceneRequest(
                            getString(R.string.scene_camera_error_response),
                            R.string.detail_camera_error
                    );
                }
            }
        });
    }

    private void askForPersonName(
            String question,
            String historyRecordId,
            float[] enrollmentEmbedding
    ) {
        pendingPersonHistoryId = historyRecordId;
        pendingPersonEmbedding = Arrays.copyOf(
                enrollmentEmbedding,
                enrollmentEmbedding.length
        );
        showStatus(question, true);
        showDetail(R.string.detail_person_name_listening);
        offlineSpanishSpeaker.speak(
                question,
                new OfflineSpanishSpeaker.Callback() {
                    @Override
                    public void onFinished() {
                        beginPersonNameListening();
                    }

                    @Override
                    public void onUnavailable() {
                        beginPersonNameListening();
                    }
                }
        );
    }

    private void beginPersonNameListening() {
        if (!activityStarted
                || !sceneRequestInProgress
                || pendingPersonHistoryId == null
                || pendingPersonEmbedding == null
                || speechRecognizer == null) {
            awaitingPersonName = false;
            pendingPersonHistoryId = null;
            pendingPersonEmbedding = null;
            return;
        }
        awaitingPersonName = true;
        personNameAttempts = 0;
        handler.removeCallbacks(expirePersonNameWindow);
        handler.postDelayed(expirePersonNameWindow, PERSON_NAME_WINDOW_MS);
        startPersonNameAttempt();
    }

    private void startPersonNameAttempt() {
        if (!activityStarted
                || !sceneRequestInProgress
                || !awaitingPersonName
                || speechRecognizer == null) {
            return;
        }
        if (personNameAttempts >= MAX_PERSON_NAME_ATTEMPTS) {
            cancelPersonNameFlow(R.string.detail_person_name_cancelled);
            return;
        }

        personNameAttempts++;
        listening = true;
        eyesView.setMode(KikoEyeMotion.Mode.LISTENING);
        showDetail(R.string.detail_person_name_listening);
        try {
            speechRecognizer.startListening(recognizerIntent);
        } catch (RuntimeException error) {
            Log.w(TAG, "Unable to listen for a person name", error);
            cancelPersonNameFlow(R.string.detail_person_name_cancelled);
        }
    }

    private void handlePersonNameResults(Bundle results) {
        ArrayList<String> hypotheses =
                results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        listening = false;
        if (SpanishPersonNameExtractor.containsCancel(hypotheses)) {
            cancelPersonNameFlow(R.string.detail_person_name_cancelled);
            return;
        }

        String personName = SpanishPersonNameExtractor.extract(hypotheses);
        if (personName == null) {
            retryPersonNameOrCancel();
            return;
        }
        confirmPersonName(personName);
    }

    private void retryPersonNameOrCancel() {
        if (!awaitingPersonName) {
            return;
        }
        if (personNameAttempts >= MAX_PERSON_NAME_ATTEMPTS) {
            cancelPersonNameFlow(R.string.detail_person_name_cancelled);
            return;
        }
        showDetail(R.string.detail_person_name_invalid);
        handler.removeCallbacks(retryPersonNameListening);
        handler.postDelayed(
                retryPersonNameListening,
                PERSON_NAME_RETRY_DELAY_MS
        );
    }

    private void confirmPersonName(String personName) {
        awaitingPersonName = false;
        listening = false;
        handler.removeCallbacks(expirePersonNameWindow);
        handler.removeCallbacks(retryPersonNameListening);
        eyesView.setMode(KikoEyeMotion.Mode.SQUINTING);
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
        }

        KeyguardManager keyguardManager =
                (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (keyguardManager != null && keyguardManager.isDeviceLocked()) {
            cancelPersonNameFlow(R.string.detail_person_name_locked);
            return;
        }

        personNameDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.person_name_confirm_title)
                .setMessage(getString(
                        R.string.person_name_confirm_message,
                        personName
                ))
                .setNegativeButton(
                        R.string.action_cancel,
                        (dialog, which) -> cancelPersonNameFlow(
                                R.string.detail_person_name_cancelled
                        )
                )
                .setPositiveButton(
                        R.string.action_save,
                        (dialog, which) -> saveConfirmedPersonName(personName)
                )
                .setOnCancelListener(dialog -> cancelPersonNameFlow(
                        R.string.detail_person_name_cancelled
                ))
                .create();
        personNameDialog.show();
    }

    private void saveConfirmedPersonName(String personName) {
        personNameDialog = null;
        String historyRecordId = pendingPersonHistoryId;
        float[] enrollmentEmbedding = pendingPersonEmbedding;
        pendingPersonHistoryId = null;
        pendingPersonEmbedding = null;
        boolean enrolled = historyRecordId != null
                && enrollmentEmbedding != null
                && faceIdentityStore.enroll(
                        historyRecordId,
                        personName,
                        enrollmentEmbedding
                );
        respondToSceneRequest(
                enrolled
                        ? getString(
                                R.string.scene_person_name_saved_response,
                                personName
                        )
                        : getString(R.string.scene_person_name_not_saved_response),
                enrolled
                        ? R.string.detail_person_name_saved
                        : R.string.detail_person_name_save_failed
        );
    }

    private void cancelPersonNameFlow(int detailResource) {
        personNameDialog = null;
        awaitingPersonName = false;
        listening = false;
        pendingPersonHistoryId = null;
        pendingPersonEmbedding = null;
        handler.removeCallbacks(expirePersonNameWindow);
        handler.removeCallbacks(retryPersonNameListening);
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
        }
        if (!activityStarted || !sceneRequestInProgress) {
            return;
        }
        respondToSceneRequest(
                getString(R.string.scene_person_name_not_saved_response),
                detailResource
        );
    }

    private void expirePersonNameWindow() {
        if (awaitingPersonName) {
            cancelPersonNameFlow(R.string.detail_person_name_cancelled);
        }
    }

    private void showCameraPreview() {
        cameraPreview.setVisibility(View.VISIBLE);
    }

    private void hideCameraPreview() {
        cameraPreview.setVisibility(View.GONE);
    }

    private void showHistorySaveWarningIfNeeded(boolean historySaved) {
        if (!historySaved) {
            Toast.makeText(
                    this,
                    R.string.detail_visual_history_save_failed,
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void respondToSceneRequest(String response, int detailResource) {
        hideCameraPreview();
        showStatus(response, true);
        showDetail(detailResource);
        offlineSpanishSpeaker.speak(response, new OfflineSpanishSpeaker.Callback() {
            @Override
            public void onFinished() {
                finishSceneRequest();
            }

            @Override
            public void onUnavailable() {
                showDetail(R.string.detail_offline_tts_unavailable);
                finishSceneRequest();
            }
        });
    }

    private void finishSceneRequest() {
        sceneRequestInProgress = false;
        awaitingPersonName = false;
        pendingPersonHistoryId = null;
        pendingPersonEmbedding = null;
        cameraPermissionPending = false;
        wakeWordDetected = false;
        handler.removeCallbacks(expireCommandWindow);
        handler.removeCallbacks(expirePersonNameWindow);
        handler.removeCallbacks(retryPersonNameListening);
        scheduleRestart(NORMAL_RESTART_DELAY_MS);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void expireCommandWindow() {
        if (sceneRequestInProgress) {
            return;
        }
        wakeWordDetected = false;
        if (activityStarted) {
            showStatus(R.string.status_listening, false);
            showDetail(getString(R.string.detail_language_ready, recognitionLanguage));
        }
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.d(TAG, "Recognizer ready for speech");
        if (awaitingPersonName) {
            eyesView.setMode(KikoEyeMotion.Mode.LISTENING);
            showDetail(R.string.detail_person_name_listening);
            return;
        }
        if (!sceneRequestInProgress) {
            eyesView.setMode(KikoEyeMotion.Mode.LISTENING);
        }
        if (!wakeWordDetected && !sceneRequestInProgress) {
            showStatus(R.string.status_listening, false);
        }
        if (!sceneRequestInProgress) {
            showDetail(getString(R.string.detail_language_ready, recognitionLanguage));
        }
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
        if (awaitingPersonName) {
            eyesView.setMode(KikoEyeMotion.Mode.RESTING);
            return;
        }
        if (!sceneRequestInProgress) {
            eyesView.setMode(KikoEyeMotion.Mode.RESTING);
        }
        Log.d(TAG, "End of speech");
    }

    @Override
    public void onError(int error) {
        Log.w(TAG, "Recognition error: " + error);
        if (awaitingPersonName) {
            listening = false;
            retryPersonNameOrCancel();
            return;
        }
        if (sceneRequestInProgress) {
            listening = false;
            return;
        }
        eyesView.setMode(KikoEyeMotion.Mode.RESTING);
        switch (error) {
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
            case SpeechRecognizer.ERROR_NO_MATCH:
                showDetail(R.string.detail_no_speech);
                scheduleRestart(NORMAL_RESTART_DELAY_MS);
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                showDetail(R.string.detail_recognizer_busy);
                scheduleRestart(BUSY_RESTART_DELAY_MS);
                break;
            case SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE:
                listening = false;
                requestLanguageModelDownload(recognitionLanguage);
                break;
            case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED:
                listening = false;
                showStatus(R.string.status_language_unsupported, false);
                showDetail(getString(
                        R.string.detail_language_not_supported,
                        recognitionLanguage
                ));
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                listening = false;
                showStatus(R.string.status_permission, false);
                showDetail(R.string.detail_permission_denied);
                break;
            default:
                listening = false;
                showStatus(R.string.status_recognition_error, false);
                showDetail(getString(R.string.detail_error_code, error));
                break;
        }
    }

    @Override
    public void onResults(Bundle results) {
        if (awaitingPersonName) {
            handlePersonNameResults(results);
            return;
        }
        inspectResults(results);
        scheduleRestart(NORMAL_RESTART_DELAY_MS);
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        if (awaitingPersonName) {
            return;
        }
        inspectResults(partialResults);
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
    }
}
