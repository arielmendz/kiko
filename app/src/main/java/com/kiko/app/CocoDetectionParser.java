package com.kiko.app;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public final class CocoDetectionParser {
    private static final float MIN_CONFIDENCE = 0.40f;

    // This order is part of the pinned EfficientDet-Lite0 v1 artifact contract.
    private static final String[] LABELS = {
            "person", "bicycle", "car", "motorcycle", "airplane", "bus",
            "train", "truck", "boat", "traffic light", "fire hydrant", null,
            "stop sign", "parking meter", "bench", "bird", "cat", "dog",
            "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe",
            null, "backpack", "umbrella", null, null, "handbag", "tie",
            "suitcase", "frisbee", "skis", "snowboard", "sports ball", "kite",
            "baseball bat", "baseball glove", "skateboard", "surfboard",
            "tennis racket", "bottle", null, "wine glass", "cup", "fork",
            "knife", "spoon", "bowl", "banana", "apple", "sandwich", "orange",
            "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair",
            "couch", "potted plant", "bed", null, "dining table", null, null,
            "toilet", null, "tv", "laptop", "mouse", "remote", "keyboard",
            "cell phone", "microwave", "oven", "toaster", "sink",
            "refrigerator", null, "book", "clock", "vase", "scissors",
            "teddy bear", "hair drier", "toothbrush"
    };

    private CocoDetectionParser() {
    }

    public static List<SceneLabel> parse(
            ByteBuffer categories,
            ByteBuffer scores,
            ByteBuffer count
    ) {
        categories.rewind();
        scores.rewind();
        count.rewind();

        int available = Math.min(
                categories.remaining() / Float.BYTES,
                scores.remaining() / Float.BYTES
        );
        int detected = count.remaining() >= Float.BYTES
                ? Math.min(Math.max(0, Math.round(count.getFloat())), available)
                : available;

        List<SceneLabel> labels = new ArrayList<>();
        for (int index = 0; index < detected; index++) {
            int category = Math.round(categories.getFloat());
            float score = scores.getFloat();
            if (score < MIN_CONFIDENCE
                    || category < 0
                    || category >= LABELS.length
                    || LABELS[category] == null) {
                continue;
            }
            labels.add(new SceneLabel(LABELS[category], score));
        }
        return labels;
    }
}
