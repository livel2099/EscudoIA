package com.livel.escudo.auth;

import com.livel.escudo.config.AppProperties;
import com.livel.escudo.user.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class TokenService {
    private final SecretKey key;
    private final AppProperties.Jwt settings;

    public TokenService(AppProperties properties) {
        this.settings = properties.jwt();
        if (settings.secret() == null || settings.secret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_ACCESS_SECRET debe contener al menos 32 bytes.");
        }
        this.key = Keys.hmacShaKeyFor(settings.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String accessToken(UserEntity user) {
        Instant now = Instant.now();
        return Jwts.builder().subject(user.getId().toString()).claim("email", user.getEmail())
                .claim("roles", user.getRoles()).issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(settings.accessTtlSeconds())))
                .signWith(key).compact();
    }

    public String newRefreshToken() { return UUID.randomUUID() + "." + UUID.randomUUID(); }
    public Instant refreshExpiry() { return Instant.now().plusSeconds(settings.refreshTtlSeconds()); }
    public Claims parse(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }

    public String hash(String raw) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}

