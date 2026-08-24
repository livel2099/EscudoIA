package com.livel.escudo.billing;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="payments")
public class PaymentEntity {
    @Id private UUID id;
    @Column(name="user_id") private UUID userId;
    @Column(nullable=false) private String type;
    @Column(nullable=false, precision=14, scale=2) private BigDecimal amount;
    @Column(nullable=false) private String currency;
    @Column(name="provider_id") private String providerId;
    @Column(nullable=false) private String status;
    @Column(name="provider_event_id", unique=true) private String providerEventId;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
    protected PaymentEntity() {}
    public PaymentEntity(UUID id, UUID userId, String type, BigDecimal amount, String currency) {
        this.id=id; this.userId=userId; this.type=type; this.amount=amount; this.currency=currency;
        this.status="PENDING"; this.createdAt=Instant.now(); this.updatedAt=this.createdAt;
    }
    public UUID getId() { return id; }
    public String getStatus() { return status; }
    public void checkoutCreated(String providerId) { this.providerId=providerId; this.updatedAt=Instant.now(); }
    public void applyWebhook(String providerId, String providerStatus, String eventId) {
        this.providerId=providerId; this.providerEventId=eventId; this.status=mapStatus(providerStatus); this.updatedAt=Instant.now();
    }
    private String mapStatus(String status) { return switch(status == null ? "" : status.toLowerCase()) {
        case "approved", "authorized" -> "CONFIRMED"; case "rejected", "cancelled", "refunded", "charged_back" -> "FAILED"; default -> "PENDING"; };
    }
}

