package com.kiko.app;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Yolo26DetectionParser {
    private static final int BOX_X1 = 0;
    private static final int BOX_Y1 = 1;
    private static final int BOX_X2 = 2;
    private static final int BOX_Y2 = 3;
    private static final int CONFIDENCE = 4;
    private static final int CATEGORY = 5;
    private static final int VALUES_PER_DETECTION = 6;
    private static final float MIN_CONFIDENCE = 0.40f;

    // Contiguous COCO order declared in the pinned YOLO26n ONNX metadata.
    private static final String[] LABELS = {
            "person", "bicycle", "car", "motorcycle", "airplane", "bus",
            "train", "truck", "boat", "traffic light", "fire hydrant",
            "stop sign", "parking meter", "bench", "bird", "cat", "dog",
            "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe",
            "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
            "skis", "snowboard", "sports ball", "kite", "baseball bat",
            "baseball glove", "skateboard", "surfboard", "tennis racket",
            "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl",
            "banana", "apple", "sandwich", "orange", "broccoli", "carrot",
            "hot dog", "pizza", "donut", "cake", "chair", "couch",
            "potted plant", "bed", "dining table", "toilet", "tv", "laptop",
            "mouse", "remote", "keyboard", "cell phone", "microwave", "oven",
            "toaster", "sink", "refrigerator", "book", "clock", "vase",
            "scissors", "teddy bear", "hair drier", "toothbrush"
    };

    private Yolo26DetectionParser() {
    }

    public static List<SceneLabel> parse(
            FloatBuffer output,
            int detectionCount,
            int valuesPerDetection
    ) {
        if (output == null
                || detectionCount <= 0
                || valuesPerDetection != VALUES_PER_DETECTION) {
            return Collections.emptyList();
        }

        FloatBuffer values = output.duplicate();
        values.rewind();
        int availableDetections = Math.min(
                detectionCount,
                values.remaining() / valuesPerDetection
        );

        List<SceneLabel> labels = new ArrayList<>();
        for (int index = 0; index < availableDetections; index++) {
            int offset = index * valuesPerDetection;
            float x1 = values.get(offset + BOX_X1);
            float y1 = values.get(offset + BOX_Y1);
            float x2 = values.get(offset + BOX_X2);
            float y2 = values.get(offset + BOX_Y2);
            float score = values.get(offset + CONFIDENCE);
            float rawCategory = values.get(offset + CATEGORY);
            int category = Math.round(rawCategory);

            if (!Float.isFinite(x1)
                    || !Float.isFinite(y1)
                    || !Float.isFinite(x2)
                    || !Float.isFinite(y2)
                    || x2 <= x1
                    || y2 <= y1
                    || !Float.isFinite(score)
                    || score < MIN_CONFIDENCE
                    || score > 1f
                    || !Float.isFinite(rawCategory)
                    || Math.abs(rawCategory - category) > 0.001f
                    || category < 0
                    || category >= LABELS.length) {
                continue;
            }
            labels.add(new SceneLabel(LABELS[category], score));
        }
        return labels;
    }
}
