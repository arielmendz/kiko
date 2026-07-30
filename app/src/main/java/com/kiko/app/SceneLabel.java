package com.kiko.app;

public final class SceneLabel {
    private final String text;
    private final float confidence;

    public SceneLabel(String text, float confidence) {
        this.text = text;
        this.confidence = confidence;
    }

    public String getText() {
        return text;
    }

    public float getConfidence() {
        return confidence;
    }
}
