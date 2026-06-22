package com.ftsm.rag.service;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VectorStoreServiceTest {

    @Test
    void fingerprintIsStableAndChangesWithPipelineConfiguration() {
        Map<String, Object> first = new TreeMap<>();
        first.put("embedding_model_name", "text-embedding-v3");
        first.put("chunk_size", 800);

        Map<String, Object> same = new TreeMap<>();
        same.put("chunk_size", 800);
        same.put("embedding_model_name", "text-embedding-v3");

        Map<String, Object> changed = new TreeMap<>(first);
        changed.put("chunk_size", 1000);

        assertEquals(
                VectorStoreService.buildIndexFingerprint(first),
                VectorStoreService.buildIndexFingerprint(same)
        );
        assertNotEquals(
                VectorStoreService.buildIndexFingerprint(first),
                VectorStoreService.buildIndexFingerprint(changed)
        );
    }

    @Test
    void tokenizerPreservesProductCodesAndChineseBigrams() {
        var terms = VectorStoreService.tokenize("ECS-2C4G 云服务器续费");

        assertTrue(terms.contains("ecs"));
        assertTrue(terms.contains("2c4g"));
        assertTrue(terms.contains("云服"));
        assertTrue(terms.contains("续费"));
    }

    @Test
    void chunkIdsMatchPythonUuid5() {
        assertEquals(
                "81a80a93-5d42-550d-91e0-7ec8ce10782b",
                VectorStoreService.pythonCompatibleChunkId(
                        "file:data/smartcloud_kb/example.txt", 0)
        );
    }
}
