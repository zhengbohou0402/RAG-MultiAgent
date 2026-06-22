package com.ftsm.rag.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SmartCloudProductCatalogService {

    public ProductProfile productFor(String query) {
        String normalized = query == null ? "" : query.toLowerCase(Locale.ROOT);
        if (normalized.contains("gpu") || normalized.contains("llm") || normalized.contains("ai")) {
            return new ProductProfile(
                    "SmartCloud GPU AI Computing",
                    "AI infrastructure",
                    List.of("NVIDIA-compatible GPU pool", "private VPC access", "model serving ready"),
                    "GPU starter bundle: reserve 2 nodes and get observability templates included.",
                    "Suitable for RAG, agent evaluation, fine-tuning experiments, and private inference."
            );
        }
        if (normalized.contains("database") || normalized.contains("mysql") || normalized.contains("mongo")) {
            return new ProductProfile(
                    "SmartCloud Managed Database",
                    "database",
                    List.of("automatic backup", "read replica", "slow-query insight"),
                    "Database migration package with one-click backup policy.",
                    "Suitable for billing, order, ticketing, and tenant data workloads."
            );
        }
        return new ProductProfile(
                "SmartCloud ECS Standard",
                "compute",
                List.of("elastic compute", "security group", "pay-as-you-go billing"),
                "Starter cloud server package for web apps and Java services.",
                "Suitable for Spring Boot, React console, observability agents, and API gateways."
        );
    }

    public Map<String, Object> asMap(ProductProfile profile) {
        return Map.of(
                "name", profile.name(),
                "category", profile.category(),
                "highlights", profile.highlights(),
                "promotion", profile.promotion(),
                "use_case", profile.useCase()
        );
    }

    public record ProductProfile(
            String name,
            String category,
            List<String> highlights,
            String promotion,
            String useCase
    ) {
    }
}
