package com.livel.escudo.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livel.escudo.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final int limit;
    private final ObjectMapper mapper;

    public RateLimitFilter(AppProperties properties, ObjectMapper mapper) {
        this.limit = Math.max(10, properties.rateLimit().requestsPerMinute());
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/")) { chain.doFilter(request, response); return; }
        long minute = Instant.now().getEpochSecond() / 60;
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded == null ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
        String key = ip + ":" + request.getRequestURI();
        Window window = windows.compute(key, (k, old) -> old == null || old.minute != minute ? new Window(minute) : old);
        if (window.count.incrementAndGet() > limit) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getWriter(), new ApiError("RATE_LIMITED", "Demasiadas solicitudes. Intentá nuevamente en un minuto.",
                    String.valueOf(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE)), Instant.now()));
            return;
        }
        if (windows.size() > 10_000) windows.entrySet().removeIf(e -> e.getValue().minute < minute - 2);
        chain.doFilter(request, response);
    }

    private static final class Window {
        final long minute;
        final AtomicInteger count = new AtomicInteger();
        Window(long minute) { this.minute = minute; }
    }
}
