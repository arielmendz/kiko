package com.kiko.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.Test;

public final class LegacyModelArtifactCleanerTest {
    @Test
    public void deletesOnlyAllowlistedEfficientDetArtifacts() throws Exception {
        File directory = Files.createTempDirectory("kiko-model-cleanup").toFile();
        File obsolete = new File(directory, "efficientdet-lite0-v1.tflite");
        File obsoletePartial = new File(
                directory,
                "efficientdet-lite0-v1.tflite.part"
        );
        File current = new File(directory, "yolo26n.onnx");
        assertTrue(obsolete.createNewFile());
        assertTrue(obsoletePartial.createNewFile());
        assertTrue(current.createNewFile());

        assertTrue(LegacyModelArtifactCleaner.clean(directory));

        assertFalse(obsolete.exists());
        assertFalse(obsoletePartial.exists());
        assertTrue(current.exists());
    }

    @Test
    public void missingDirectoryIsAlreadyClean() {
        assertTrue(LegacyModelArtifactCleaner.clean(null));
    }
}
