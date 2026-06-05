package com.se100.bds.notificationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "core-service", fallback = CoreServiceClientFallback.class)
public interface CoreServiceClient {

    @GetMapping("/contracts/{contractId}")
    Map<String, Object> getContractById(@PathVariable("contractId") UUID contractId);
}
