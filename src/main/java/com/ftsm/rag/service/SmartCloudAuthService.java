package com.ftsm.rag.service;

import com.ftsm.rag.config.AppConfig;
import com.ftsm.rag.model.LoginResponse;
import com.ftsm.rag.model.SmartCloudUserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class SmartCloudAuthService {

    private final AppConfig appConfig;
    private final SmartCloudBusinessDataService businessDataService;

    public SmartCloudAuthService(AppConfig appConfig, SmartCloudBusinessDataService businessDataService) {
        this.appConfig = appConfig;
        this.businessDataService = businessDataService;
    }

    public Optional<LoginResponse> login(String username, String password, String tenantId) {
        return businessDataService.authenticate(username, password, tenantId)
                .map(user -> {
                    long expiresAt = Instant.now().plusSeconds(appConfig.getSmartcloud().getTokenTtlMinutes() * 60L)
                            .getEpochSecond();
                    return new LoginResponse(createToken(user, expiresAt), user, expiresAt);
                });
    }

    public SmartCloudUserContext resolve(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return SmartCloudUserContext.demo();
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        return verifyToken(token).orElseGet(SmartCloudUserContext::demo);
    }

    private String createToken(SmartCloudUserContext user, long expiresAt) {
        String payload = encode(String.join("|",
                user.tenantId(),
                user.userId(),
                user.username(),
                user.displayName(),
                user.role(),
                Long.toString(expiresAt)));
        return payload + "." + sign(payload);
    }

    private Optional<SmartCloudUserContext> verifyToken(String token) {
        try {
            String[] parts = token.split("\\.", 2);
            if (parts.length != 2 || !sign(parts[0]).equals(parts[1])) {
                return Optional.empty();
            }
            String[] fields = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8)
                    .split("\\|", -1);
            if (fields.length != 6 || Long.parseLong(fields[5]) < Instant.now().getEpochSecond()) {
                return Optional.empty();
            }
            return Optional.of(new SmartCloudUserContext(fields[0], fields[1], fields[2], fields[3], fields[4]));
        } catch (Exception error) {
            log.debug("JWT demo token verification failed: {}", error.getMessage());
            return Optional.empty();
        }
    }

    private String encode(String raw) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appConfig.getSmartcloud().getJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) {
            throw new IllegalStateException("Could not sign SmartCloud demo token", error);
        }
    }

    public Map<String, Object> publicDemoCredentials() {
        return Map.of(
                "username", "demo-admin",
                "password", "demo123456",
                "tenant_id", appConfig.getSmartcloud().getDefaultTenantId()
        );
    }
}
