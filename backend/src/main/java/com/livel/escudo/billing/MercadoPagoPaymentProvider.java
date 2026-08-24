package com.livel.escudo.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.livel.escudo.common.ApiException;
import com.livel.escudo.config.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class MercadoPagoPaymentProvider implements PaymentProvider {
    private final AppProperties.MercadoPago settings;
    private final RestClient client;
    public MercadoPagoPaymentProvider(AppProperties properties, RestClient.Builder builder) {
        this.settings=properties.mercadoPago(); this.client=builder.baseUrl(settings.apiBaseUrl()).build();
    }
    @Override public CheckoutResult createOneTimeCheckout(UUID paymentId, String title, BigDecimal amount, String currency) {
        if (settings.mockMode()) return mock(paymentId);
        ensureConfigured();
        Map<String,Object> body=Map.of(
                "items", List.of(Map.of("id", paymentId.toString(), "title", title, "quantity", 1, "currency_id", currency, "unit_price", amount)),
                "external_reference", paymentId.toString(),
                "notification_url", settings.webhookUrl(),
                "back_urls", Map.of("success", frontend("/pago/exito"), "pending", frontend("/pago/pendiente"), "failure", frontend("/pago/error")),
                "auto_return", "approved");
        JsonNode response=post("/checkout/preferences", body);
        String url=settings.productionMode() ? response.path("init_point").asText() : response.path("sandbox_init_point").asText();
        return new CheckoutResult(response.path("id").asText(), url, settings.publicKey(), !settings.productionMode());
    }
    @Override public CheckoutResult createSubscription(UUID paymentId, String title, BigDecimal amount, String currency) {
        if (settings.mockMode()) return mock(paymentId);
        ensureConfigured();
        Map<String,Object> body=Map.of("reason", title, "external_reference", paymentId.toString(), "back_url", frontend("/pago/exito"),
                "auto_recurring", Map.of("frequency",1,"frequency_type","months","transaction_amount",amount,"currency_id",currency),
                "status","pending");
        JsonNode response=post("/preapproval", body);
        return new CheckoutResult(response.path("id").asText(), response.path("init_point").asText(), settings.publicKey(), !settings.productionMode());
    }
    @Override public PaymentStatus queryStatus(String providerPaymentId) {
        ensureConfigured();
        try {
            JsonNode r=client.get().uri("/v1/payments/{id}",providerPaymentId).header("Authorization","Bearer "+settings.accessToken())
                    .retrieve().body(JsonNode.class);
            return new PaymentStatus(r.path("id").asText(),r.path("external_reference").asText(),r.path("status").asText());
        } catch(Exception e) { throw new ApiException(HttpStatus.BAD_GATEWAY,"PAYMENT_PROVIDER_ERROR","No pudimos verificar el pago con Mercado Pago."); }
    }
    private JsonNode post(String path,Object body) {
        try { return client.post().uri(path).header("Authorization","Bearer "+settings.accessToken())
                .header("X-Idempotency-Key",UUID.randomUUID().toString()).body(body).retrieve().body(JsonNode.class); }
        catch(Exception e) { throw new ApiException(HttpStatus.BAD_GATEWAY,"PAYMENT_PROVIDER_ERROR","Mercado Pago no pudo crear el checkout."); }
    }
    private void ensureConfigured() {
        if (settings.accessToken()==null || settings.accessToken().isBlank()) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"PAYMENT_NOT_CONFIGURED","Los pagos todavía no están configurados.");
    }
    private CheckoutResult mock(UUID id) { return new CheckoutResult("mock-"+id,frontend("/pago/pendiente?mock=true"),settings.publicKey(),true); }
    private String frontend(String path) {
        String base = System.getenv("FRONTEND_URL");
        if (base == null || base.isBlank()) base = System.getenv("RENDER_EXTERNAL_URL");
        if (base == null || base.isBlank()) base = "http://localhost:5173";
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + path;
    }
}

