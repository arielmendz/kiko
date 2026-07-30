package com.kiko.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;

import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class OfflineSpanishSpeaker implements AutoCloseable {
    private static final long INITIALIZATION_TIMEOUT_MS = 5_000L;

    public interface Callback {
        void onFinished();

        void onUnavailable();
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable failPendingInitialization = this::failPendingInitialization;
    private final TextToSpeech textToSpeech;
    private boolean initialized;
    private boolean closed;
    private Voice offlineSpanishVoice;
    private PendingSpeech pendingSpeech;
    private Callback activeCallback;

    public OfflineSpanishSpeaker(Context context) {
        textToSpeech = new TextToSpeech(
                context.getApplicationContext(),
                this::handleInitialization
        );
        textToSpeech.setOnUtteranceProgressListener(
                new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        finishActiveSpeech();
                    }

                    @Override
                    public void onError(String utteranceId) {
                        failActiveSpeech();
                    }
                }
        );
    }

    public void speak(String text, Callback callback) {
        if (closed) {
            callback.onUnavailable();
            return;
        }
        if (!initialized) {
            pendingSpeech = new PendingSpeech(text, callback);
            mainHandler.removeCallbacks(failPendingInitialization);
            mainHandler.postDelayed(
                    failPendingInitialization,
                    INITIALIZATION_TIMEOUT_MS
            );
            return;
        }
        speakWhenReady(text, callback);
    }

    public void stop() {
        pendingSpeech = null;
        activeCallback = null;
        mainHandler.removeCallbacks(failPendingInitialization);
        textToSpeech.stop();
    }

    @Override
    public void close() {
        closed = true;
        stop();
        textToSpeech.shutdown();
    }

    private void handleInitialization(int status) {
        initialized = true;
        mainHandler.removeCallbacks(failPendingInitialization);
        if (closed) {
            return;
        }

        if (status == TextToSpeech.SUCCESS) {
            offlineSpanishVoice = selectOfflineSpanishVoice(textToSpeech.getVoices());
            if (offlineSpanishVoice != null) {
                textToSpeech.setVoice(offlineSpanishVoice);
                textToSpeech.setPitch(0.72f);
                textToSpeech.setSpeechRate(0.88f);
            }
        }

        PendingSpeech waiting = pendingSpeech;
        pendingSpeech = null;
        if (waiting != null) {
            speakWhenReady(waiting.text, waiting.callback);
        }
    }

    private void speakWhenReady(String text, Callback callback) {
        if (offlineSpanishVoice == null) {
            callback.onUnavailable();
            return;
        }

        activeCallback = callback;
        int result = textToSpeech.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                UUID.randomUUID().toString()
        );
        if (result == TextToSpeech.ERROR) {
            activeCallback = null;
            callback.onUnavailable();
        }
    }

    private void finishActiveSpeech() {
        mainHandler.post(() -> {
            Callback callback = activeCallback;
            activeCallback = null;
            if (callback != null && !closed) {
                callback.onFinished();
            }
        });
    }

    private void failActiveSpeech() {
        mainHandler.post(() -> {
            Callback callback = activeCallback;
            activeCallback = null;
            if (callback != null && !closed) {
                callback.onUnavailable();
            }
        });
    }

    private void failPendingInitialization() {
        PendingSpeech waiting = pendingSpeech;
        pendingSpeech = null;
        if (waiting != null && !closed) {
            waiting.callback.onUnavailable();
        }
    }

    static Voice selectOfflineSpanishVoice(Set<Voice> voices) {
        if (voices == null) {
            return null;
        }

        return voices.stream()
                .filter(voice -> !voice.isNetworkConnectionRequired())
                .filter(voice -> "es".equals(voice.getLocale().getLanguage()))
                .filter(voice -> !voice.getFeatures().contains(
                        TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED
                ))
                .min(Comparator.comparingInt(OfflineSpanishSpeaker::voiceRank))
                .orElse(null);
    }

    private static int voiceRank(Voice voice) {
        Locale locale = voice.getLocale();
        if ("US".equals(locale.getCountry())) {
            return 0;
        }
        if (!locale.getCountry().isEmpty()) {
            return 1;
        }
        return 2;
    }

    private static final class PendingSpeech {
        private final String text;
        private final Callback callback;

        private PendingSpeech(String text, Callback callback) {
            this.text = text;
            this.callback = callback;
        }
    }
}
