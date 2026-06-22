package com.ftsm.rag.controller;

import com.ftsm.rag.model.MarketingAsset;
import com.ftsm.rag.model.MarketingRequest;
import com.ftsm.rag.service.MarketingAssetService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api")
public class MarketingController {

    private final MarketingAssetService marketingAssetService;

    public MarketingController(MarketingAssetService marketingAssetService) {
        this.marketingAssetService = marketingAssetService;
    }

    @PostMapping("/marketing/generate")
    public MarketingAsset generate(@RequestBody MarketingRequest request) {
        return marketingAssetService.generate(request);
    }

    @GetMapping("/assets/marketing/{assetId}/{filename:.+}")
    public ResponseEntity<Resource> asset(@PathVariable String assetId, @PathVariable String filename) {
        Resource resource = marketingAssetService.load(assetId, filename);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = filename.endsWith(".png")
                ? MediaType.IMAGE_PNG
                : MediaType.TEXT_HTML;
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)))
                .contentType(mediaType)
                .body(resource);
    }
}
