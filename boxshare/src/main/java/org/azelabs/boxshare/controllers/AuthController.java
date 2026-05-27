package org.azelabs.boxshare.controllers;

import lombok.RequiredArgsConstructor;
import org.azelabs.boxshare.dtos.AuthRequest;
import org.azelabs.boxshare.dtos.AuthResponse;
import org.azelabs.boxshare.dtos.RegisterRequest;
import org.azelabs.boxshare.services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Validated @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticate(@Validated @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }
}
