package com.livel.escudo.audit;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {
    @Id private UUID id;
    @Column(name = "actor_id") private UUID actorId;
    @Column(nullable = false) private String action;
    @Column(name = "entity_type") private String entityType;
    @Column(name = "entity_id") private UUID entityId;
    @Column(name = "ip_hash") private String ipHash;
    @Column(name = "user_agent_hash") private String userAgentHash;
    @Column(name = "request_id", nullable = false) private String requestId;
    @Column(nullable = false) private String result;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected AuditLogEntity() {}
    public AuditLogEntity(UUID actorId, String action, String entityType, UUID entityId, String ipHash,
                          String userAgentHash, String requestId, String result) {
        this.id = UUID.randomUUID(); this.actorId = actorId; this.action = action; this.entityType = entityType;
        this.entityId = entityId; this.ipHash = ipHash; this.userAgentHash = userAgentHash;
        this.requestId = requestId; this.result = result; this.createdAt = Instant.now();
    }
}

