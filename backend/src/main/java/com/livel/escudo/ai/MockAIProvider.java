package com.livel.escudo.ai;

import org.springframework.stereotype.Component;

@Component
public class MockAIProvider implements AIProvider {
    @Override public AIAnalysis analyzeSanitized(String text) {
        String lower = text.toLowerCase();
        boolean urgency = lower.matches("(?s).*(urgente|ahora|inmediatamente|último aviso|ultimo aviso|suspendid[ao]).*");
        boolean credentials = lower.matches("(?s).*(contraseña|contrasena|clave|usuario|iniciar sesión|iniciar sesion).*");
        boolean payment = lower.matches("(?s).*(pagar|transfer|cv[ub]|dinero|premio|deuda).*");
        boolean financialLure = lower.matches("(?s).*(fondos? (?:internacionales )?recibidos?|total depositado|naturaleza del cr[eé]dito|cr[eé]dito (?:ha sido )?confirmado|dep[oó]sito (?:recibido|confirmado)).*");
        boolean linkAction = lower.matches("(?s).*https?://.*") && lower.matches("(?s).*(acceder|acceda|ingres[ae]|haga clic|bot[oó]n|ver detalles|revisar los detalles).*");
        boolean securityAssurance = lower.matches("(?s).*(protocolos seguros|fondos (?:est[aá]n )?disponibles|cr[eé]dito (?:ha sido )?confirmado).*");
        double signal = Math.min(1, (urgency ? .25 : 0) + (credentials ? .45 : 0) + (payment ? .3 : 0)
                + (financialLure ? .35 : 0) + (linkAction ? .25 : 0) + (securityAssurance ? .1 : 0));
        boolean socialEngineering = urgency && (credentials || payment) || financialLure && linkAction || securityAssurance && linkAction;
        return new AIAnalysis(socialEngineering, urgency, credentials, payment, signal,
                "Clasificador local de desarrollo; no se enviaron datos a un proveedor externo.");
    }
}
