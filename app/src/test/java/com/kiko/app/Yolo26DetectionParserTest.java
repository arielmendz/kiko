package com.kiko.app;

import static org.junit.Assert.assertEquals;

import java.nio.FloatBuffer;
import java.util.List;

import org.junit.Test;

public final class Yolo26DetectionParserTest {
    @Test
    public void mapsContiguousCocoIndexesAndFiltersWeakDetections() {
        List<SceneLabel> labels = Yolo26DetectionParser.parse(
                detections(
                        row(10f, 20f, 100f, 200f, 0.95f, 0f),
                        row(30f, 40f, 140f, 240f, 0.81f, 16f),
                        row(50f, 60f, 150f, 260f, 0.39f, 62f),
                        row(60f, 70f, 160f, 270f, 0.72f, 79f)
                ),
                4,
                6
        );

        assertEquals(3, labels.size());
        assertEquals("person", labels.get(0).getText());
        assertEquals("dog", labels.get(1).getText());
        assertEquals("toothbrush", labels.get(2).getText());
    }

    @Test
    public void rejectsMalformedBoxesScoresAndCategories() {
        List<SceneLabel> labels = Yolo26DetectionParser.parse(
                detections(
                        row(100f, 20f, 10f, 200f, 0.99f, 0f),
                        row(10f, 20f, 100f, 200f, Float.NaN, 0f),
                        row(10f, 20f, 100f, 200f, 0.99f, 80f),
                        row(10f, 20f, 100f, 200f, 0.99f, 2.5f),
                        row(10f, 20f, 100f, 200f, 0.90f, 2f)
                ),
                5,
                6
        );

        assertEquals(1, labels.size());
        assertEquals("car", labels.get(0).getText());
    }

    @Test
    public void returnsEmptyForUnexpectedTensorWidth() {
        List<SceneLabel> labels = Yolo26DetectionParser.parse(
                detections(row(10f, 20f, 100f, 200f, 0.95f, 0f)),
                1,
                5
        );

        assertEquals(0, labels.size());
    }

    private static float[] row(float... values) {
        return values;
    }

    private static FloatBuffer detections(float[]... rows) {
        FloatBuffer buffer = FloatBuffer.allocate(rows.length * 6);
        for (float[] row : rows) {
            buffer.put(row);
        }
        buffer.rewind();
        return buffer;
    }
}
