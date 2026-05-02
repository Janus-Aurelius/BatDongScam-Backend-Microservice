package com.se100.bds.services.domains.report.impl;

import com.se100.bds.models.entities.property.Property;
import com.se100.bds.models.schemas.report.FinancialReport;
import com.se100.bds.models.schemas.report.RankedRevenueItem;
import com.se100.bds.repositories.domains.mongo.report.FinancialReportRepository;
import com.se100.bds.repositories.domains.property.PropertyRepository;
import com.se100.bds.services.domains.report.FinancialUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class FinancialUpdateServiceImpl implements FinancialUpdateService {
    private final FinancialReportRepository financialReportRepository;
    private final PropertyRepository propertyRepository;

    @Override
    @Async
    public void transaction(UUID propertyId, BigDecimal value, int month, int year) {
        Property property = propertyRepository.findById(propertyId).orElse(null);
        if (property == null) return;

        UUID propertyTypeId = property.getPropertyType().getId();
        UUID wardId = property.getWard().getId();
        UUID districtId = property.getWard().getDistrict().getId();
        UUID cityId = property.getWard().getDistrict().getCity().getId();

        FinancialReport financialReport = financialReportRepository.findByBaseReportData_MonthAndBaseReportData_Year(month, year);
        if (financialReport == null) return;

        CompletableFuture<Void> cityFuture = CompletableFuture.runAsync(() -> {
            for (RankedRevenueItem city : financialReport.getRevenueCities()) {
                if (cityId.equals(city.getId())) {
                    city.setRevenue(city.getRevenue().add(value));
                }
            }
        });

        CompletableFuture<Void> districtFuture = CompletableFuture.runAsync(() -> {
            for (RankedRevenueItem district : financialReport.getRevenueDistricts()) {
                if (districtId.equals(district.getId())) {
                    district.setRevenue(district.getRevenue().add(value));
                }
            }
        });

        CompletableFuture<Void> wardFuture = CompletableFuture.runAsync(() -> {
            for (RankedRevenueItem ward : financialReport.getRevenueWards()) {
                if (wardId.equals(ward.getId())) {
                    ward.setRevenue(ward.getRevenue().add(value));
                }
            }
        });

        CompletableFuture<Void> propertyTypeFuture = CompletableFuture.runAsync(() -> {
            for (RankedRevenueItem propertyType : financialReport.getRevenuePropertyTypes()) {
                if (propertyTypeId.equals(propertyType.getId())) {
                    propertyType.setRevenue(propertyType.getRevenue().add(value));
                }
            }
        });

        CompletableFuture.allOf(cityFuture, districtFuture, wardFuture, propertyTypeFuture).join();

        financialReport.setTotalRevenue(financialReport.getTotalRevenue().add(value));
        financialReportRepository.save(financialReport);
    }
}
