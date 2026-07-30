package com.kiko.app;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import org.junit.Test;

public final class CocoDetectionParserTest {
    @Test
    public void mapsReviewedCocoIndexesAndFiltersWeakDetections() {
        List<SceneLabel> labels = CocoDetectionParser.parse(
                floats(0f, 17f, 62f),
                floats(0.95f, 0.81f, 0.39f),
                floats(3f)
        );

        assertEquals(2, labels.size());
        assertEquals("person", labels.get(0).getText());
        assertEquals("dog", labels.get(1).getText());
    }

    @Test
    public void ignoresSparseAndOutOfRangeCategories() {
        List<SceneLabel> labels = CocoDetectionParser.parse(
                floats(11f, 500f, 2f),
                floats(0.99f, 0.99f, 0.90f),
                floats(3f)
        );

        assertEquals(1, labels.size());
        assertEquals("car", labels.get(0).getText());
    }

    private static ByteBuffer floats(float... values) {
        ByteBuffer buffer = ByteBuffer.allocate(values.length * Float.BYTES)
                .order(ByteOrder.nativeOrder());
        for (float value : values) {
            buffer.putFloat(value);
        }
        buffer.rewind();
        return buffer;
    }
}
