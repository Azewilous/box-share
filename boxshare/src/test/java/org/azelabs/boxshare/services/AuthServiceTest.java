package org.azelabs.boxshare.services;

import org.azelabs.boxshare.application.enums.RoleType;
import org.azelabs.boxshare.dtos.AuthRequest;
import org.azelabs.boxshare.dtos.AuthResponse;
import org.azelabs.boxshare.dtos.RegisterRequest;
import org.azelabs.boxshare.models.UserModel;
import org.azelabs.boxshare.repositories.IUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock IUserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;
    @Mock EmailService emailService;

    @InjectMocks AuthService authService;

    @Test
    void register_savesUserWithEncodedPassword_andReturnsToken() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .username("johndoe")
                .password("plaintext")
                .build();

        UserModel savedUser = new UserModel();
        savedUser.setIdentity(UUID.randomUUID());
        savedUser.setPassword("encoded");

        when(passwordEncoder.encode("plaintext")).thenReturn("encoded");
        when(userRepository.save(any(UserModel.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any())).thenReturn("mock-jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("mock-jwt-token");

        ArgumentCaptor<UserModel> captor = ArgumentCaptor.forClass(UserModel.class);
        verify(userRepository).save(captor.capture());
        UserModel persisted = captor.getValue();
        assertThat(persisted.getEmail()).isEqualTo("john@example.com");
        assertThat(persisted.getPassword()).isEqualTo("encoded");
        assertThat(persisted.getRole()).isNotNull();
        assertThat(persisted.getRole().getType()).isEqualTo(RoleType.USER);
    }

    @Test
    void authenticate_withValidCredentials_returnsToken() {
        AuthRequest request = AuthRequest.builder()
                .email("john@example.com")
                .password("plaintext")
                .build();

        UserModel user = new UserModel();
        user.setIdentity(UUID.randomUUID());
        user.setPassword("encoded");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any())).thenReturn("mock-jwt-token");

        AuthResponse response = authService.authenticate(request);

        assertThat(response.getToken()).isEqualTo("mock-jwt-token");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void authenticate_withBadCredentials_throwsException() {
        AuthRequest request = AuthRequest.builder()
                .email("john@example.com")
                .password("wrongpassword")
                .build();

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.authenticate(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByEmail(any());
    }
}