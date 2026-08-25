package com.livel.escudo.scan;

import com.livel.escudo.common.ApiException;
import com.livel.escudo.user.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/scans")
public class ScanController {
    private final ScanService service;
    private final ImageTextExtractor imageTextExtractor;

    public ScanController(ScanService service, ImageTextExtractor imageTextExtractor) {
        this.service = service;
        this.imageTextExtractor = imageTextExtractor;
    }

    @PostMapping("/text")
    public ScanService.ScanResponse text(@Valid @RequestBody ScanRequest request, @AuthenticationPrincipal UserEntity user, HttpServletRequest http) {
        return service.analyze("TEXT", request.content(), user, http);
    }
    @PostMapping("/url")
    public ScanService.ScanResponse url(@Valid @RequestBody ScanRequest request, @AuthenticationPrincipal UserEntity user, HttpServletRequest http) {
        return service.analyze("URL", request.content(), user, http);
    }
    @PostMapping
    public ScanService.ScanResponse generic(@Valid @RequestBody GenericScanRequest request, @AuthenticationPrincipal UserEntity user, HttpServletRequest http) {
        String type = request.type().toUpperCase();
        if (!Set.of("TEXT", "URL").contains(type)) throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_SCAN_TYPE", "El tipo de análisis todavía no está disponible.");
        return service.analyze(type, request.content(), user, http);
    }
    @PostMapping(value = "/image", consumes = "multipart/form-data")
    public ScanService.ScanResponse image(@RequestPart("file") MultipartFile file,
                                          @RequestPart(value = "context", required = false) String context,
                                          @AuthenticationPrincipal UserEntity user, HttpServletRequest http) {
        if (file.isEmpty() || file.getSize() > 5_000_000) throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "INVALID_IMAGE", "La imagen debe pesar menos de 5 MB.");
        if (file.getContentType() == null || !Set.of("image/png", "image/jpeg", "image/webp").contains(file.getContentType()))
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "INVALID_IMAGE_TYPE", "Usá una imagen PNG, JPG o WebP.");
        if (context != null && context.length() > 2_000)
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "IMAGE_CONTEXT_TOO_LARGE", "El contexto no puede superar los 2.000 caracteres.");
        String extractedText = imageTextExtractor.extract(file);
        String analysisText = context == null || context.isBlank() ? extractedText :
                extractedText + "\n\nContexto aportado por el usuario:\n" + context.strip();
        return service.analyze("IMAGE", analysisText, user, http);
    }
    @GetMapping public Page<ScanService.ScanSummary> history(@AuthenticationPrincipal UserEntity user,
                                                             @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        return service.history(user.getId(), page, size);
    }
    @GetMapping("/{id}") public ScanService.ScanResponse detail(@PathVariable UUID id, @AuthenticationPrincipal UserEntity user) { return service.detail(id, user); }
    @GetMapping("/{id}/report") public Map<String, Object> report(@PathVariable UUID id, @AuthenticationPrincipal UserEntity user) {
        return Map.of("product", "ESCUDO IA", "disclaimer", "Evaluación de riesgo orientativa; no es una garantía de seguridad.", "scan", service.detail(id, user));
    }
    public record ScanRequest(@NotBlank @Size(max=20000) String content) {}
    public record GenericScanRequest(@NotBlank String type, @NotBlank @Size(max=20000) String content) {}
}
