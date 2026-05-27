package org.azelabs.boxshare.controllers;

import lombok.RequiredArgsConstructor;
import org.azelabs.boxshare.dtos.AuthRequest;
import org.azelabs.boxshare.dtos.AuthResponse;
import org.azelabs.boxshare.dtos.RegisterRequest;
import org.azelabs.boxshare.services.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.azelabs.boxshare.models.HybridUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    public static final String COOKIE_NAME = "access_token";

    private final AuthService authService;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Validated @RequestBody RegisterRequest request) {
        AuthResponse body = authService.register(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, tokenCookie(body.getToken()).toString())
                .body(body);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticate(@Validated @RequestBody AuthRequest request) {
        AuthResponse body = authService.authenticate(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, tokenCookie(body.getToken()).toString())
                .body(body);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie().toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(@AuthenticationPrincipal HybridUser principal) {
        AuthResponse body = authService.me(principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, tokenCookie(body.getToken()).toString())
                .body(body);
    }

    private ResponseCookie tokenCookie(String token) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .path("/")
                .maxAge(expirationMs / 1000)
                .sameSite("Lax")
                .build();
    }

    private ResponseCookie clearCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }
}
