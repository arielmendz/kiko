package com.kiko.app;

import java.io.File;

final class LegacyModelArtifactCleaner {
    private static final String[] OBSOLETE_FILENAMES = {
            "efficientdet-lite0-v1.tflite",
            "efficientdet-lite0-v1.tflite.part"
    };

    private LegacyModelArtifactCleaner() {
    }

    static boolean clean(File modelsDirectory) {
        if (modelsDirectory == null || !modelsDirectory.exists()) {
            return true;
        }

        boolean cleaned = true;
        for (String filename : OBSOLETE_FILENAMES) {
            File artifact = new File(modelsDirectory, filename);
            if (artifact.exists() && !artifact.delete()) {
                cleaned = false;
            }
        }
        return cleaned;
    }
}
