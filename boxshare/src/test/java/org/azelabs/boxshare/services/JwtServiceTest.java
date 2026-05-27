package org.azelabs.boxshare.services;

import org.azelabs.boxshare.models.HybridUser;
import org.azelabs.boxshare.models.UserModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String TEST_SECRET = "test-secret-key-that-is-long-enough-for-hmac-sha-signing-algorithm-needs-64-chars";
    private static final long EXPIRATION_MS = 86400000L;

    private JwtService jwtService;
    private UserDetails userDetails;
    private UUID identity;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", EXPIRATION_MS);

        identity = UUID.randomUUID();
        UserModel user = new UserModel();
        user.setIdentity(identity);
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        user.setPassword("hashed");
        userDetails = new HybridUser(user);
    }

    @Test
    void generateToken_returnsNonNullToken() {
        String token = jwtService.generateToken(userDetails);
        assertThat(token).isNotBlank();
    }

    @Test
    void extractIdentity_returnsUserIdentityAsSubject() {
        String token = jwtService.generateToken(userDetails);
        String extracted = jwtService.extractIdentity(token);
        assertThat(extracted).isEqualTo(identity.toString());
    }

    @Test
    void isTokenValid_withMatchingUser_returnsTrue() {
        String token = jwtService.generateToken(userDetails);
        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void isTokenValid_withDifferentUser_returnsFalse() {
        String token = jwtService.generateToken(userDetails);

        UserModel otherUser = new UserModel();
        otherUser.setIdentity(UUID.randomUUID());
        otherUser.setPassword("hashed");
        UserDetails otherDetails = new HybridUser(otherUser);

        assertThat(jwtService.isTokenValid(token, otherDetails)).isFalse();
    }

    @Test
    void isTokenValid_withExpiredToken_returnsFalse() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L);
        String expiredToken = jwtService.generateToken(userDetails);

        assertThat(jwtService.isTokenValid(expiredToken, userDetails)).isFalse();
    }
}