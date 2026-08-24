package com.livel.escudo.billing;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="plan_prices")
public class PlanPriceEntity {
    @Id private UUID id;
    @Column(name="plan_id", nullable=false) private UUID planId;
    @Column(nullable=false) private String currency;
    @Column(nullable=false, precision=14, scale=2) private BigDecimal amount;
    @Column(name="active_from", nullable=false) private Instant activeFrom;
    @Column(name="active_to") private Instant activeTo;
    protected PlanPriceEntity() {}
    public UUID getPlanId() { return planId; }
    public String getCurrency() { return currency; }
    public BigDecimal getAmount() { return amount; }
}

