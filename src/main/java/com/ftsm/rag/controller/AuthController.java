package com.ftsm.rag.controller;

import com.ftsm.rag.model.LoginRequest;
import com.ftsm.rag.model.LoginResponse;
import com.ftsm.rag.model.SmartCloudUserContext;
import com.ftsm.rag.service.SmartCloudAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SmartCloudAuthService authService;

    public AuthController(SmartCloudAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request.getUsername(), request.getPassword(), request.getTenantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid demo credentials"));
    }

    @GetMapping("/me")
    public SmartCloudUserContext me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return authService.resolve(authorization);
    }

    @GetMapping("/demo")
    public ResponseEntity<Map<String, Object>> demo() {
        return ResponseEntity.ok(authService.publicDemoCredentials());
    }
}
