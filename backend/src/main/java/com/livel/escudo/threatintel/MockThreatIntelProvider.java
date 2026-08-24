package com.livel.escudo.threatintel;

import org.springframework.stereotype.Component;

@Component
public class MockThreatIntelProvider implements ThreatIntelProvider {
    @Override public Reputation lookup(String normalizedDomain) { return new Reputation(false, 0, "mock"); }
}

