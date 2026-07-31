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
        assertEquals(6, ModelCatalog.getModels().size());
        assertNotNull(ModelCatalog.findById("gemma-3-1b"));
        assertNotNull(ModelCatalog.findById("bonsai-1.7b"));
        assertNotNull(ModelCatalog.findById("qwen3-0.6b"));
        assertNotNull(ModelCatalog.findById("lfm2.5-350m"));
        assertNotNull(ModelCatalog.findById(ModelCatalog.VISION_MODEL_ID));
        assertNotNull(ModelCatalog.findById(ModelCatalog.FACE_MODEL_ID));
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
            } else if (model.getPurpose() == ModelPurpose.VISION) {
                assertEquals(ModelCatalog.VISION_MODEL_ID, model.getId());
                assertTrue(model.getFilename().endsWith(".onnx"));
                assertTrue(model.getDownloadUrl().contains("/releases/assets/"));
            } else {
                assertEquals(ModelPurpose.FACE_RECOGNITION, model.getPurpose());
                assertEquals(ModelCatalog.FACE_MODEL_ID, model.getId());
                assertTrue(model.getFilename().endsWith(".onnx"));
                assertTrue(model.getRevision().matches("[0-9a-f]{40}"));
            }
        }
    }

    @Test
    public void visionArtifactMatchesTheReviewedOfficialPin() {
        ModelSpec vision = ModelCatalog.findById(ModelCatalog.VISION_MODEL_ID);

        assertNotNull(vision);
        assertEquals(ModelPurpose.VISION, vision.getPurpose());
        assertEquals("398736502", vision.getRevision());
        assertEquals(9_941_957L, vision.getByteSize());
        assertEquals(
                "2e947b787d9e787b93a16772a5f55b1d4d8c4d86f53146149c5d6a642442d6f7",
                vision.getSha256()
        );
        assertEquals("AGPL-3.0", vision.getLicense());
        assertEquals(
                "application/octet-stream",
                vision.getDownloadHeaders().get("Accept")
        );
    }

    @Test
    public void faceArtifactMatchesTheReviewedOfficialPin() {
        ModelSpec face = ModelCatalog.findById(ModelCatalog.FACE_MODEL_ID);

        assertNotNull(face);
        assertEquals(ModelPurpose.FACE_RECOGNITION, face.getPurpose());
        assertEquals(
                "89e1f6f89ab68a12ab974b5b65162abf464a461f",
                face.getRevision()
        );
        assertEquals(38_696_353L, face.getByteSize());
        assertEquals(
                "0ba9fbfa01b5270c96627c4ef784da859931e02f04419c829e83484087c34e79",
                face.getSha256()
        );
        assertEquals("Apache-2.0", face.getLicense());
    }

    @Test
    public void onlyGemmaRequiresAuthentication() {
        for (ModelSpec model : ModelCatalog.getModels()) {
            assertEquals("gemma-3-1b".equals(model.getId()), model.isGated());
        }
    }
}
