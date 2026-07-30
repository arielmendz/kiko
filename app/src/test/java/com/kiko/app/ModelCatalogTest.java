package com.kiko.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public final class ModelCatalogTest {
    @Test
    public void containsLanguageModelsAndTheRunnableVisionModel() {
        assertEquals(5, ModelCatalog.getModels().size());
        assertNotNull(ModelCatalog.findById("gemma-3-1b"));
        assertNotNull(ModelCatalog.findById("bonsai-1.7b"));
        assertNotNull(ModelCatalog.findById("qwen3-0.6b"));
        assertNotNull(ModelCatalog.findById("lfm2.5-350m"));
        assertNotNull(ModelCatalog.findById(ModelCatalog.VISION_MODEL_ID));
    }

    @Test
    public void entriesArePinnedAndIntegrityChecked() {
        Set<String> ids = new HashSet<>();
        Set<String> filenames = new HashSet<>();
        for (ModelSpec model : ModelCatalog.getModels()) {
            assertTrue(ids.add(model.getId()));
            assertTrue(filenames.add(model.getFilename()));
            assertTrue(model.getByteSize() > 0L);
            assertTrue(model.getSha256().matches("[0-9a-f]{64}"));
            assertTrue(model.getDownloadUrl().contains(model.getRevision()));
            assertFalse(model.getDownloadUrl().contains("/resolve/main/"));

            if (model.getPurpose() == ModelPurpose.LANGUAGE) {
                assertTrue(model.getFilename().endsWith(".gguf"));
                assertTrue(model.getRevision().matches("[0-9a-f]{40}"));
            } else {
                assertEquals(ModelCatalog.VISION_MODEL_ID, model.getId());
                assertTrue(model.getFilename().endsWith(".tflite"));
                assertEquals("1", model.getRevision());
            }
        }
    }

    @Test
    public void visionArtifactMatchesTheReviewedOfficialPin() {
        ModelSpec vision = ModelCatalog.findById(ModelCatalog.VISION_MODEL_ID);

        assertNotNull(vision);
        assertEquals(ModelPurpose.VISION, vision.getPurpose());
        assertEquals(4_563_519L, vision.getByteSize());
        assertEquals(
                "2e04c53bfeac0ac2a30c057c7e2a777594ce39baaac35a92f74fb1e8c4fc4e0b",
                vision.getSha256()
        );
        assertEquals("Apache-2.0", vision.getLicense());
    }

    @Test
    public void onlyGemmaRequiresAuthentication() {
        for (ModelSpec model : ModelCatalog.getModels()) {
            assertEquals("gemma-3-1b".equals(model.getId()), model.isGated());
        }
    }
}
