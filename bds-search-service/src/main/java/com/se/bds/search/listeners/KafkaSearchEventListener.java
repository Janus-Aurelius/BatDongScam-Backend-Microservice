package com.se.bds.search.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.event.PropertySearchedEvent;
import com.se.bds.common.event.PropertyStatusChangedEvent;
import com.se.bds.search.client.CoreServiceClient;
import com.se.bds.search.services.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaSearchEventListener {

    private final SearchService searchService;
    private final CoreServiceClient coreServiceClient;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "property-searched", groupId = "search-service-group")
    public void onPropertySearched(String message) {
        log.info("Received property-searched event: {}", message);
        try {
            PropertySearchedEvent event = objectMapper.readValue(message, PropertySearchedEvent.class);
            if (event.propertyId() != null) {
                CoreServiceClient.PropertyLocationInfo info = coreServiceClient.getPropertyLocationInfo(event.propertyId());
                if (info != null) {
                    searchService.addSearch(
                            event.userId(),
                            info.cityId(),
                            info.districtId(),
                            info.wardId(),
                            event.propertyId(),
                            info.propertyTypeId()
                    );
                } else {
                    log.warn("Property location info not found for propertyId={}", event.propertyId());
                }
            }
        } catch (Exception e) {
            log.error("Failed to process property-searched event", e);
        }
    }

    @KafkaListener(topics = "property-status-changed", groupId = "search-service-group")
    public void onPropertyStatusChanged(String message) {
        log.info("Received property-status-changed event: {}", message);
        try {
            PropertyStatusChangedEvent event = objectMapper.readValue(message, PropertyStatusChangedEvent.class);
            searchService.handlePropertyStatusChanged(event);
        } catch (Exception e) {
            log.error("Failed to process property-status-changed event", e);
        }
    }
}
