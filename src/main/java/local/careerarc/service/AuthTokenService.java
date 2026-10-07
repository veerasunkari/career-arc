package local.careerarc.service;

import local.careerarc.entity.Account;
import local.careerarc.entity.RefreshToken;
import local.careerarc.repository.RefreshTokenRepository;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthTokenService {
    private static final Duration ACCESS_LIFETIME = Duration.ofMinutes(15);
    private static final Duration REFRESH_LIFETIME = Duration.ofDays(7);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtEncoder jwtEncoder;
    private final RefreshTokenRepository refreshTokens;

    public AuthTokenService(JwtEncoder jwtEncoder, RefreshTokenRepository refreshTokens) {
        this.jwtEncoder = jwtEncoder;
        this.refreshTokens = refreshTokens;
    }

    @Transactional
    public TokenPair create(Account account) {
        return create(account, UUID.randomUUID().toString());
    }

    @Transactional
    public Optional<TokenPair> rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return Optional.empty();
        Optional<RefreshToken> found = refreshTokens.findByTokenHashForUpdate(hash(rawToken));
        if (found.isEmpty()) return Optional.empty();

        RefreshToken current = found.get();
        Instant now = Instant.now();
        if (current.usedAt != null || current.revokedAt != null || !current.expiresAt.isAfter(now)
                || !current.account.enabled) {
            revokeFamily(current.familyId, now);
            return Optional.empty();
        }

        current.usedAt = now;
        refreshTokens.save(current);
        return Optional.of(create(current.account, current.familyId, now));
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        refreshTokens.findByTokenHashForUpdate(hash(rawToken))
            .ifPresent(token -> revokeFamily(token.familyId, Instant.now()));
    }

    private TokenPair create(Account account, String familyId) {
        return create(account, familyId, Instant.now());
    }

    private TokenPair create(Account account, String familyId, Instant now) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokens.save(new RefreshToken(hash(rawToken), familyId, account, now.plus(REFRESH_LIFETIME)));
        Instant accessExpiry = now.plus(ACCESS_LIFETIME);
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("career-arc")
            .subject(account.email)
            .issuedAt(now)
            .expiresAt(accessExpiry)
            .claim("roles", List.of(account.role))
            .build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(
            org.springframework.security.oauth2.jwt.JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new TokenPair(account.email, account.name, account.role, accessToken, rawToken, REFRESH_LIFETIME);
    }

    private void revokeFamily(String familyId, Instant now) {
        List<RefreshToken> family = refreshTokens.findByFamilyId(familyId);
        for (RefreshToken token : family) {
            if (token.revokedAt == null) token.revokedAt = now;
        }
        refreshTokens.saveAll(family);
    }

    private static String hash(String rawToken) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    public record TokenPair(String email, String name, String role, String accessToken,
                            String refreshToken, Duration refreshLifetime) {}
}
