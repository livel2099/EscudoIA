package com.livel.escudo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Cors cors,
        MercadoPago mercadoPago,
        RateLimit rateLimit
) {
    public record Jwt(String secret, long accessTtlSeconds, long refreshTtlSeconds) {}
    public record Cors(String allowedOrigins) {}
    public record MercadoPago(boolean productionMode, boolean mockMode, String checkoutMode,
                              String accessToken, String publicKey, String webhookUrl,
                              String webhookSecret, String apiBaseUrl) {}
    public record RateLimit(int requestsPerMinute) {}
}

