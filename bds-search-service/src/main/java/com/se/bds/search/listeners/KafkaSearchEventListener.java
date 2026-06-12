package com.se.bds.search.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.event.PropertySearchedEvent;
import com.se.bds.common.event.PropertyStatusChangedEvent;
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
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "property-searched", groupId = "search-service-group")
    public void onPropertySearched(String message) {
        log.info("Received property-searched event: {}", message);
        try {
            PropertySearchedEvent event = objectMapper.readValue(message, PropertySearchedEvent.class);
            if (event.propertyId() != null) {
                searchService.addSearch(
                        event.userId(),
                        event.cityId(),
                        event.districtId(),
                        event.wardId(),
                        event.propertyId(),
                        event.propertyTypeId()
                );
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
