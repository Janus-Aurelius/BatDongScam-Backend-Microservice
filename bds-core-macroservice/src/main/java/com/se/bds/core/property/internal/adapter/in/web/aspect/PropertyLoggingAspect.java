package com.se.bds.core.property.internal.adapter.in.web.aspect;

import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class PropertyLoggingAspect {

    @Before("execution(* com.se.bds.core.property.internal.adapter.in.web.PropertyController.*(..))")
    public void logAuditTrail(JoinPoint joinPoint) {
        // Tích hợp OpenTelemetry (Otel)
        String traceId = Span.current().getSpanContext().getTraceId();
        String account = SecurityContextHolder.getContext().getAuthentication() != null ?
                SecurityContextHolder.getContext().getAuthentication().getName() : "Anonymous";
        String method = joinPoint.getSignature().getName();

        // Ghi log chuẩn Auditability (Masking PII để tuân thủ Compliance)
        log.info("[Audit] TraceID: {} | Action: {} | Account: {} | Status: IN_PROGRESS", traceId, method, maskAccount(account));
    }

    private String maskAccount(String account) {
        if (account.length() <= 3) return "***";
        return account.substring(0, 3) + "***";
    }
}
