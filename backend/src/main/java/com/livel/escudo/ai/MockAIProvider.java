package com.livel.escudo.ai;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class MockAIProvider implements AIProvider {
    @Override public AIAnalysis analyzeSanitized(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        boolean urgency = lower.matches("(?s).*(urgente|ahora|inmediatamente|último aviso|ultimo aviso|suspendid[ao]).*");
        boolean credentials = lower.matches("(?s).*(contraseña|contrasena|clave|usuario|iniciar sesión|iniciar sesion).*");
        boolean payment = lower.matches("(?s).*(pagar|transfer|cv[ub]|dinero|premio|deuda).*");
        boolean financialLure = lower.matches("(?s).*(fondos? (?:internacionales )?recibidos?|total depositado|naturaleza del cr[eé]dito|cr[eé]dito (?:ha sido )?confirmado|dep[oó]sito (?:recibido|confirmado)).*");
        boolean linkAction = lower.matches("(?s).*https?://.*") && lower.matches("(?s).*(acceder|acceda|ingres[ae]|haga clic|bot[oó]n|ver detalles|revisar los detalles).*");
        boolean securityAssurance = lower.matches("(?s).*(protocolos seguros|fondos (?:est[aá]n )?disponibles|cr[eé]dito (?:ha sido )?confirmado).*");
        boolean legalNotice = lower.matches("(?s).*(notificaci[oó]n jur[ií]dica|comunicaci[oó]n procesal|asunto jur[ií]dico|documento procesal).*");
        boolean legalCaseDetails = lower.matches("(?s).*(informaci[oó]n procesal|n[uú]mero de proceso|tribunal|caso\\s*:?\\s*\\d+).*");
        boolean legalLure = legalNotice && legalCaseDetails;
        boolean documentAccessCta = lower.matches("(?s).*(acceder (?:al |a la )?(?:documento|comunicaci[oó]n|expediente)|documento (?:procesal )?preparado para su revisi[oó]n|requiere su consulta).*");
        double signal = Math.min(1, (urgency ? .25 : 0) + (credentials ? .45 : 0) + (payment ? .3 : 0)
                + (financialLure ? .35 : 0) + (linkAction ? .25 : 0) + (securityAssurance ? .1 : 0)
                + (legalLure ? .35 : 0) + (documentAccessCta ? .25 : 0));
        boolean socialEngineering = urgency && (credentials || payment) || financialLure && linkAction ||
                securityAssurance && linkAction || legalLure && documentAccessCta;
        return new AIAnalysis(socialEngineering, urgency, credentials, payment, signal,
                "Clasificador local de desarrollo; no se enviaron datos a un proveedor externo.");
    }
}
