package com.livel.escudo.ai;

public interface AIProvider {
    AIAnalysis analyzeSanitized(String sanitizedText);
    record AIAnalysis(boolean socialEngineering, boolean urgency, boolean credentialRequest,
                      boolean paymentRequest, double riskSignal, String explanation) {}
}

