package com.livel.escudo.risk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RiskEngineServiceTest {
    @Autowired RiskEngineService engine;

    @Test void detectsCredentialUrgencyAndPaymentSignals() {
        var analysis=engine.analyzeText("URGENTE: verificá tu cuenta, ingresá tu contraseña y transferí ahora.");
        assertThat(analysis.result().score()).isGreaterThanOrEqualTo(30);
        assertThat(analysis.result().indicators()).extracting(RiskModels.Indicator::type)
                .contains("URGENCY","CREDENTIAL_REQUEST","PAYMENT_REQUEST");
    }

    @Test void blocksPrivateUrlsWithoutFetchingThem() {
        org.assertj.core.api.Assertions.assertThatThrownBy(()->engine.analyzeUrl("http://127.0.0.1/admin"))
                .hasMessageContaining("red privada");
    }

    @Test void redactsPersonallyIdentifiableInformation() {
        String sanitized=engine.sanitize("Escribime a persona@example.com, código OTP 123456");
        assertThat(sanitized).doesNotContain("persona@example.com","123456").contains("[REDACTED_EMAIL]","[REDACTED_OTP]");
    }
}

