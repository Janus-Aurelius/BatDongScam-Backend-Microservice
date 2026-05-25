package com.se.bds.core.transaction.internal.application.port.out;

import com.se.bds.core.transaction.internal.domain.model.ProcessedWebhookEvent;

import java.util.Optional;

/**
 * Repository port for webhook event deduplication (US-011).
 */
public interface ProcessedWebhookEventRepository {

    ProcessedWebhookEvent save(ProcessedWebhookEvent event);

    Optional<ProcessedWebhookEvent> findByExternalEventId(String externalEventId);

    boolean existsByExternalEventId(String externalEventId);
}
