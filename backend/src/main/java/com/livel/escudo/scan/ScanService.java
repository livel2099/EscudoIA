package com.livel.escudo.scan;

import com.livel.escudo.audit.AuditService;
import com.livel.escudo.common.ApiException;
import com.livel.escudo.risk.RiskEngineService;
import com.livel.escudo.risk.RiskModels;
import com.livel.escudo.user.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class ScanService {
    private final RiskEngineService risk;
    private final ScanRepository scans;
    private final AuditService audit;
    @Value("${FREE_DAILY_SCAN_LIMIT:10}")
    private int freeDailyScanLimit;
    public ScanService(RiskEngineService risk, ScanRepository scans, AuditService audit) { this.risk = risk; this.scans = scans; this.audit = audit; }

    @Transactional
    public ScanResponse analyze(String type, String content, UserEntity user, HttpServletRequest request) {
        if (user != null) enforceFreeQuota(user.getId());
        RiskEngineService.Analysis analysis = "URL".equals(type) ? risk.analyzeUrl(content) : risk.analyzeText(content);
        if (user == null) return fromTransient(UUID.randomUUID(), type, analysis);
        RiskModels.Result result = analysis.result();
        ScanEntity entity = new ScanEntity(user.getId(), type, analysis.sanitizedInput(), result.score(), result.level(),
                result.classification(), result.confidence(), result.summary(), result.recommendedAction(), result.engineVersion());
        result.indicators().forEach(i -> entity.addIndicator(new RiskIndicatorEntity(i.type(), i.category(), i.score(), i.severity(), i.source(), i.confidence(), i.explanation())));
        scans.save(entity);
        audit.record(user.getId(), "SCAN_COMPLETED", "SCAN", entity.getId(), request, "SUCCESS");
        return fromEntity(entity);
    }

    @Transactional(readOnly = true)
    public Page<ScanSummary> history(UUID userId, int page, int size) {
        return scans.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size))))
                .map(s -> new ScanSummary(s.getId(), s.getType(), s.getRiskScore(), s.getRiskLevel(), s.getClassification(), s.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public ScanResponse detail(UUID id, UserEntity user) {
        ScanEntity scan = scans.findDetailedById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SCAN_NOT_FOUND", "No encontramos ese análisis."));
        if (!scan.getUserId().equals(user.getId()) && user.getRoles().stream().noneMatch(r -> r.equals("ROLE_ADMIN") || r.equals("ROLE_SUPER_ADMIN")))
            throw new ApiException(HttpStatus.FORBIDDEN, "SCAN_FORBIDDEN", "No tenés permiso para ver ese análisis.");
        return fromEntity(scan);
    }

    private void enforceFreeQuota(UUID userId) {
        Instant start = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        if (scans.countByUserIdAndCreatedAtAfter(userId, start) >= freeDailyScanLimit)
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "SCAN_LIMIT_REACHED", "Alcanzaste el límite diario del plan gratuito.");
    }

    private ScanResponse fromTransient(UUID id, String type, RiskEngineService.Analysis a) {
        var r = a.result();
        return new ScanResponse(id, type, "COMPLETED", r.score(), r.level(), r.classification(), r.confidence(), r.engineVersion(),
                r.summary(), r.recommendedAction(), r.indicators(), Instant.now(), true);
    }
    private ScanResponse fromEntity(ScanEntity s) {
        List<RiskModels.Indicator> indicators = s.getIndicators().stream().map(i -> new RiskModels.Indicator(i.getType(), i.getCategory(), i.getScore(), i.getSeverity(), i.getSource(), i.getConfidence(), i.getExplanation())).toList();
        return new ScanResponse(s.getId(), s.getType(), s.getStatus(), s.getRiskScore(), s.getRiskLevel(), s.getClassification(), s.getConfidence(),
                s.getEngineVersion(), s.getSummary(), s.getRecommendation(), indicators, s.getCreatedAt(), false);
    }

    public record ScanResponse(UUID id, String type, String status, int score, String level, String classification,
                               double confidence, String engineVersion, String summary, String recommendedAction,
                               List<RiskModels.Indicator> indicators, Instant createdAt, boolean guest) {}
    public record ScanSummary(UUID id, String type, int score, String level, String classification, Instant createdAt) {}
}

