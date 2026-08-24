package com.livel.escudo.auth;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "token_hash", nullable = false, unique = true) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(name = "device_info") private String deviceInfo;

    protected RefreshTokenEntity() {}
    public RefreshTokenEntity(UUID userId, String tokenHash, Instant expiresAt, String deviceInfo) {
        this.id = UUID.randomUUID(); this.userId = userId; this.tokenHash = tokenHash;
        this.expiresAt = expiresAt; this.deviceInfo = deviceInfo == null ? "unknown" : deviceInfo.substring(0, Math.min(255, deviceInfo.length()));
    }
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public boolean active() { return revokedAt == null && expiresAt.isAfter(Instant.now()); }
    public void revoke() { this.revokedAt = Instant.now(); }
}

