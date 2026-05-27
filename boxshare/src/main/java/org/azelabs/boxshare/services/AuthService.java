package org.azelabs.boxshare.services;

import lombok.RequiredArgsConstructor;
import org.azelabs.boxshare.application.enums.RoleType;
import java.util.UUID;
import org.azelabs.boxshare.dtos.AuthRequest;
import org.azelabs.boxshare.dtos.AuthResponse;
import org.azelabs.boxshare.dtos.RegisterRequest;
import org.azelabs.boxshare.models.HybridUser;
import org.azelabs.boxshare.models.RoleModel;
import org.azelabs.boxshare.models.UserModel;
import org.azelabs.boxshare.repositories.IUserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        RoleModel role = new RoleModel();
        role.setType(RoleType.USER);

        UserModel user = new UserModel();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setIdentity(UUID.randomUUID());
        user.setRole(role);

        userRepository.save(user);

        String token = jwtService.generateToken(new HybridUser(user));
        return AuthResponse.builder().token(token).build();
    }

    public AuthResponse authenticate(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserModel user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtService.generateToken(new HybridUser(user));
        return AuthResponse.builder().token(token).build();
    }
}