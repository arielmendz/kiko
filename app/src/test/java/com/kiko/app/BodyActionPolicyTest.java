package com.kiko.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class BodyActionPolicyTest {
    private final BodyActionPolicy policy = new BodyActionPolicy();
    private final BodyCapabilities capabilities = BodyCapabilities.loopback();

    @Test
    public void authorizesOnlyCapabilityBoundedSteps() {
        BodyActionPolicy.Decision allowed = policy.authorize(
                BodyActionRequest.moveSteps(6),
                capabilities,
                "move-1"
        );
        BodyActionPolicy.Decision rejected = policy.authorize(
                BodyActionRequest.moveSteps(7),
                capabilities,
                "move-2"
        );

        assertTrue(allowed.isAllowed());
        assertEquals(Integer.valueOf(6), allowed.getCommand().getStepCount());
        assertFalse(rejected.isAllowed());
        assertEquals(
                BodyActionPolicy.Rejection.COUNT_OUT_OF_RANGE,
                rejected.getRejection()
        );
    }

    @Test
    public void authorizesOnlyAllowlistedDanceAndNativeStop() {
        assertTrue(policy.authorize(
                BodyActionRequest.dance(),
                capabilities,
                "dance-1"
        ).isAllowed());
        assertTrue(policy.authorize(
                BodyActionRequest.stop(),
                capabilities,
                "stop-1"
        ).isAllowed());
    }
}
