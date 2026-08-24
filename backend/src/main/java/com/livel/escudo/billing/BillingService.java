package com.livel.escudo.billing;

import com.livel.escudo.audit.AuditService;
import com.livel.escudo.common.ApiException;
import com.livel.escudo.config.AppProperties;
import com.livel.escudo.user.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
public class BillingService {
    private final PlanRepository plans; private final PlanPriceRepository prices; private final PaymentRepository payments;
    private final PaymentProvider provider; private final AuditService audit; private final AppProperties.MercadoPago mp;
    public BillingService(PlanRepository plans, PlanPriceRepository prices, PaymentRepository payments, PaymentProvider provider, AuditService audit, AppProperties props) {
        this.plans=plans; this.prices=prices; this.payments=payments; this.provider=provider; this.audit=audit; this.mp=props.mercadoPago();
    }
    @Transactional(readOnly=true) public List<PlanView> publicPlans() {
        return plans.findByActiveTrueOrderByNameAsc().stream().map(p -> {
            var price=prices.current(p.getId(),Instant.now()).orElse(null);
            return new PlanView(p.getCode(),p.getName(),price==null?BigDecimal.ZERO:price.getAmount(),price==null?"ARS":price.getCurrency(),p.getLimitsJson(),p.getFeaturesJson());
        }).toList();
    }
    @Transactional public PaymentProvider.CheckoutResult checkout(String code, UserEntity user, boolean subscription, HttpServletRequest request) {
        PlanEntity plan=plans.findByCodeAndActiveTrue(code).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"PLAN_NOT_FOUND","El plan no está disponible."));
        PlanPriceEntity price=prices.current(plan.getId(),Instant.now()).orElseThrow(() -> new ApiException(HttpStatus.CONFLICT,"PRICE_NOT_AVAILABLE","El plan no tiene un precio vigente."));
        UUID id=UUID.randomUUID(); PaymentEntity payment=payments.save(new PaymentEntity(id,user.getId(),subscription?"SUBSCRIPTION":"ONE_TIME",price.getAmount(),price.getCurrency()));
        var result=subscription?provider.createSubscription(id,"ESCUDO IA - "+plan.getName(),price.getAmount(),price.getCurrency()):provider.createOneTimeCheckout(id,"ESCUDO IA - "+plan.getName(),price.getAmount(),price.getCurrency());
        payment.checkoutCreated(result.providerId()); audit.record(user.getId(),"PAYMENT_CHECKOUT_CREATED","PAYMENT",payment.getId(),request,"SUCCESS"); return result;
    }
    @Transactional public void processWebhook(String dataId,String type,String signature,String requestId, HttpServletRequest request) {
        if (dataId==null || dataId.isBlank() || !("payment".equalsIgnoreCase(type) || type==null)) return;
        verifySignatureIfConfigured(dataId,signature,requestId);
        PaymentProvider.PaymentStatus status=provider.queryStatus(dataId);
        String eventId="payment:"+dataId+":"+status.status(); if(payments.existsByProviderEventId(eventId)) return;
        UUID internal;
        try { internal=UUID.fromString(status.externalReference()); } catch(Exception e) { throw new ApiException(HttpStatus.BAD_REQUEST,"UNKNOWN_PAYMENT","El pago no pertenece a ESCUDO IA."); }
        PaymentEntity payment=payments.findById(internal).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"PAYMENT_NOT_FOUND","No encontramos el pago informado."));
        payment.applyWebhook(status.providerId(),status.status(),eventId);
        if("CONFIRMED".equals(payment.getStatus())) audit.record(null,"PAYMENT_CONFIRMED","PAYMENT",payment.getId(),request,"SUCCESS");
    }
    private void verifySignatureIfConfigured(String dataId,String signature,String requestId) {
        if(mp.webhookSecret()==null || mp.webhookSecret().isBlank()) return;
        if(signature==null || requestId==null) throw new ApiException(HttpStatus.UNAUTHORIZED,"INVALID_WEBHOOK_SIGNATURE","Firma de webhook ausente.");
        Map<String,String> parts=new HashMap<>(); for(String p:signature.split(",")){String[] kv=p.trim().split("=",2);if(kv.length==2)parts.put(kv[0],kv[1]);}
        String ts=parts.get("ts"),v1=parts.get("v1"); if(ts==null||v1==null) throw new ApiException(HttpStatus.UNAUTHORIZED,"INVALID_WEBHOOK_SIGNATURE","Firma de webhook inválida.");
        String manifest="id:"+dataId.toLowerCase()+";request-id:"+requestId+";ts:"+ts+";";
        try { Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(mp.webhookSecret().getBytes(StandardCharsets.UTF_8),"HmacSHA256"));String expected=HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));if(!constantTime(expected,v1))throw new ApiException(HttpStatus.UNAUTHORIZED,"INVALID_WEBHOOK_SIGNATURE","Firma de webhook inválida."); }
        catch(ApiException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}
    }
    private boolean constantTime(String a,String b){return java.security.MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8));}
    public record PlanView(String code,String name,BigDecimal amount,String currency,String limitsJson,String featuresJson){}
}

