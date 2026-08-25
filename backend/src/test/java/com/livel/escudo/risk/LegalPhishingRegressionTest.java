package com.livel.escudo.risk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LegalPhishingRegressionTest {
    @Autowired RiskEngineService engine;

    @Test void classifiesFakeLegalNoticeWithSuspiciousHostingLinkAsPhishing() {
        String message = """
                Notificación jurídica
                Se encuentra disponible una comunicación procesal
                Estimado/a persona@example.com:
                Existe documentación relacionada con un asunto jurídico que requiere su consulta.
                Información procesal
                Número de proceso: 18368
                Tribunal: PJN
                Asunto: Caso 93134
                Documento procesal preparado para su revisión
                Acceder al documento: https://194.185.153.160.host.secureserver.net/?7UDXUS=ARS6523988128
                """;

        var analysis = engine.analyzeText(message);

        assertThat(analysis.sanitizedInput()).doesNotContain("persona@example.com");
        assertThat(analysis.result().score()).isGreaterThanOrEqualTo(75);
        assertThat(analysis.result().level()).isIn("HIGH", "CRITICAL");
        assertThat(analysis.result().classification()).isEqualTo("PHISHING");
        assertThat(analysis.result().recommendedAction()).isEqualTo("DO_NOT_PROCEED");
        assertThat(analysis.result().indicators()).extracting(RiskModels.Indicator::type)
                .contains("LEGAL_PROCESS_LURE", "DOCUMENT_ACCESS_CTA", "EMBEDDED_IP_HOSTNAME", "OPAQUE_QUERY");
    }

    @Test void marksLegalDocumentTrapWithoutVisibleDestinationAsHighRiskButNotConfirmedPhishing() {
        String ocrText = """
                Oficina Jurídica
                Notificación jurídica
                Se encuentra disponible una comunicación procesal
                Información procesal. Número de proceso: 18368. Tribunal: PJN. Caso: 93134.
                Documento procesal preparado para su revisión
                Acceder al documento
                """;

        var analysis = engine.analyzeText(ocrText);

        assertThat(analysis.result().score()).isGreaterThanOrEqualTo(61);
        assertThat(analysis.result().level()).isIn("HIGH", "CRITICAL");
        assertThat(analysis.result().classification()).isEqualTo("SUSPICIOUS");
        assertThat(analysis.result().recommendedAction()).isEqualTo("DO_NOT_PROCEED");
    }

    @Test void doesNotEscalateNeutralLegalInformationWithoutDocumentCta() {
        var analysis = engine.analyzeText("Información general del Poder Judicial sobre horarios y canales oficiales de atención.");

        assertThat(analysis.result().score()).isLessThanOrEqualTo(40);
        assertThat(analysis.result().classification()).isNotEqualTo("PHISHING");
    }
}
