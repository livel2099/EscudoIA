package com.livel.escudo.billing;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "plans")
public class PlanEntity {
    @Id private UUID id;
    @Column(nullable=false, unique=true) private String code;
    @Column(nullable=false) private String name;
    @Column(nullable=false) private boolean active;
    @Column(name="limits_json", nullable=false, columnDefinition="text") private String limitsJson;
    @Column(name="features_json", nullable=false, columnDefinition="text") private String featuresJson;
    protected PlanEntity() {}
    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
    public String getLimitsJson() { return limitsJson; }
    public String getFeaturesJson() { return featuresJson; }
}

