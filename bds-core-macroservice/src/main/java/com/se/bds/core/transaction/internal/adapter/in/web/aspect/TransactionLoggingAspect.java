package com.se.bds.core.transaction.internal.adapter.in.web.aspect;

import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * AOP-based logging aspect for the Transaction bounded context.
 * Leverages OpenTelemetry traceId for distributed tracing (same pattern as PropertyLoggingAspect).
 *
 * <h3>Logging Levels</h3>
 * <ul>
 *     <li><b>Accounts Level</b> — {@code @Before} on controller methods: who did what</li>
 *     <li><b>Methods Level</b> — {@code @Around} on service methods: entry/exit + timing</li>
 *     <li><b>Events Level</b> — handled inline in service implementations via domain events</li>
 * </ul>
 */
@Aspect
@Component
@Slf4j
public class TransactionLoggingAspect {

    private static final long SLOW_THRESHOLD_MS = 5000;

    // ── Accounts Level: Log who is performing actions on transaction controllers ──

    @Before("execution(* com.se.bds.core.transaction.internal.adapter.in.web.*Controller.*(..))")
    public void logAccountsLevel(JoinPoint joinPoint) {
        String traceId = Span.current().getSpanContext().getTraceId();
        String account = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName() : "Anonymous";
        String method = joinPoint.getSignature().getName();
        String controller = joinPoint.getTarget().getClass().getSimpleName();

        log.info("[Audit] TraceID: {} | Controller: {} | Action: {} | Account: {} | Status: IN_PROGRESS",
                traceId, controller, method, maskAccount(account));
    }

    // ── Methods Level: Log entry/exit and timing for service methods ──

    @Around("execution(* com.se.bds.core.transaction.internal.application.service.*.*(..))")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String traceId = Span.current().getSpanContext().getTraceId();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        log.debug("[METHOD] ENTER {}.{}() | TraceID: {}", className, methodName, traceId);

        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long durationMs = System.currentTimeMillis() - startTime;

            if (durationMs > SLOW_THRESHOLD_MS) {
                log.warn("[METHOD] SLOW {}.{}() took {}ms (threshold: {}ms) | TraceID: {}",
                        className, methodName, durationMs, SLOW_THRESHOLD_MS, traceId);
            } else {
                log.debug("[METHOD] EXIT {}.{}() | durationMs={} | TraceID: {}",
                        className, methodName, durationMs, traceId);
            }
            return result;
        } catch (Throwable ex) {
            long durationMs = System.currentTimeMillis() - startTime;
            log.error("[METHOD] EXCEPTION in {}.{}() after {}ms: {} | TraceID: {}",
                    className, methodName, durationMs, ex.getMessage(), traceId);
            throw ex;
        }
    }

    // ── Exception logging for adapter-level failures ──

    @AfterThrowing(pointcut = "execution(* com.se.bds.core.transaction.internal.adapter.out..*.*(..))", throwing = "ex")
    public void logAdapterException(JoinPoint joinPoint, Throwable ex) {
        String traceId = Span.current().getSpanContext().getTraceId();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        log.error("[ADAPTER] FAILURE {}.{}(): {} | TraceID: {}",
                className, methodName, ex.getMessage(), traceId);
    }

    private String maskAccount(String account) {
        if (account == null || account.length() <= 3) return "***";
        return account.substring(0, 3) + "***";
    }
}
