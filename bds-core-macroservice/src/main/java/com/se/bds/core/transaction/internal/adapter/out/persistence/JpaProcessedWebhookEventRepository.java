package com.se.bds.core.transaction.internal.adapter.out.persistence;

import com.se.bds.core.transaction.internal.domain.model.ProcessedWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaProcessedWebhookEventRepository extends JpaRepository<ProcessedWebhookEvent, UUID> {

    Optional<ProcessedWebhookEvent> findByExternalEventId(String externalEventId);

    boolean existsByExternalEventId(String externalEventId);
}
