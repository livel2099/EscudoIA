package com.livel.escudo.admin;

import com.livel.escudo.audit.AuditService;
import com.livel.escudo.common.ApiException;
import com.livel.escudo.risk.RiskConfigEntity;
import com.livel.escudo.risk.RiskConfigRepository;
import com.livel.escudo.scan.ScanRepository;
import com.livel.escudo.user.UserEntity;
import com.livel.escudo.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserRepository users; private final ScanRepository scans; private final RiskConfigRepository config; private final AuditService audit;
    public AdminController(UserRepository users,ScanRepository scans,RiskConfigRepository config,AuditService audit){this.users=users;this.scans=scans;this.config=config;this.audit=audit;}
    @GetMapping("/users") @Transactional(readOnly=true) public Page<UserView> users(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="25")int size){
        return users.findAll(PageRequest.of(Math.max(0,page),Math.min(100,Math.max(1,size)))).map(u->new UserView(u.getId(),u.getEmail(),u.getStatus(),u.getLocale(),u.getRoles()));}
    @GetMapping("/scans") @Transactional(readOnly=true) public Page<ScanView> scans(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="25")int size){
        return scans.findAll(PageRequest.of(Math.max(0,page),Math.min(100,Math.max(1,size)))).map(s->new ScanView(s.getId(),s.getUserId(),s.getType(),s.getRiskScore(),s.getRiskLevel(),s.getClassification(),s.getCreatedAt()));}
    @GetMapping("/risk-config") public List<ConfigView> riskConfig(){return config.findAll().stream().map(c->new ConfigView(c.getComponentCode(),c.getWeight(),c.getVersion())).toList();}
    @PutMapping("/risk-config") @Transactional public List<ConfigView> update(@Valid @RequestBody List<ConfigUpdate> updates,@AuthenticationPrincipal UserEntity actor,HttpServletRequest request){
        if(updates.stream().mapToInt(ConfigUpdate::weight).sum()!=100)throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_RISK_WEIGHTS","Los pesos del motor deben sumar 100.");
        Set<String> known=config.findAll().stream().map(RiskConfigEntity::getComponentCode).collect(java.util.stream.Collectors.toSet());
        if(updates.size()!=known.size()||!updates.stream().map(ConfigUpdate::component).collect(java.util.stream.Collectors.toSet()).equals(known))
            throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_RISK_COMPONENTS","Debés enviar todos los componentes configurados.");
        String version="risk-"+Instant.now().getEpochSecond();
        updates.forEach(u->{RiskConfigEntity c=config.findById(u.component()).orElseThrow();c.update(u.weight(),version);});
        audit.record(actor.getId(),"RISK_CONFIG_UPDATED","RISK_CONFIG",null,request,"SUCCESS");return riskConfig();}
    public record UserView(UUID id,String email,String status,String locale,Set<String> roles){}
    public record ScanView(UUID id,UUID userId,String type,int score,String level,String classification,Instant createdAt){}
    public record ConfigView(String component,int weight,String version){}
    public record ConfigUpdate(@NotBlank String component,@Min(0)@Max(100)int weight){}
}

