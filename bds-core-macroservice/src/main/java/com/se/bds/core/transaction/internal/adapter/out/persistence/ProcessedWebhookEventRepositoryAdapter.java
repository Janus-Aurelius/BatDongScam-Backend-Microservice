package com.se.bds.core.transaction.internal.adapter.out.persistence;

import com.se.bds.core.transaction.internal.application.port.out.ProcessedWebhookEventRepository;
import com.se.bds.core.transaction.internal.domain.model.ProcessedWebhookEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProcessedWebhookEventRepositoryAdapter implements ProcessedWebhookEventRepository {

    private final JpaProcessedWebhookEventRepository jpaProcessedWebhookEventRepository;

    @Override
    public ProcessedWebhookEvent save(ProcessedWebhookEvent event) {
        return jpaProcessedWebhookEventRepository.save(event);
    }

    @Override
    public Optional<ProcessedWebhookEvent> findByExternalEventId(String externalEventId) {
        return jpaProcessedWebhookEventRepository.findByExternalEventId(externalEventId);
    }

    @Override
    public boolean existsByExternalEventId(String externalEventId) {
        return jpaProcessedWebhookEventRepository.existsByExternalEventId(externalEventId);
    }
}
