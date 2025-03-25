package viettel.dac.identityservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import viettel.dac.identityservice.common.constants.SecurityConstants;
import viettel.dac.identityservice.config.JwtConfig;
import viettel.dac.identityservice.entity.RefreshToken;
import viettel.dac.identityservice.entity.User;
import viettel.dac.identityservice.repository.RefreshTokenRepository;
import viettel.dac.identityservice.security.impl.JwtTokenProviderImpl;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JwtTokenProviderTest {

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Spy
    private Clock fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());

    @InjectMocks
    private JwtTokenProviderImpl tokenProvider;

    private SecretKey testKey;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Generate a test key
        testKey = Jwts.SIG.HS512.key().build();

        // Configure the mock JwtConfig
        when(jwtConfig.getSigningKey()).thenReturn(testKey);
        when(jwtConfig.getAccessTokenExpirationMs()).thenReturn(900000L); // 15 minutes
        when(jwtConfig.getRefreshTokenExpirationMs()).thenReturn(604800000L); // 7 days
        when(jwtConfig.getMfaTokenExpirationMs()).thenReturn(300000L); // 5 minutes

        // Initialize the token provider
        tokenProvider.init();

        // Set up test user
        testUser = User.builder()
                .id(UUID.randomUUID().toString())
                .username("testuser")
                .email("test@example.com")
                .build();
    }

    @Test
    void testGenerateAccessToken() {
        // Set up test data
        Set<String> scopes = new HashSet<>(Arrays.asList("read", "write"));

        // Generate a token
        String token = tokenProvider.generateAccessToken(testUser, scopes);

        // Parse and validate the token
        Claims claims = Jwts.parser()
                .verifyWith(testKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // Verify claims
        assertEquals(testUser.getId(), claims.getSubject());
        assertEquals(testUser.getEmail(), claims.get(SecurityConstants.EMAIL_CLAIM));
        assertEquals(testUser.getUsername(), claims.get(SecurityConstants.USERNAME_CLAIM));
        assertEquals(SecurityConstants.ACCESS_TOKEN_TYPE, claims.get(SecurityConstants.TOKEN_TYPE_CLAIM));

        @SuppressWarnings("unchecked")
        List<String> tokenScopes = (List<String>) claims.get(SecurityConstants.SCOPES_CLAIM);
        assertTrue(tokenScopes.containsAll(scopes));

        // Verify expiration
        long expectedExpirationMs = fixedClock.millis() + jwtConfig.getAccessTokenExpirationMs();
        long actualExpirationMs = claims.getExpiration().getTime();
        // Allow for small timing differences
        assertTrue(Math.abs(expectedExpirationMs - actualExpirationMs) < 1000);
    }

    @Test
    void testValidateToken_ValidToken() {
        // Generate a valid token
        String token = Jwts.builder()
                .subject(testUser.getId())
                .issuedAt(Date.from(fixedClock.instant()))
                .expiration(Date.from(fixedClock.instant().plus(Duration.ofHours(1))))
                .id(UUID.randomUUID().toString())
                .claim(SecurityConstants.TOKEN_TYPE_CLAIM, SecurityConstants.ACCESS_TOKEN_TYPE)
                .signWith(testKey)
                .compact();

        // Test validation
        TokenValidationResult result = tokenProvider.validateToken(token);

        // Verify result
        assertTrue(result.isValid());
        assertEquals(testUser.getId(), result.getUserId());
        assertNotNull(result.getClaims());
        assertEquals(SecurityConstants.ACCESS_TOKEN_TYPE, result.getClaims().get(SecurityConstants.TOKEN_TYPE_CLAIM));
    }

    @Test
    void testValidateToken_ExpiredToken() {
        // Generate an expired token
        String token = Jwts.builder()
                .subject(testUser.getId())
                .issuedAt(Date.from(fixedClock.instant().minus(Duration.ofHours(2))))
                .expiration(Date.from(fixedClock.instant().minus(Duration.ofHours(1))))
                .id(UUID.randomUUID().toString())
                .claim(SecurityConstants.TOKEN_TYPE_CLAIM, SecurityConstants.ACCESS_TOKEN_TYPE)
                .signWith(testKey)
                .compact();

        // Test validation
        TokenValidationResult result = tokenProvider.validateToken(token);

        // Verify result
        assertFalse(result.isValid());
        assertNull(result.getUserId());
        assertTrue(result.getErrorMessage().contains("expired"));
    }

    @Test
    void testValidateToken_BlacklistedToken() {
        // Generate a valid token
        String token = Jwts.builder()
                .subject(testUser.getId())
                .issuedAt(Date.from(fixedClock.instant()))
                .expiration(Date.from(fixedClock.instant().plus(Duration.ofHours(1))))
                .id(UUID.randomUUID().toString())
                .claim(SecurityConstants.TOKEN_TYPE_CLAIM, SecurityConstants.ACCESS_TOKEN_TYPE)
                .signWith(testKey)
                .compact();

        // Set up the token as blacklisted
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(true);

        // Test validation
        TokenValidationResult result = tokenProvider.validateToken(token);

        // Verify result
        assertFalse(result.isValid());
        assertNull(result.getUserId());
        assertEquals("Token revoked", result.getErrorMessage());
    }

    @Test
    void testGenerateRefreshToken() {
        // Generate a refresh token
        String token = tokenProvider.generateRefreshToken(testUser, "access-token-id");

        // Verify that the refresh token is saved in the repository
        verify(refreshTokenRepository).save(any(RefreshToken.class));

        // Verify token claims
        Claims claims = Jwts.parser()
                .verifyWith(testKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals(testUser.getId(), claims.getSubject());
        assertEquals(SecurityConstants.REFRESH_TOKEN_TYPE, claims.get(SecurityConstants.TOKEN_TYPE_CLAIM));
        assertEquals("access-token-id", claims.get("access_token_id"));

        // Verify expiration
        long expectedExpirationMs = fixedClock.millis() + jwtConfig.getRefreshTokenExpirationMs();
        long actualExpirationMs = claims.getExpiration().getTime();
        // Allow for small timing differences
        assertTrue(Math.abs(expectedExpirationMs - actualExpirationMs) < 1000);
    }
}