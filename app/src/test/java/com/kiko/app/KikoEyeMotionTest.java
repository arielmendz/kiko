package com.kiko.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class KikoEyeMotionTest {
    private static final float TOLERANCE = 0.001f;

    @Test
    public void listeningMovesFromRightToLeft() {
        KikoEyeMotion.Sample right = KikoEyeMotion.sample(
                KikoEyeMotion.Mode.LISTENING,
                600L
        );
        KikoEyeMotion.Sample left = KikoEyeMotion.sample(
                KikoEyeMotion.Mode.LISTENING,
                1_800L
        );

        assertEquals(1f, right.getGazeX(), TOLERANCE);
        assertEquals(-1f, left.getGazeX(), TOLERANCE);
    }

    @Test
    public void listeningBlinkClosesAndReopens() {
        KikoEyeMotion.Sample open = KikoEyeMotion.sample(
                KikoEyeMotion.Mode.LISTENING,
                0L
        );
        KikoEyeMotion.Sample closed = KikoEyeMotion.sample(
                KikoEyeMotion.Mode.LISTENING,
                90L
        );
        KikoEyeMotion.Sample reopened = KikoEyeMotion.sample(
                KikoEyeMotion.Mode.LISTENING,
                180L
        );

        assertEquals(1f, open.getOpenness(), TOLERANCE);
        assertTrue(closed.getOpenness() < 0.1f);
        assertEquals(1f, reopened.getOpenness(), TOLERANCE);
    }

    @Test
    public void squintIsNarrowerThanRestingEyes() {
        float resting = KikoEyeMotion.sample(
                KikoEyeMotion.Mode.RESTING,
                0L
        ).getOpenness();
        float squinting = KikoEyeMotion.sample(
                KikoEyeMotion.Mode.SQUINTING,
                0L
        ).getOpenness();

        assertTrue(squinting < resting);
    }
}
