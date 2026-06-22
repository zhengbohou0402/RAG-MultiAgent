package com.ftsm.rag.controller;

import com.ftsm.rag.model.TraceRecord;
import com.ftsm.rag.service.SmartCloudTraceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/traces")
public class TraceController {

    private final SmartCloudTraceService traceService;

    public TraceController(SmartCloudTraceService traceService) {
        this.traceService = traceService;
    }

    @GetMapping
    public List<TraceRecord> list() {
        return traceService.list();
    }

    @GetMapping("/{conversationId}")
    public List<TraceRecord> byConversation(@PathVariable String conversationId) {
        return traceService.byConversation(conversationId);
    }
}
