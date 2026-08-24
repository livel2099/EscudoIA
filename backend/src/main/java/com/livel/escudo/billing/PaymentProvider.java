package com.livel.escudo.billing;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentProvider {
    CheckoutResult createOneTimeCheckout(UUID internalPaymentId, String title, BigDecimal amount, String currency);
    CheckoutResult createSubscription(UUID internalPaymentId, String title, BigDecimal amount, String currency);
    PaymentStatus queryStatus(String providerPaymentId);
    record CheckoutResult(String providerId, String checkoutUrl, String publicKey, boolean sandbox) {}
    record PaymentStatus(String providerId, String externalReference, String status) {}
}

