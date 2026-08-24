package com.livel.escudo.risk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DepositPhishingRegressionTest {
    @Autowired RiskEngineService engine;

    @Test void classifiesUnexpectedInternationalDepositWithShortenedCtaAsHighRiskPhishing() {
        String message = """
                GESTOR INTERNACIONAL DE DEPÓSITOS
                Notificación de Fondos
                FONDOS RECIBIDOS
                Código de Transacción: DEP-387383986
                Estimado(a): persona@example.com
                Naturaleza del Crédito: Fondos Internacionales Recibidos
                Ubicación: ESTADOS UNIDOS
                Total Depositado: USD 3.028,32
                Los fondos están disponibles en su cuenta. Disponibles para operaciones.
                Para revisar los detalles, acceda mediante el botón de abajo:
                [Acceder a Cuenta](https://goo.su/80qga6hv1?persona@example.com=google=2369594877=bancario-deposito-2&exp=X838KGNKPAZ)
                El crédito ha sido confirmado mediante protocolos seguros.
                """;

        var analysis = engine.analyzeText(message);

        assertThat(analysis.sanitizedInput()).doesNotContain("persona@example.com").contains("[REDACTED_EMAIL]");
        assertThat(analysis.result().score()).isGreaterThanOrEqualTo(70);
        assertThat(analysis.result().level()).isIn("HIGH", "CRITICAL");
        assertThat(analysis.result().classification()).isEqualTo("PHISHING");
        assertThat(analysis.result().recommendedAction()).isEqualTo("DO_NOT_PROCEED");
        assertThat(analysis.result().indicators()).extracting(RiskModels.Indicator::type)
                .contains("FINANCIAL_LURE", "ACCOUNT_ACCESS_CTA", "SHORTENED_URL");
    }

    @Test void keepsNeutralDepositNoticeWithoutLinkAtLowOrCaution() {
        var analysis = engine.analyzeText("Se acreditó tu sueldo. Consultá el movimiento desde la app oficial.");

        assertThat(analysis.result().score()).isLessThanOrEqualTo(40);
        assertThat(analysis.result().level()).isIn("LOW", "CAUTION");
        assertThat(analysis.result().classification()).isNotEqualTo("PHISHING");
    }
}
