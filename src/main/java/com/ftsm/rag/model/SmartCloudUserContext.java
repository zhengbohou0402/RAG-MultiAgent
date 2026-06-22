package com.ftsm.rag.model;

public record SmartCloudUserContext(
        String tenantId,
        String userId,
        String username,
        String displayName,
        String role
) {
    public static SmartCloudUserContext demo() {
        return new SmartCloudUserContext(
                "tenant-demo",
                "demo-admin",
                "demo-admin",
                "Demo Admin",
                "TENANT_ADMIN"
        );
    }
}
