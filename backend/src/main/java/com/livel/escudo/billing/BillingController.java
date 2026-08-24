package com.livel.escudo.billing;

import com.livel.escudo.user.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class BillingController {
    private final BillingService billing;
    public BillingController(BillingService billing){this.billing=billing;}
    @GetMapping("/api/plans") public List<BillingService.PlanView> plans(){return billing.publicPlans();}
    @GetMapping("/api/subscription") public Map<String,String> subscription(){return Map.of("plan","FREE","status","ACTIVE");}
    @PostMapping("/api/payments/scan-checkout") public PaymentProvider.CheckoutResult scanCheckout(@AuthenticationPrincipal UserEntity user,HttpServletRequest http){return billing.checkout("SCAN_PREMIUM",user,false,http);}
    @PostMapping("/api/subscription/checkout") public PaymentProvider.CheckoutResult subscriptionCheckout(@AuthenticationPrincipal UserEntity user,@Valid @RequestBody CheckoutRequest request,HttpServletRequest http){return billing.checkout(request.planCode(),user,true,http);}
    @PostMapping("/api/subscription/cancel") public ResponseEntity<Void> cancel(){return ResponseEntity.noContent().build();}
    @PostMapping({"/api/webhooks/mercadopago","/api/pagos/webhook"}) public ResponseEntity<Void> webhook(@RequestBody(required=false) Map<String,Object> body,
        @RequestParam(value="data.id",required=false) String queryDataId,@RequestParam(required=false) String type,
        @RequestHeader(value="x-signature",required=false) String signature,@RequestHeader(value="x-request-id",required=false) String requestId,HttpServletRequest http){
        String dataId=queryDataId;if(dataId==null&&body!=null&&body.get("data") instanceof Map<?,?> data)dataId=String.valueOf(data.get("id"));
        if(type==null&&body!=null&&body.get("type")!=null)type=String.valueOf(body.get("type"));billing.processWebhook(dataId,type,signature,requestId,http);return ResponseEntity.ok().build();}
    public record CheckoutRequest(@NotBlank String planCode){}
}
