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
    public void containsTheFourDownloadOnlyModels() {
        assertEquals(4, ModelCatalog.getModels().size());
        assertNotNull(ModelCatalog.findById("gemma-3-1b"));
        assertNotNull(ModelCatalog.findById("bonsai-1.7b"));
        assertNotNull(ModelCatalog.findById("qwen3-0.6b"));
        assertNotNull(ModelCatalog.findById("lfm2.5-350m"));
    }

    @Test
    public void entriesArePinnedAndIntegrityChecked() {
        Set<String> ids = new HashSet<>();
        Set<String> filenames = new HashSet<>();
        for (ModelSpec model : ModelCatalog.getModels()) {
            assertTrue(ids.add(model.getId()));
            assertTrue(filenames.add(model.getFilename()));
            assertTrue(model.getFilename().endsWith(".gguf"));
            assertTrue(model.getByteSize() > 0L);
            assertTrue(model.getRevision().matches("[0-9a-f]{40}"));
            assertTrue(model.getSha256().matches("[0-9a-f]{64}"));
            assertTrue(model.getDownloadUrl().contains(model.getRevision()));
            assertFalse(model.getDownloadUrl().contains("/resolve/main/"));
        }
    }

    @Test
    public void onlyGemmaRequiresAuthentication() {
        for (ModelSpec model : ModelCatalog.getModels()) {
            assertEquals("gemma-3-1b".equals(model.getId()), model.isGated());
        }
    }
}
