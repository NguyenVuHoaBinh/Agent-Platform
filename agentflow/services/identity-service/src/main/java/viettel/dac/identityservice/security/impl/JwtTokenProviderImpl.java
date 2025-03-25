package viettel.dac.identityservice.security.impl;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;
import viettel.dac.identityservice.common.constants.SecurityConstants;
import viettel.dac.identityservice.config.JwtConfig;
import viettel.dac.identityservice.entity.RefreshToken;
import viettel.dac.identityservice.entity.User;
import viettel.dac.identityservice.repository.RefreshTokenRepository;
import viettel.dac.identityservice.security.JwtTokenProvider;
import viettel.dac.identityservice.security.TokenBlacklistService;
import viettel.dac.identityservice.security.TokenValidationResult;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProviderImpl implements JwtTokenProvider {
    private final JwtConfig jwtConfig;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;
    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        signingKey = jwtConfig.getSigningKey();
    }

    @Override
    public String generateAccessToken(User user, Set<String> scopes) {
        Date now = Date.from(clock.instant());
        Date expiryDate = new Date(now.getTime() + jwtConfig.getAccessTokenExpirationMs());

        // Collect user information
        String userId = user.getId();
        String email = user.getEmail();
        String username = user.getUsername();

        // Generate JWT with secure algorithm
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setId(UUID.randomUUID().toString())
                .claim(SecurityConstants.EMAIL_CLAIM, email)
                .claim(SecurityConstants.USERNAME_CLAIM, username)
                .claim(SecurityConstants.SCOPES_CLAIM, new ArrayList<>(scopes))
                .claim(SecurityConstants.TOKEN_TYPE_CLAIM, SecurityConstants.ACCESS_TOKEN_TYPE)
                .signWith(signingKey, SignatureAlgorithm.valueOf(SecurityConstants.TOKEN_SIGNING_ALGORITHM))
                .compact();
    }

    @Override
    public String generateRefreshToken(User user, String accessTokenId) {
        Date now = Date.from(clock.instant());
        Date expiryDate = new Date(now.getTime() + jwtConfig.getRefreshTokenExpirationMs());

        String tokenId = UUID.randomUUID().toString();

        // Store refresh token for validation
        RefreshToken refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID().toString())
                .tokenId(DigestUtils.sha256Hex(tokenId))
                .userId(user.getId())
                .issuedAt(now.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .expiresAt(expiryDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        // Generate refresh token with limited claims
        return Jwts.builder()
                .setSubject(user.getId())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setId(tokenId)
                .claim(SecurityConstants.TOKEN_TYPE_CLAIM, SecurityConstants.REFRESH_TOKEN_TYPE)
                .claim("access_token_id", accessTokenId)
                .signWith(signingKey, SignatureAlgorithm.valueOf(SecurityConstants.TOKEN_SIGNING_ALGORITHM))
                .compact();
    }

    @Override
    public String generateMfaToken(String userId) {
        Date now = Date.from(clock.instant());
        Date expiryDate = new Date(now.getTime() + jwtConfig.getMfaTokenExpirationMs());

        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setId(UUID.randomUUID().toString())
                .claim(SecurityConstants.TOKEN_TYPE_CLAIM, SecurityConstants.MFA_TOKEN_TYPE)
                .signWith(signingKey, SignatureAlgorithm.valueOf(SecurityConstants.TOKEN_SIGNING_ALGORITHM))
                .compact();
    }

    @Override
    public TokenValidationResult validateToken(String token) {
        try {
            // Check if token is blacklisted
            if (tokenBlacklistService.isBlacklisted(token)) {
                return TokenValidationResult.invalid("Token revoked");
            }

            // Parse and validate token
            Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .setClock(() -> Date.from(clock.instant()))
                    .build()
                    .parseClaimsJws(token);

            Claims claims = claimsJws.getBody();

            // Additional validation for token type
            String tokenType = claims.get(SecurityConstants.TOKEN_TYPE_CLAIM, String.class);
            if (tokenType == null) {
                return TokenValidationResult.invalid("Invalid token type");
            }

            return TokenValidationResult.valid(claims.getSubject(), claims);

        } catch (ExpiredJwtException e) {
            return TokenValidationResult.invalid("Token expired");
        } catch (SignatureException e) {
            return TokenValidationResult.invalid("Invalid token signature");
        } catch (MalformedJwtException e) {
            return TokenValidationResult.invalid("Malformed token");
        } catch (JwtException e) {
            return TokenValidationResult.invalid("Invalid token: " + e.getMessage());
        }
    }

    @Override
    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    @Override
    public String getTokenId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getId();
    }

    @Override
    public String getTokenType(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get(SecurityConstants.TOKEN_TYPE_CLAIM, String.class);
    }

    @Override
    public void revokeToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String tokenId = claims.getId();
            Date expiration = claims.getExpiration();

            tokenBlacklistService.blacklistToken(tokenId, expiration);

            // If it's a refresh token, also invalidate it in the database
            String tokenType = claims.get(SecurityConstants.TOKEN_TYPE_CLAIM, String.class);
            if (SecurityConstants.REFRESH_TOKEN_TYPE.equals(tokenType)) {
                refreshTokenRepository.findByTokenId(DigestUtils.sha256Hex(tokenId))
                        .ifPresent(refreshToken -> {
                            refreshToken.setRevoked(true);
                            refreshTokenRepository.save(refreshToken);
                        });
            }
        } catch (Exception e) {
            log.error("Error revoking token", e);
        }
    }

    @Override
    public long getAccessTokenExpiration() {
        return TimeUnit.MILLISECONDS.toSeconds(jwtConfig.getAccessTokenExpirationMs());
    }
}