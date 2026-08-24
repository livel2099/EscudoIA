package com.livel.escudo.scan;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "risk_indicators")
public class RiskIndicatorEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "scan_id", nullable = false) private ScanEntity scan;
    @Column(nullable = false) private String type;
    @Column(nullable = false) private String category;
    @Column(nullable = false) private int score;
    @Column(nullable = false) private String severity;
    @Column(nullable = false) private String source;
    @Column(nullable = false) private double confidence;
    @Column(nullable = false, length = 1000) private String explanation;

    protected RiskIndicatorEntity() {}
    public RiskIndicatorEntity(String type, String category, int score, String severity, String source, double confidence, String explanation) {
        this.id = UUID.randomUUID(); this.type = type; this.category = category; this.score = score;
        this.severity = severity; this.source = source; this.confidence = confidence; this.explanation = explanation;
    }
    void attach(ScanEntity scan) { this.scan = scan; }
    public String getType() { return type; }
    public String getCategory() { return category; }
    public int getScore() { return score; }
    public String getSeverity() { return severity; }
    public String getSource() { return source; }
    public double getConfidence() { return confidence; }
    public String getExplanation() { return explanation; }
}

