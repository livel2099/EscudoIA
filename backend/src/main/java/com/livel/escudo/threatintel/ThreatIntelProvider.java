package com.livel.escudo.threatintel;

public interface ThreatIntelProvider {
    Reputation lookup(String normalizedDomain);
    record Reputation(boolean knownThreat, double confidence, String provider) {}
}

