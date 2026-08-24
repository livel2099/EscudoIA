package com.livel.escudo.audit;

import com.livel.escudo.common.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuditService {
    private final AuditLogRepository repository;
    public AuditService(AuditLogRepository repository) { this.repository = repository; }

    public void record(UUID actorId, String action, String entityType, UUID entityId, HttpServletRequest request, String result) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null) ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");
        Object rid = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        repository.save(new AuditLogEntity(actorId, action, entityType, entityId, hash(ip), hash(ua),
                rid == null ? "unknown" : rid.toString(), result));
    }

    private String hash(String value) {
        if (value == null) return null;
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}

