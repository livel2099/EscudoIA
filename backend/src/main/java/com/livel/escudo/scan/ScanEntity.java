package com.livel.escudo.scan;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "scans")
public class ScanEntity {
    @Id private UUID id;
    @Column(name = "user_id") private UUID userId;
    @Column(nullable = false) private String type;
    @Column(nullable = false) private String status;
    @Column(name = "risk_score", nullable = false) private int riskScore;
    @Column(name = "risk_level", nullable = false) private String riskLevel;
    @Column(nullable = false) private String classification;
    @Column(nullable = false) private double confidence;
    @Column(name = "sanitized_text", length = 20000) private String sanitizedText;
    @Column(nullable = false, length = 2000) private String summary;
    @Column(nullable = false) private String recommendation;
    @Column(name = "engine_version", nullable = false) private String engineVersion;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @OneToMany(mappedBy = "scan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("score DESC")
    private List<RiskIndicatorEntity> indicators = new ArrayList<>();

    protected ScanEntity() {}
    public ScanEntity(UUID userId, String type, String sanitizedText, int riskScore, String riskLevel,
                      String classification, double confidence, String summary, String recommendation, String engineVersion) {
        this.id = UUID.randomUUID(); this.userId = userId; this.type = type; this.status = "COMPLETED";
        this.sanitizedText = sanitizedText; this.riskScore = riskScore; this.riskLevel = riskLevel;
        this.classification = classification; this.confidence = confidence; this.summary = summary;
        this.recommendation = recommendation; this.engineVersion = engineVersion; this.createdAt = Instant.now();
    }
    public void addIndicator(RiskIndicatorEntity indicator) { indicator.attach(this); indicators.add(indicator); }
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public int getRiskScore() { return riskScore; }
    public String getRiskLevel() { return riskLevel; }
    public String getClassification() { return classification; }
    public double getConfidence() { return confidence; }
    public String getSanitizedText() { return sanitizedText; }
    public String getSummary() { return summary; }
    public String getRecommendation() { return recommendation; }
    public String getEngineVersion() { return engineVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public List<RiskIndicatorEntity> getIndicators() { return List.copyOf(indicators); }
}

