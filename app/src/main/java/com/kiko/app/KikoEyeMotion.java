package com.kiko.app;

final class KikoEyeMotion {
    private static final long GAZE_PERIOD_MS = 2_400L;
    private static final long BLINK_PERIOD_MS = 3_200L;
    private static final long BLINK_DURATION_MS = 180L;
    private static final float BLINK_MIN_OPENNESS = 0.08f;

    enum Mode {
        RESTING,
        LISTENING,
        SQUINTING
    }

    private KikoEyeMotion() {
    }

    static Sample sample(Mode mode, long elapsedMillis) {
        if (mode == null) {
            throw new IllegalArgumentException("Eye mode is required");
        }
        long safeElapsedMillis = Math.max(0L, elapsedMillis);
        switch (mode) {
            case LISTENING:
                return listeningSample(safeElapsedMillis);
            case SQUINTING:
                return new Sample(0.26f, 0f, 0f);
            case RESTING:
            default:
                return new Sample(0f, 0f, 0f);
        }
    }

    static Mode wakeSessionMode(boolean wakeWordDetected, boolean listening) {
        return wakeWordDetected && listening ? Mode.LISTENING : Mode.RESTING;
    }

    private static Sample listeningSample(long elapsedMillis) {
        double gazeAngle = 2d * Math.PI * elapsedMillis / GAZE_PERIOD_MS;
        float gazeX = (float) Math.sin(gazeAngle);
        float gazeY = 0.12f * (float) Math.cos(gazeAngle);

        long blinkPhase = elapsedMillis % BLINK_PERIOD_MS;
        float openness = 1f;
        if (blinkPhase < BLINK_DURATION_MS) {
            float midpoint = BLINK_DURATION_MS / 2f;
            float distanceFromMidpoint = Math.abs(blinkPhase - midpoint);
            float blinkProgress = distanceFromMidpoint / midpoint;
            openness = BLINK_MIN_OPENNESS
                    + ((1f - BLINK_MIN_OPENNESS) * blinkProgress);
        }
        return new Sample(openness, gazeX, gazeY);
    }

    static final class Sample {
        private final float openness;
        private final float gazeX;
        private final float gazeY;

        private Sample(float openness, float gazeX, float gazeY) {
            this.openness = openness;
            this.gazeX = gazeX;
            this.gazeY = gazeY;
        }

        float getOpenness() {
            return openness;
        }

        float getGazeX() {
            return gazeX;
        }

        float getGazeY() {
            return gazeY;
        }
    }
}
