package com.livel.escudo.risk;

import com.livel.escudo.ai.AIProvider;
import com.livel.escudo.common.ApiException;
import com.livel.escudo.threatintel.ThreatIntelProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.IDN;
import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

import static com.livel.escudo.risk.RiskModels.*;

@Service
public class RiskEngineService {
    private final RiskConfigRepository configs;
    private final AIProvider ai;
    private final ThreatIntelProvider threatIntel;
    private static final Pattern EMAIL = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(?:\\+?54)?[ -]?(?:9[ -]?)?\\d{2,4}[ -]?\\d{6,8}(?!\\d)");
    private static final Pattern CARD = Pattern.compile("(?<!\\d)(?:\\d[ -]?){13,19}(?!\\d)");
    private static final Pattern TAX_ID = Pattern.compile("(?<!\\d)\\d{2}-?\\d{8}-?\\d(?!\\d)");
    private static final Pattern OTP = Pattern.compile("(?i)(?:código|codigo|otp)[ :#-]*\\d{4,8}");

    public RiskEngineService(RiskConfigRepository configs, AIProvider ai, ThreatIntelProvider threatIntel) {
        this.configs = configs; this.ai = ai; this.threatIntel = threatIntel;
    }

    public Analysis analyzeText(String raw) {
        if (raw == null || raw.isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_SCAN", "Ingresá un mensaje para analizar.");
        if (raw.length() > 20_000) throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "INPUT_TOO_LARGE", "El texto supera los 20.000 caracteres.");
        String sanitized = sanitize(raw.strip());
        return new Analysis(sanitized, calculate(sanitized, null));
    }

    public Analysis analyzeUrl(String raw) {
        if (raw == null || raw.isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_SCAN", "Ingresá una URL para analizar.");
        String candidate = raw.trim().matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*") ? raw.trim() : "https://" + raw.trim();
        URI uri;
        try { uri = URI.create(candidate); } catch (IllegalArgumentException e) { throw invalidUrl(); }
        if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) throw invalidUrl();
        String host = IDN.toASCII(uri.getHost().toLowerCase(Locale.ROOT));
        if (isPrivateHost(host)) throw new ApiException(HttpStatus.BAD_REQUEST, "PRIVATE_URL_BLOCKED", "No analizamos destinos locales o de red privada.");
        String normalized = uri.getScheme().toLowerCase() + "://" + host + (uri.getPort() > 0 ? ":" + uri.getPort() : "") +
                (uri.getRawPath() == null || uri.getRawPath().isBlank() ? "/" : uri.getRawPath()) +
                (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        return new Analysis(normalized, calculate(normalized, host));
    }

    private Result calculate(String text, String domain) {
        String lower = text.toLowerCase(Locale.ROOT);
        List<Indicator> found = new ArrayList<>();
        add(found, lower.matches("(?s).*(urgente|inmediatamente|ahora mismo|último aviso|ultimo aviso|cuenta suspendida|vence hoy).*") ,
                "URGENCY", "SOCIAL_ENGINEERING", 75, "HIGH", .86, "Usa presión temporal para acelerar una decisión.");
        add(found, lower.matches("(?s).*(contraseña|contrasena|iniciar sesión|iniciar sesion|verificá tu cuenta|verifica tu cuenta|credenciales).*") ,
                "CREDENTIAL_REQUEST", "SOCIAL_ENGINEERING", 100, "HIGH", .9, "Solicita datos de acceso o verificación de cuenta.");
        add(found, lower.matches("(?s).*(código otp|codigo otp|código de verificación|codigo de verificacion|token).*") ,
                "OTP_REQUEST", "CONTEXT_BEHAVIOR", 100, "CRITICAL", .94, "Solicita un código de un solo uso.");
        add(found, lower.matches("(?s).*(transferí|transferi|transferencia|pagá|paga ahora|cv[ub]|alias|premio|deuda).*") ,
                "PAYMENT_REQUEST", "CONTEXT_BEHAVIOR", 90, "HIGH", .86, "Incluye una solicitud o incentivo de pago.");
        if (domain != null) analyzeDomain(domain, lower, found);
        AIProvider.AIAnalysis aiResult = ai.analyzeSanitized(text);
        add(found, aiResult.riskSignal() >= .5, "AI_CLASSIFIER", "AI_CLASSIFIER",
                (int)Math.round(aiResult.riskSignal() * 100), "MEDIUM", .65, aiResult.explanation());
        if (domain != null) {
            var reputation = threatIntel.lookup(domain);
            add(found, reputation.knownThreat(), "KNOWN_THREAT_MATCH", "URL_REPUTATION", 100, "CRITICAL",
                    reputation.confidence(), "Coincide con inteligencia de amenazas conocida.");
        }
        Map<String, RiskConfigEntity> weights = new HashMap<>();
        configs.findAll().forEach(c -> weights.put(c.getComponentCode(), c));
        Map<String, Integer> strongest = new HashMap<>();
        found.forEach(i -> strongest.merge(i.category(), i.score(), Math::max));
        int score = (int)Math.round(strongest.entrySet().stream().mapToDouble(e ->
                weights.getOrDefault(e.getKey(), defaultConfig(e.getKey())).getWeight() * (e.getValue() / 100.0)).sum());
        score = Math.max(0, Math.min(100, score));
        String level = score <= 20 ? "LOW" : score <= 40 ? "CAUTION" : score <= 60 ? "MEDIUM" : score <= 80 ? "HIGH" : "CRITICAL";
        boolean credential = has(found, "CREDENTIAL_REQUEST"); boolean brand = has(found, "BRAND_IMPERSONATION");
        String classification = credential && brand ? "PHISHING" : has(found, "PAYMENT_REQUEST") && score > 40 ? "FRAUD" : score > 20 ? "SUSPICIOUS" : "LOW_RISK";
        String version = weights.values().stream().map(RiskConfigEntity::getVersion).findFirst().orElse("risk-1.0.0");
        String summary = found.isEmpty() ? "No se identificaron indicadores significativos de riesgo en este análisis."
                : "Se detectaron " + found.size() + " indicadores que conviene revisar antes de continuar.";
        String action = score > 60 ? "DO_NOT_PROCEED" : score > 20 ? "VERIFY_OFFICIAL_CHANNEL" : "PROCEED_WITH_CAUTION";
        double confidence = found.isEmpty() ? .58 : found.stream().mapToDouble(Indicator::confidence).average().orElse(.6);
        return new Result(score, Math.round(confidence * 100.0) / 100.0, classification, level, version, List.copyOf(found), summary, action);
    }

    private void analyzeDomain(String domain, String url, List<Indicator> found) {
        Set<String> shorteners = Set.of("bit.ly", "tinyurl.com", "t.co", "cutt.ly", "goo.su", "is.gd");
        Set<String> riskyTlds = Set.of("zip", "mov", "click", "top", "xyz", "work", "support");
        add(found, shorteners.contains(domain), "SHORTENED_URL", "URL_REPUTATION", 75, "MEDIUM", .9, "El enlace oculta el destino mediante un acortador.");
        String tld = domain.contains(".") ? domain.substring(domain.lastIndexOf('.') + 1) : "";
        add(found, riskyTlds.contains(tld), "SUSPICIOUS_TLD", "DOMAIN_ANALYSIS", 80, "MEDIUM", .72, "La extensión del dominio aparece con frecuencia en campañas abusivas.");
        add(found, domain.startsWith("xn--"), "UNICODE_HOMOGLYPH", "BRAND_IMPERSONATION", 95, "HIGH", .82, "El dominio usa caracteres internacionalizados que pueden imitar otros nombres.");
        Set<String> brands = Set.of("mercadopago", "mercadolibre", "bancogalicia", "santander", "bbva", "brubank", "uala");
        boolean brandLike = brands.stream().anyMatch(b -> domain.contains(b) && !domain.equals(b + ".com") && !domain.equals(b + ".com.ar"));
        add(found, brandLike, "BRAND_IMPERSONATION", "BRAND_IMPERSONATION", 100, "HIGH", .8, "El dominio parece usar el nombre de una marca sin coincidir con su dominio principal.");
        add(found, url.matches("(?s).*(login|verify|verificar|secure|cuenta|wallet|premio).*") , "URL_REPUTATION", "URL_REPUTATION", 55, "MEDIUM", .65, "La ruta contiene términos habituales en páginas de captación de datos.");
    }

    public String sanitize(String raw) {
        String value = EMAIL.matcher(raw).replaceAll("[REDACTED_EMAIL]");
        value = TAX_ID.matcher(value).replaceAll("[REDACTED_TAX_ID]");
        value = CARD.matcher(value).replaceAll("[REDACTED_CARD]");
        value = PHONE.matcher(value).replaceAll("[REDACTED_PHONE]");
        return OTP.matcher(value).replaceAll("[REDACTED_OTP]");
    }

    private void add(List<Indicator> list, boolean condition, String type, String category, int score, String severity, double confidence, String explanation) {
        if (condition) list.add(new Indicator(type, category, score, severity, type.equals("SOCIAL_ENGINEERING") ? "AI_MOCK" : "HEURISTIC", confidence, explanation));
    }
    private boolean has(List<Indicator> list, String type) { return list.stream().anyMatch(i -> i.type().equals(type)); }
    private ApiException invalidUrl() { return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_URL", "La URL no tiene un formato válido."); }
    private boolean isPrivateHost(String host) {
        return host.equals("localhost") || host.equals("0.0.0.0") || host.equals("::1") || host.startsWith("127.") || host.startsWith("10.") ||
                host.startsWith("192.168.") || host.matches("172\\.(1[6-9]|2\\d|3[01])\\..*") || host.equals("169.254.169.254") || host.endsWith(".local");
    }
    private RiskConfigEntity defaultConfig(String ignored) { return new RiskConfigEntityProxy(); }
    private static final class RiskConfigEntityProxy extends RiskConfigEntity {
        @Override public int getWeight() { return 0; }
        @Override public String getVersion() { return "risk-1.0.0"; }
    }
    public record Analysis(String sanitizedInput, Result result) {}
}
