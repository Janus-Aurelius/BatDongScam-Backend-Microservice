package com.se100.bds.notificationservice.client;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Component
public class CoreServiceClientFallback implements CoreServiceClient {
    @Override
    public Map<String, Object> getContractById(UUID contractId) {
        return Collections.emptyMap();
    }
}
