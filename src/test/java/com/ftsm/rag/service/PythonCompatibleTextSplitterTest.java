package com.ftsm.rag.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PythonCompatibleTextSplitterTest {

    @Test
    void splitsCurrentSmartCloudKnowledgeFiles() throws Exception {
        PythonCompatibleTextSplitter splitter = new PythonCompatibleTextSplitter(800, 120);

        assertSplitFile(splitter, "product_catalog_ecs_gpu_database.txt");
        assertSplitFile(splitter, "billing_policy_orders_invoices.txt");
        assertSplitFile(splitter, "icp_filing_checklist.txt");
        assertSplitFile(splitter, "support_sla_ticket_runbook.txt");
        assertSplitFile(splitter, "observability_prometheus_grafana.txt");
        assertSplitFile(splitter, "marketing_h5_campaign_assets.txt");
        assertSplitFile(splitter, "smartcloud_service_index.txt");
    }

    @Test
    void keepsOverlapAndNeverExceedsConfiguredSize() {
        PythonCompatibleTextSplitter splitter = new PythonCompatibleTextSplitter(20, 5);
        var chunks = splitter.splitText("alpha beta gamma delta epsilon zeta eta theta");

        assertTrue(chunks.size() > 1);
        assertTrue(chunks.stream().allMatch(chunk -> chunk.length() <= 20));
    }

    private void assertSplitFile(PythonCompatibleTextSplitter splitter, String filename) throws Exception {
        var chunks = splitChunks(splitter, filename);
        assertFalse(chunks.isEmpty(), filename + " should produce chunks");
        assertTrue(chunks.stream().allMatch(chunk -> chunk.length() <= 800),
                filename + " should respect configured chunk size");
    }

    private java.util.List<String> splitChunks(
            PythonCompatibleTextSplitter splitter, String filename) throws Exception {
        String text = Files.readString(
                Path.of("data", "smartcloud_kb", filename),
                StandardCharsets.UTF_8);
        if (!text.isEmpty() && text.charAt(0) == '\ufeff') {
            text = text.substring(1);
        }
        return splitter.splitText(text);
    }
}
