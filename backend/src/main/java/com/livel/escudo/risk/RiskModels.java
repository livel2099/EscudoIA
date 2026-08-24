package com.livel.escudo.risk;

import java.util.List;

public final class RiskModels {
    private RiskModels() {}
    public record Indicator(String type, String category, int score, String severity, String source,
                            double confidence, String explanation) {}
    public record Result(int score, double confidence, String classification, String level,
                         String engineVersion, List<Indicator> indicators, String summary, String recommendedAction) {}
}

