package com.ftsm.rag.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class SmartCloudMetricsService {

    private final MeterRegistry meterRegistry;

    public SmartCloudMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void agentRoute(String route) {
        meterRegistry.counter("smartcloud_agent_route_total", "route", route == null ? "unknown" : route).increment();
    }

    public void cache(String result) {
        meterRegistry.counter("smartcloud_cache_total", "result", result).increment();
    }

    public void protocol(String protocol, String method) {
        meterRegistry.counter("smartcloud_protocol_calls_total",
                "protocol", protocol == null ? "unknown" : protocol,
                "method", method == null ? "unknown" : method).increment();
    }
}
