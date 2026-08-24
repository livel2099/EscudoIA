package com.livel.escudo.ai;

import org.springframework.stereotype.Component;

@Component
public class MockAIProvider implements AIProvider {
    @Override public AIAnalysis analyzeSanitized(String text) {
        String lower = text.toLowerCase();
        boolean urgency = lower.matches("(?s).*(urgente|ahora|inmediatamente|último aviso|ultimo aviso|suspendid[ao]).*");
        boolean credentials = lower.matches("(?s).*(contraseña|contrasena|clave|usuario|iniciar sesión|iniciar sesion).*");
        boolean payment = lower.matches("(?s).*(pagar|transfer|cv[u|b]|dinero|premio|deuda).*");
        double signal = Math.min(1, (urgency ? .25 : 0) + (credentials ? .45 : 0) + (payment ? .3 : 0));
        return new AIAnalysis((urgency && (credentials || payment)), urgency, credentials, payment, signal,
                "Clasificador local de desarrollo; no se enviaron datos a un proveedor externo.");
    }
}

