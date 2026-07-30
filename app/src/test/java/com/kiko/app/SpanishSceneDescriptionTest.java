package com.kiko.app;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public final class SpanishSceneDescriptionTest {
    @Test
    public void describesDetectedObjectsByConfidenceInSpanish() {
        String description = SpanishSceneDescription.describe(Arrays.asList(
                new SceneLabel("chair", 0.81f),
                new SceneLabel("person", 0.96f),
                new SceneLabel("dog", 0.90f)
        ));

        assertEquals(
                "Veo una persona, un perro y una silla.",
                description
        );
    }

    @Test
    public void countsRepeatedDetections() {
        String description = SpanishSceneDescription.describe(Arrays.asList(
                new SceneLabel("person", 0.92f),
                new SceneLabel("person", 0.88f),
                new SceneLabel("chair", 0.76f)
        ));

        assertEquals("Veo 2 personas y una silla.", description);
    }

    @Test
    public void ignoresUnknownAndLowConfidenceLabels() {
        String description = SpanishSceneDescription.describe(Arrays.asList(
                new SceneLabel("Unknown model label", 0.99f),
                new SceneLabel("person", 0.39f)
        ));

        assertEquals(
                "Veo algo, pero todavía no sé describirlo bien.",
                description
        );
    }

    @Test
    public void handlesNoLabels() {
        assertEquals(
                "No logro distinguir qué hay delante de mí.",
                SpanishSceneDescription.describe(Collections.emptyList())
        );
    }
}
