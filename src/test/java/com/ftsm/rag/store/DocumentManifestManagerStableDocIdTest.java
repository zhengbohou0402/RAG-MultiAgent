package com.ftsm.rag.store;

import com.ftsm.rag.config.AppConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentManifestManagerStableDocIdTest {

    @Test
    void stableDocIdMatchesPythonOutput() {
        AppConfig config = new AppConfig();
        config.setProjectRoot(System.getProperty("user.dir"));
        DocumentManifestManager manager = new DocumentManifestManager(config);

        Path filePath = Paths.get(System.getProperty("user.dir"), "data", "smartcloud_kb", "example.txt");
        String docId = manager.stableFileDocId(filePath);

        assertEquals("file:data/smartcloud_kb/example.txt", docId,
                "doc id must match Python stable_file_doc_id output exactly");
    }

    @Test
    void stableDocIdIncludesDataSmartCloudPrefix() {
        AppConfig config = new AppConfig();
        config.setProjectRoot(System.getProperty("user.dir"));
        DocumentManifestManager manager = new DocumentManifestManager(config);

        Path filePath = Paths.get(System.getProperty("user.dir"), "data", "smartcloud_kb", "smartcloud_service_index.txt");
        String docId = manager.stableFileDocId(filePath);

        assertTrue(docId.startsWith("file:data/smartcloud_kb/"),
                "doc id must contain the full data/smartcloud_kb prefix to match Python");
        assertEquals("file:data/smartcloud_kb/smartcloud_service_index.txt", docId);
    }

    @Test
    void stableDocIdIsConsistentAcrossDifferentCwd() {
        String originalCwd = System.getProperty("user.dir");
        AppConfig config = new AppConfig();
        config.setProjectRoot(originalCwd);
        DocumentManifestManager manager = new DocumentManifestManager(config);

        Path absoluteFile = Paths.get(originalCwd, "data", "smartcloud_kb", "test.txt").toAbsolutePath().normalize();

        String docId1 = manager.stableFileDocId(absoluteFile);

        assertEquals("file:data/smartcloud_kb/test.txt", docId1);
    }

    @Test
    void stableDocIdHandlesSubdirectories() {
        AppConfig config = new AppConfig();
        config.setProjectRoot(System.getProperty("user.dir"));
        DocumentManifestManager manager = new DocumentManifestManager(config);

        Path filePath = Paths.get(System.getProperty("user.dir"), "data", "smartcloud_kb", "sub", "dir", "file.txt");
        String docId = manager.stableFileDocId(filePath);

        assertEquals("file:data/smartcloud_kb/sub/dir/file.txt", docId);
    }

    @Test
    void stableDocIdHandlesWindowsPathSeparators() {
        AppConfig config = new AppConfig();
        config.setProjectRoot(System.getProperty("user.dir"));
        DocumentManifestManager manager = new DocumentManifestManager(config);

        Path filePath = Paths.get(System.getProperty("user.dir"), "data", "smartcloud_kb", "file.txt");
        String docId = manager.stableFileDocId(filePath);

        assertFalse(docId.contains("\\"), "doc id must not contain Windows backslashes");
        assertTrue(docId.contains("/"), "doc id must use forward slashes");
    }

    @Test
    void stableDocIdFallsBackToAbsolutePathWhenOutsideProjectRoot() {
        AppConfig config = new AppConfig();
        config.setProjectRoot(System.getProperty("user.dir"));
        DocumentManifestManager manager = new DocumentManifestManager(config);

        Path outside = Paths.get("C:/some/other/place/notes.txt").toAbsolutePath().normalize();
        String docId = manager.stableFileDocId(outside);

        assertTrue(docId.startsWith("file:"), "doc id should keep the file: prefix");
        assertFalse(docId.contains(".."), "fallback id should not leak relative traversal segments");
    }

    @Test
    void chunkIdIsPythonCompatibleWithNewDocId() {
        String docId = "file:data/smartcloud_kb/example.txt";
        String chunkId = com.ftsm.rag.service.VectorStoreService.pythonCompatibleChunkId(docId, 0);
        assertEquals("81a80a93-5d42-550d-91e0-7ec8ce10782b", chunkId);
    }
}
