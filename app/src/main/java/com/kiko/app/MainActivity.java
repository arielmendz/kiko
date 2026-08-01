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
import android.os.SystemClock;
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
import java.util.UUID;

public final class MainActivity extends ComponentActivity implements RecognitionListener {
    private static final String TAG = "KikoSpeech";
    private static final int MICROPHONE_PERMISSION_REQUEST = 100;
    private static final int CAMERA_PERMISSION_REQUEST = 101;
    private static final long NORMAL_RESTART_DELAY_MS = 1_000L;
    private static final long BUSY_RESTART_DELAY_MS = 2_000L;
    private static final long COMMAND_WINDOW_MS = 10_000L;
    private static final long PERSON_NAME_WINDOW_MS = 12_000L;
    private static final long PERSON_NAME_RETRY_DELAY_MS = 600L;
    private static final long BODY_TRANSPORT_POLL_MS = 100L;
    private static final int MAX_PERSON_NAME_ATTEMPTS = 2;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable restartListening = this::startListening;
    private final Runnable expireCommandWindow = this::expireCommandWindow;
    private final Runnable expirePersonNameWindow = this::expirePersonNameWindow;
    private final Runnable retryPersonNameListening = this::startPersonNameAttempt;
    private final Runnable bodySimulationTick = this::pollBodySimulation;

    private TextView statusView;
    private TextView detailView;
    private KikoEyesView eyesView;
    private PreviewView cameraPreview;
    private Button emergencyStopButton;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private SceneCameraCapture sceneCameraCapture;
    private LocalVisionEngine localVisionEngine;
    private FaceIdentityStore faceIdentityStore;
    private VisualHistoryStore visualHistoryStore;
    private PersonMemoryStore personMemoryStore;
    private PetMemoryStore petMemoryStore;
    private OfflineSpanishSpeaker offlineSpanishSpeaker;
    private final BodyActionPolicy bodyActionPolicy = new BodyActionPolicy();
    private BodyTransport bodyTransport;
    private BodyActionRequest activeBodyAction;
    private String recognitionLanguage = SpeechLanguageSelector.PREFERRED_SPANISH;
    private boolean activityStarted;
    private boolean listening;
    private boolean wakeWordDetected;
    private boolean sceneRequestInProgress;
    private boolean personMemoryRequestInProgress;
    private boolean bodyCommandInProgress;
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
        bodyTransport = new LoopbackBodyTransport();
        setContentView(createContentView());

        sceneCameraCapture = new SceneCameraCapture(this);
        localVisionEngine = new LocalVisionEngine(this);
        faceIdentityStore = new FaceIdentityStore(this);
        visualHistoryStore = new VisualHistoryStore(this);
        personMemoryStore = new PersonMemoryStore(this);
        petMemoryStore = new PetMemoryStore(this);
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
        handler.removeCallbacks(bodySimulationTick);
        listening = false;
        sceneRequestInProgress = false;
        personMemoryRequestInProgress = false;
        bodyCommandInProgress = false;
        activeBodyAction = null;
        if (bodyTransport != null) {
            bodyTransport.disconnect();
        }
        if (emergencyStopButton != null) {
            emergencyStopButton.setVisibility(View.GONE);
        }
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

        TextView bodyModeView = new TextView(this);
        bodyModeView.setText(R.string.body_simulator_mode);
        bodyModeView.setTextColor(getColor(R.color.kiko_accent));
        bodyModeView.setTextSize(16);
        bodyModeView.setGravity(Gravity.CENTER);
        bodyModeView.setPadding(0, dp(20), 0, dp(8));

        emergencyStopButton = new Button(this);
        emergencyStopButton.setText(R.string.action_stop_simulation);
        emergencyStopButton.setVisibility(View.GONE);
        emergencyStopButton.setOnClickListener(view -> stopBodySimulation());

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

        Button personMemoriesButton = new Button(this);
        personMemoriesButton.setText(R.string.action_person_memories);
        personMemoriesButton.setOnClickListener(view -> startActivity(
                new Intent(this, PersonMemoryActivity.class)
        ));

        Button sleepButton = new Button(this);
        sleepButton.setText(R.string.action_sleep);
        sleepButton.setOnClickListener(view -> startActivity(
                new Intent(this, SleepMaintenanceActivity.class)
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
        content.addView(bodyModeView);
        content.addView(emergencyStopButton);
        content.addView(modelsButton);
        content.addView(visualHistoryButton);
        content.addView(personMemoriesButton);
        content.addView(sleepButton);
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
                || personMemoryRequestInProgress
                || (bodyCommandInProgress && !bodyTransport.isActive())
                || supportCheckInProgress
                || speechRecognizer == null) {
            return;
        }

        if (!wakeWordDetected && !bodyCommandInProgress) {
            showStatus(R.string.status_listening, false);
        }
        listening = true;
        eyesView.setMode(KikoEyeMotion.wakeSessionMode(wakeWordDetected, listening));
        Log.d(TAG, "Starting on-device recognition in " + recognitionLanguage);
        speechRecognizer.startListening(recognizerIntent);
    }

    private void scheduleRestart(long delayMillis) {
        listening = false;
        if (!sceneRequestInProgress && !personMemoryRequestInProgress) {
            eyesView.setMode(KikoEyeMotion.Mode.RESTING);
        }
        if (activityStarted && speechRecognizer != null) {
            handler.removeCallbacks(restartListening);
            handler.postDelayed(restartListening, delayMillis);
        }
    }

    private void inspectResults(Bundle results, boolean finalResults) {
        ArrayList<String> hypotheses =
                results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (hypotheses == null || hypotheses.isEmpty()) {
            return;
        }

        Log.d(TAG, "Recognition hypotheses received: " + hypotheses.size());
        showDetail(getString(R.string.detail_heard, hypotheses.get(0)));
        if (bodyCommandInProgress
                && bodyTransport.isActive()
                && SpanishBodyCommandParser.containsEmergencyStop(hypotheses)) {
            stopBodySimulation();
            return;
        }
        boolean containsWakeWord = WakeWordMatcher.containsKiko(hypotheses);
        if (containsWakeWord) {
            wakeWordDetected = true;
            eyesView.setMode(KikoEyeMotion.wakeSessionMode(true, listening));
            handler.removeCallbacks(expireCommandWindow);
            handler.postDelayed(expireCommandWindow, COMMAND_WINDOW_MS);
            showStatus(R.string.status_detected, true);
        }
        if (!sceneRequestInProgress
                && !personMemoryRequestInProgress
                && !bodyCommandInProgress
                && (wakeWordDetected || containsWakeWord)
                && SpanishCommandMatcher.containsDescribeScene(hypotheses)) {
            beginSceneRequest();
            return;
        }
        if (finalResults
                && !sceneRequestInProgress
                && !personMemoryRequestInProgress
                && !bodyCommandInProgress
                && (wakeWordDetected || containsWakeWord)) {
            SpanishBodyCommandParser.Result bodyResult =
                    SpanishBodyCommandParser.parse(hypotheses);
            if (bodyResult != null) {
                beginBodyCommand(bodyResult);
                return;
            }
            PetMemoryCommand petMemoryCommand =
                    SpanishPetMemoryParser.parse(hypotheses);
            if (petMemoryCommand != null) {
                beginPetMemoryRequest(petMemoryCommand);
                return;
            }
            PersonMemoryCommand memoryCommand =
                    SpanishPersonMemoryParser.parse(hypotheses);
            if (memoryCommand != null) {
                beginPersonMemoryRequest(memoryCommand);
            }
        }
    }

    private void beginBodyCommand(SpanishBodyCommandParser.Result result) {
        if (!result.hasAction()) {
            int response = result.getIssue()
                    == SpanishBodyCommandParser.Issue.MISSING_STEP_COUNT
                    ? R.string.body_steps_missing_response
                    : R.string.body_steps_invalid_response;
            respondToBodyCommand(getString(response));
            return;
        }

        BodyActionRequest request = result.getAction();
        BodyActionPolicy.Decision decision = bodyActionPolicy.authorize(
                request,
                bodyTransport.getCapabilities(),
                UUID.randomUUID().toString()
        );
        if (!decision.isAllowed()) {
            if (decision.getRejection()
                    == BodyActionPolicy.Rejection.COUNT_OUT_OF_RANGE) {
                int maxSteps = bodyTransport.getCapabilities().getMaxStepsPerCommand();
                respondToBodyCommand(getResources().getQuantityString(
                        R.plurals.body_steps_out_of_range_response,
                        maxSteps,
                        maxSteps
                ));
            } else {
                respondToBodyCommand(getString(
                        R.string.body_action_unavailable_response
                ));
            }
            return;
        }

        if (request.getType() == BodyActionRequest.Type.STOP
                && !bodyTransport.isActive()) {
            respondToBodyCommand(getString(R.string.body_nothing_to_stop_response));
            return;
        }

        handler.removeCallbacks(restartListening);
        handler.removeCallbacks(expireCommandWindow);
        handler.removeCallbacks(bodySimulationTick);
        bodyCommandInProgress = true;
        activeBodyAction = request;
        wakeWordDetected = false;
        listening = false;
        eyesView.setMode(KikoEyeMotion.Mode.RESTING);
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
        }

        BodyEvent event = bodyTransport.send(
                decision.getCommand(),
                SystemClock.elapsedRealtime()
        );
        handleBodyEvent(event);
    }

    private void handleBodyEvent(BodyEvent event) {
        if (event == null) {
            respondToBodyCommand(getString(R.string.body_action_failed_response));
            return;
        }

        switch (event.getType()) {
            case ACCEPTED:
                showBodyActionStarted();
                emergencyStopButton.setVisibility(View.VISIBLE);
                handler.postDelayed(
                        bodySimulationTick,
                        BODY_TRANSPORT_POLL_MS
                );
                scheduleRestart(250L);
                break;
            case COMPLETED:
                respondToBodyCommand(bodyCompletionResponse());
                break;
            case STOPPED:
                respondToBodyCommand(getString(R.string.body_stopped_response));
                break;
            case REJECTED:
            default:
                int response = "body_busy".equals(event.getReason())
                        ? R.string.body_busy_response
                        : R.string.body_action_failed_response;
                respondToBodyCommand(getString(response));
                break;
        }
    }

    private void showBodyActionStarted() {
        if (activeBodyAction != null
                && activeBodyAction.getType() == BodyActionRequest.Type.MOVE_STEPS) {
            int stepCount = activeBodyAction.getStepCount();
            showStatus(getResources().getQuantityString(
                    R.plurals.body_steps_started,
                    stepCount,
                    stepCount
            ), true);
        } else {
            showStatus(R.string.body_dance_started, true);
        }
        showDetail(R.string.body_simulator_detail);
    }

    private String bodyCompletionResponse() {
        if (activeBodyAction != null
                && activeBodyAction.getType() == BodyActionRequest.Type.MOVE_STEPS) {
            int stepCount = activeBodyAction.getStepCount();
            return getResources().getQuantityString(
                    R.plurals.body_steps_completed_response,
                    stepCount,
                    stepCount
            );
        }
        return getString(R.string.body_dance_completed_response);
    }

    private void pollBodySimulation() {
        if (!bodyCommandInProgress || !bodyTransport.isActive()) {
            return;
        }
        BodyEvent event = bodyTransport.tick(SystemClock.elapsedRealtime());
        if (event == null) {
            handler.postDelayed(bodySimulationTick, BODY_TRANSPORT_POLL_MS);
            return;
        }
        handleBodyEvent(event);
    }

    private void stopBodySimulation() {
        if (bodyTransport == null || !bodyTransport.isActive()) {
            if (activityStarted) {
                respondToBodyCommand(getString(R.string.body_nothing_to_stop_response));
            }
            return;
        }
        BodyActionPolicy.Decision decision = bodyActionPolicy.authorize(
                BodyActionRequest.stop(),
                bodyTransport.getCapabilities(),
                UUID.randomUUID().toString()
        );
        if (!decision.isAllowed()) {
            respondToBodyCommand(getString(R.string.body_action_unavailable_response));
            return;
        }
        handler.removeCallbacks(bodySimulationTick);
        BodyEvent event = bodyTransport.send(
                decision.getCommand(),
                SystemClock.elapsedRealtime()
        );
        handleBodyEvent(event);
    }

    private void respondToBodyCommand(String response) {
        bodyCommandInProgress = true;
        handler.removeCallbacks(restartListening);
        handler.removeCallbacks(expireCommandWindow);
        handler.removeCallbacks(bodySimulationTick);
        listening = false;
        emergencyStopButton.setVisibility(View.GONE);
        eyesView.setMode(KikoEyeMotion.Mode.RESTING);
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
        }
        showStatus(response, true);
        showDetail(R.string.body_simulator_detail);
        offlineSpanishSpeaker.speak(response, new OfflineSpanishSpeaker.Callback() {
            @Override
            public void onFinished() {
                finishBodyCommand();
            }

            @Override
            public void onUnavailable() {
                finishBodyCommand();
            }
        });
    }

    private void finishBodyCommand() {
        bodyCommandInProgress = false;
        activeBodyAction = null;
        wakeWordDetected = false;
        handler.removeCallbacks(expireCommandWindow);
        scheduleRestart(NORMAL_RESTART_DELAY_MS);
    }

    private void beginPetMemoryRequest(PetMemoryCommand command) {
        beginMemoryRequest();

        String response;
        int detailResource;
        if (command.isUpdate()) {
            PetMemoryRecord updated = petMemoryStore.apply(command);
            if (updated == null) {
                response = getString(R.string.person_memory_update_failed_response);
                detailResource = R.string.detail_person_memory_update_failed;
            } else {
                response = SpanishPetMemoryResponses.updateResponse(command);
                detailResource = R.string.detail_person_memory_updated;
            }
        } else if (command.getType()
                == PetMemoryCommand.Type.QUERY_OWNER_PETS) {
            response = SpanishPetMemoryResponses.queryResponse(
                    command,
                    null,
                    petMemoryStore.findByOwner(command.getOwnerName())
            );
            detailResource = R.string.detail_person_memory_consulted;
        } else {
            PetMemoryRecord record = petMemoryStore.find(
                    command.getPetName(),
                    command.getKind()
            );
            response = SpanishPetMemoryResponses.queryResponse(
                    command,
                    record,
                    null
            );
            detailResource = R.string.detail_person_memory_consulted;
        }
        respondToPersonMemoryRequest(response, detailResource);
    }

    private void beginPersonMemoryRequest(PersonMemoryCommand command) {
        beginMemoryRequest();

        String response;
        int detailResource;
        if (command.isUpdate()) {
            PersonMemoryRecord updated = personMemoryStore.apply(command);
            if (updated == null) {
                response = getString(R.string.person_memory_update_failed_response);
                detailResource = R.string.detail_person_memory_update_failed;
            } else {
                response = SpanishPersonMemoryResponses.updateResponse(command);
                detailResource = R.string.detail_person_memory_updated;
            }
        } else {
            PersonMemoryRecord record = personMemoryStore.find(
                    command.getPersonName()
            );
            response = SpanishPersonMemoryResponses.queryResponse(
                    command,
                    record
            );
            detailResource = R.string.detail_person_memory_consulted;
        }
        respondToPersonMemoryRequest(response, detailResource);
    }

    private void beginMemoryRequest() {
        personMemoryRequestInProgress = true;
        handler.removeCallbacks(restartListening);
        handler.removeCallbacks(expireCommandWindow);
        listening = false;
        eyesView.setMode(KikoEyeMotion.Mode.RESTING);
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
        }
    }

    private void respondToPersonMemoryRequest(
            String response,
            int detailResource
    ) {
        showStatus(response, true);
        showDetail(detailResource);
        offlineSpanishSpeaker.speak(response, new OfflineSpanishSpeaker.Callback() {
            @Override
            public void onFinished() {
                finishPersonMemoryRequest();
            }

            @Override
            public void onUnavailable() {
                finishPersonMemoryRequest();
            }
        });
    }

    private void finishPersonMemoryRequest() {
        personMemoryRequestInProgress = false;
        wakeWordDetected = false;
        handler.removeCallbacks(expireCommandWindow);
        scheduleRestart(NORMAL_RESTART_DELAY_MS);
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
        if (enrolled) {
            visualHistoryStore.setPersonName(historyRecordId, personName);
        }
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
        if (sceneRequestInProgress
                || personMemoryRequestInProgress
                || bodyCommandInProgress) {
            return;
        }
        wakeWordDetected = false;
        eyesView.setMode(KikoEyeMotion.wakeSessionMode(false, listening));
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
        if (bodyCommandInProgress) {
            eyesView.setMode(KikoEyeMotion.Mode.RESTING);
            return;
        }
        if (!sceneRequestInProgress && !personMemoryRequestInProgress) {
            eyesView.setMode(KikoEyeMotion.wakeSessionMode(
                    wakeWordDetected,
                    listening
            ));
        }
        if (!wakeWordDetected
                && !sceneRequestInProgress
                && !personMemoryRequestInProgress) {
            showStatus(R.string.status_listening, false);
        }
        if (!sceneRequestInProgress && !personMemoryRequestInProgress) {
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
        if (!sceneRequestInProgress && !personMemoryRequestInProgress) {
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
        if (bodyCommandInProgress) {
            listening = false;
            eyesView.setMode(KikoEyeMotion.Mode.RESTING);
            if (bodyTransport.isActive()) {
                scheduleRestart(NORMAL_RESTART_DELAY_MS);
            }
            return;
        }
        if (sceneRequestInProgress || personMemoryRequestInProgress) {
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
        inspectResults(results, true);
        if (!sceneRequestInProgress
                && !personMemoryRequestInProgress
                && (!bodyCommandInProgress || bodyTransport.isActive())) {
            scheduleRestart(NORMAL_RESTART_DELAY_MS);
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        if (awaitingPersonName) {
            return;
        }
        inspectResults(partialResults, false);
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
    }
}
