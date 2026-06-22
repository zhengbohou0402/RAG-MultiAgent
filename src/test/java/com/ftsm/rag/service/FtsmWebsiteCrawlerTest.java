package com.ftsm.rag.service;

import com.ftsm.rag.config.AppConfig;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FtsmWebsiteCrawlerTest {

    private FtsmWebsiteCrawler crawler;

    @BeforeEach
    void setUp() {
        AppConfig config = new AppConfig();
        crawler = new FtsmWebsiteCrawler(config);
    }

    @Test
    void crawlerOnlyAcceptsConfiguredHttpsPrefixes() {
        assertTrue(crawler.isAllowedUrl("https://docs.smartcloud.local/products/ecs"));
        assertTrue(crawler.isAllowedUrl("https://support.smartcloud.local/billing/invoices"));
        assertTrue(crawler.isAllowedUrl("https://status.smartcloud.local/incidents"));
        assertFalse(crawler.isAllowedUrl("http://docs.smartcloud.local/products/ecs"));
        assertFalse(crawler.isAllowedUrl("https://smartcloud.local.attacker.example/page"));
        assertFalse(crawler.isAllowedUrl("https://docs.smartcloud.example/products/ecs"));
        assertFalse(crawler.isAllowedUrl("file:///etc/passwd"));
    }

    @Test
    void normalizesFragmentsAndRejectsUnexpectedPorts() {
        assertEquals(
                "https://docs.smartcloud.local/products/ecs?a=1",
                FtsmWebsiteCrawler.normalizeUrl(
                        "https://DOCS.SMARTCLOUD.LOCAL:443//products/ecs/?a=1#top"
                )
        );
        assertEquals("", FtsmWebsiteCrawler.normalizeUrl("https://docs.smartcloud.local:8443/products/ecs"));
    }

    @Test
    void filtersDownloadsAndDiscoversUniqueRelativeLinks() {
        String html = """
                <html><body>
                  <a href="/products/ecs#top">ECS</a>
                  <a href="/products/ecs">Duplicate</a>
                  <a href="/products/report.pdf">PDF</a>
                  <a href="https://example.com/outside">Outside</a>
                  <a href="https://support.smartcloud.local/billing/invoices">Invoices</a>
                </body></html>
                """;

        List<String> links = crawler.extractLinks(
                Jsoup.parse(html, "https://docs.smartcloud.local/products")
        );

        assertEquals(List.of(
                "https://docs.smartcloud.local/products/ecs",
                "https://support.smartcloud.local/billing/invoices"
        ), links);
        assertTrue(crawler.shouldSkipUrl("https://docs.smartcloud.local/products/report.PDF?download=1"));
        assertTrue(crawler.shouldSkipUrl("https://docs.smartcloud.local/wp-login.php"));
        assertFalse(crawler.shouldSkipUrl("https://docs.smartcloud.local/products/gpu-ai-computing"));
    }

    @Test
    void cleansBrowserTextWithoutFlatteningSections() {
        assertEquals(
                "Product Overview\nCompute and billing",
                FtsmWebsiteCrawler.cleanText(
                        "  Product   Overview \r\n\r\n x \n Compute\tand billing  "
                )
        );
    }
}
