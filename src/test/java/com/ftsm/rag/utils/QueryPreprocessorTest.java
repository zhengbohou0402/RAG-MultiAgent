package com.ftsm.rag.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryPreprocessorTest {

    private final QueryPreprocessor preprocessor = new QueryPreprocessor();

    @Test
    void convertsTraditionalChineseToSimplified() {
        List<String> queries = preprocessor.process("雲服務器怎麼續費");
        assertTrue(queries.get(0).contains("云") || queries.get(0).contains("续费"),
                "Traditional-to-simplified conversion should keep recognizable SmartCloud terms, got: "
                        + queries.get(0));
        assertFalse(queries.get(0).contains("雲"),
                "Traditional form should have been converted: " + queries.get(0));
    }

    @Test
    void expandsBillingSynonyms() {
        List<String> queries = preprocessor.process("账单怎么开电子发票");
        assertTrue(queries.size() >= 2,
                "Expected at least one synonym expansion, got: " + queries);
        String joined = String.join(" | ", queries).toLowerCase();
        assertTrue(joined.contains("billing") || joined.contains("invoice"),
                "Expected billing/invoice synonym expansion, got: " + joined);
    }

    @Test
    void navigationPatternsAreStripped() {
        List<String> queries = preprocessor.process("Can you tell me where can I find the SmartCloud ECS guide?");
        String main = queries.get(0);
        assertTrue(main.toLowerCase().contains("smartcloud"),
                "Subject matter should remain: " + main);
        boolean strippedFound = queries.stream()
                .anyMatch(q -> !q.toLowerCase().contains("can you tell me")
                        && !q.toLowerCase().contains("where can i find"));
        assertTrue(strippedFound,
                "At least one rewrite should have navigation pattern removed: " + queries);
    }

    @Test
    void topicRewriteIsAddedForIcpDomain() {
        List<String> queries = preprocessor.process("域名备案需要什么材料?");
        String joined = String.join(" | ", queries).toLowerCase();
        assertTrue(joined.contains("icp") || joined.contains("company license"),
                "Topic rewrite should add ICP filing terminology, got: " + joined);
    }

    @Test
    void normalizationFoldsWhitespaceAndFullWidth() {
        List<String> queries = preprocessor.process("  你好  ？ 请问云服务器  在哪里续费？  ");
        String main = queries.get(0);
        assertEquals(main.trim(), main, "Main query should be trimmed: " + main);
        assertFalse(main.contains("  "), "No double spaces should remain: " + main);
    }

    @Test
    void casualMessageDoesNotGainExpansions() {
        List<String> queries = preprocessor.process("你好");
        assertEquals(1, queries.size(),
                "Casual greetings should yield only the normalized main query, got: " + queries);
    }
}
