package microservices.appointmentservice.services.ranking.impl;

import microservices.appointmentservice.repositories.ranking.*;
import microservices.appointmentservice.schemas.ranking.*;
import microservices.appointmentservice.services.ranking.RankingService;
import microservices.appointmentservice.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankingServiceImpl implements RankingService {

    private final IndividualSalesAgentPerformanceMonthRepository agentMonthRepo;
    private final IndividualSalesAgentPerformanceCareerRepository agentCareerRepo;
    private final IndividualCustomerPotentialMonthRepository customerMonthRepo;
    private final IndividualCustomerPotentialAllRepository customerAllRepo;
    private final IndividualPropertyOwnerContributionMonthRepository ownerMonthRepo;
    private final IndividualPropertyOwnerContributionAllRepository ownerAllRepo;

    // ──────────────────────── Internal helpers ────────────────────────

    @Override
    public String getTier(UUID userId, Constants.RoleEnum role, int month, int year) {
        return switch (role) {
            case SALESAGENT -> agentMonthRepo.findByAgentIdAndMonthAndYear(userId, month, year)
                    .map(r -> r.getPerformanceTier() != null ? r.getPerformanceTier().getValue() : Constants.PerformanceTierEnum.BRONZE.getValue())
                    .orElse(Constants.PerformanceTierEnum.BRONZE.getValue());
            case CUSTOMER -> customerMonthRepo.findByCustomerIdAndMonthAndYear(userId, month, year)
                    .map(r -> r.getCustomerTier() != null ? r.getCustomerTier().getValue() : Constants.CustomerTierEnum.BRONZE.getValue())
                    .orElse(Constants.CustomerTierEnum.BRONZE.getValue());
            case PROPERTY_OWNER -> ownerMonthRepo.findByOwnerIdAndMonthAndYear(userId, month, year)
                    .map(r -> r.getContributionTier() != null ? r.getContributionTier().getValue() : Constants.ContributionTierEnum.BRONZE.getValue())
                    .orElse(Constants.ContributionTierEnum.BRONZE.getValue());
            default -> "BRONZE";
        };
    }

    @Override
    public String getCurrentTier(UUID userId, Constants.RoleEnum role) {
        LocalDateTime now = LocalDateTime.now();
        return getTier(userId, role, now.getMonthValue(), now.getYear());
    }

    @Override
    public IndividualSalesAgentPerformanceMonth getSaleAgentCurrentMonth(UUID agentId) {
        LocalDateTime now = LocalDateTime.now();
        return getSaleAgentMonth(agentId, now.getMonthValue(), now.getYear());
    }

    @Override
    public IndividualCustomerPotentialMonth getCustomerCurrentMonth(UUID customerId) {
        LocalDateTime now = LocalDateTime.now();
        return getCustomerMonth(customerId, now.getMonthValue(), now.getYear());
    }

    @Override
    public IndividualPropertyOwnerContributionMonth getPropertyOwnerCurrentMonth(UUID propertyOwnerId) {
        LocalDateTime now = LocalDateTime.now();
        return getPropertyOwnerMonth(propertyOwnerId, now.getMonthValue(), now.getYear());
    }

    // ──────────────────────── REST endpoint methods ────────────────────────

    @Override
    public IndividualSalesAgentPerformanceMonth getSaleAgentMonth(UUID agentId, int month, int year) {
        return agentMonthRepo.findByAgentIdAndMonthAndYear(agentId, month, year)
                .orElseGet(() -> agentMonthRepo.save(IndividualSalesAgentPerformanceMonth.builder()
                        .agentId(agentId)
                        .month(month)
                        .year(year)
                        .performancePoint(0)
                        .performanceTier(Constants.PerformanceTierEnum.BRONZE)
                        .rankingPosition(0)
                        .handlingProperties(0)
                        .monthPropertiesAssigned(0)
                        .monthAppointmentsAssigned(0)
                        .monthAppointmentsCompleted(0)
                        .monthContracts(0)
                        .monthRates(0)
                        .avgRating(BigDecimal.ZERO)
                        .monthCustomerSatisfactionAvg(BigDecimal.ZERO)
                        .build()));
    }

    @Override
    public IndividualSalesAgentPerformanceCareer getSaleAgentCareer(UUID agentId) {
        return agentCareerRepo.findByAgentId(agentId)
                .orElseGet(() -> agentCareerRepo.save(IndividualSalesAgentPerformanceCareer.builder()
                        .agentId(agentId)
                        .performancePoint(0)
                        .careerRanking(0)
                        .propertiesAssigned(0)
                        .appointmentAssigned(0)
                        .appointmentCompleted(0)
                        .totalContracts(0)
                        .customerSatisfactionAvg(BigDecimal.ZERO)
                        .totalRates(0)
                        .avgRating(BigDecimal.ZERO)
                        .build()));
    }

    @Override
    public IndividualCustomerPotentialMonth getCustomerMonth(UUID customerId, int month, int year) {
        return customerMonthRepo.findByCustomerIdAndMonthAndYear(customerId, month, year)
                .orElseGet(() -> customerMonthRepo.save(IndividualCustomerPotentialMonth.builder()
                        .customerId(customerId)
                        .month(month)
                        .year(year)
                        .leadScore(0)
                        .customerTier(Constants.CustomerTierEnum.BRONZE)
                        .leadPosition(0)
                        .monthViewingsRequested(0)
                        .monthViewingAttended(0)
                        .monthSpending(BigDecimal.ZERO)
                        .monthPurchases(0)
                        .monthRentals(0)
                        .monthContractsSigned(0)
                        .build()));
    }

    @Override
    public IndividualCustomerPotentialAll getCustomerAll(UUID customerId) {
        return customerAllRepo.findByCustomerId(customerId)
                .orElseGet(() -> customerAllRepo.save(IndividualCustomerPotentialAll.builder()
                        .customerId(customerId)
                        .leadScore(0)
                        .leadPosition(0)
                        .viewingsRequested(0)
                        .viewingsAttended(0)
                        .spending(BigDecimal.ZERO)
                        .totalPurchases(0)
                        .totalRentals(0)
                        .totalContractsSigned(0)
                        .build()));
    }

    @Override
    public IndividualPropertyOwnerContributionMonth getPropertyOwnerMonth(UUID propertyOwnerId, int month, int year) {
        return ownerMonthRepo.findByOwnerIdAndMonthAndYear(propertyOwnerId, month, year)
                .orElseGet(() -> ownerMonthRepo.save(IndividualPropertyOwnerContributionMonth.builder()
                        .ownerId(propertyOwnerId)
                        .month(month)
                        .year(year)
                        .contributionPoint(0)
                        .contributionTier(Constants.ContributionTierEnum.BRONZE)
                        .rankingPosition(0)
                        .monthContributionValue(BigDecimal.ZERO)
                        .monthTotalProperties(0)
                        .monthTotalForSales(0)
                        .monthTotalForRents(0)
                        .monthTotalPropertiesSold(0)
                        .monthTotalPropertiesRented(0)
                        .build()));
    }

    @Override
    public IndividualPropertyOwnerContributionAll getPropertyOwnerAll(UUID propertyOwnerId) {
        return ownerAllRepo.findByOwnerId(propertyOwnerId)
                .orElseGet(() -> ownerAllRepo.save(IndividualPropertyOwnerContributionAll.builder()
                        .ownerId(propertyOwnerId)
                        .contributionPoint(0)
                        .rankingPosition(0)
                        .contributionValue(BigDecimal.ZERO)
                        .totalProperties(0)
                        .totalPropertiesSold(0)
                        .totalPropertiesRented(0)
                        .build()));
    }

    @Override
    public IndividualSalesAgentPerformanceMonth getMySaleAgentMonth(int month, int year) {
        throw new UnsupportedOperationException("Use getSaleAgentMonth with explicit agentId");
    }

    @Override
    public IndividualSalesAgentPerformanceCareer getMySaleAgentCareer() {
        throw new UnsupportedOperationException("Use getSaleAgentCareer with explicit agentId");
    }

    @Override
    public IndividualCustomerPotentialMonth getMyCustomerMonth(int month, int year) {
        throw new UnsupportedOperationException("Use getCustomerMonth with explicit customerId");
    }

    @Override
    public IndividualCustomerPotentialAll getMyCustomerAll() {
        throw new UnsupportedOperationException("Use getCustomerAll with explicit customerId");
    }

    @Override
    public IndividualPropertyOwnerContributionMonth getMyPropertyOwnerMonth(int month, int year) {
        throw new UnsupportedOperationException("Use getPropertyOwnerMonth with explicit ownerId");
    }

    @Override
    public IndividualPropertyOwnerContributionAll getMyPropertyOwnerAll() {
        throw new UnsupportedOperationException("Use getPropertyOwnerAll with explicit ownerId");
    }

    // ──────────────────────── Action methods ────────────────────────

    @Override
    @Async
    public void agentAction(UUID agentId, Constants.AgentActionEnum actionType, BigDecimal amount) {
        try {
            LocalDateTime now = LocalDateTime.now();
            int month = now.getMonthValue();
            int year = now.getYear();

            IndividualSalesAgentPerformanceMonth monthRecord = getSaleAgentMonth(agentId, month, year);
            IndividualSalesAgentPerformanceCareer careerRecord = getSaleAgentCareer(agentId);

            switch (actionType) {
                case APPOINTMENT_ASSIGNED -> {
                    monthRecord.setMonthAppointmentsAssigned(orZero(monthRecord.getMonthAppointmentsAssigned()) + 1);
                    careerRecord.setAppointmentAssigned(orZero(careerRecord.getAppointmentAssigned()) + 1);
                }
                case APPOINTMENT_COMPLETED -> {
                    monthRecord.setMonthAppointmentsCompleted(orZero(monthRecord.getMonthAppointmentsCompleted()) + 1);
                    careerRecord.setAppointmentCompleted(orZero(careerRecord.getAppointmentCompleted()) + 1);
                    monthRecord.setPerformancePoint(orZero(monthRecord.getPerformancePoint()) + 10);
                    careerRecord.setPerformancePoint(orZero(careerRecord.getPerformancePoint()) + 10);
                }
                case APPOINTMENT_CANCELLED -> {
                    monthRecord.setPerformancePoint(Math.max(0, orZero(monthRecord.getPerformancePoint()) - 5));
                    careerRecord.setPerformancePoint(Math.max(0, orZero(careerRecord.getPerformancePoint()) - 5));
                }
                case RATED -> {
                    if (amount != null) {
                        int totalRatesMonth = orZero(monthRecord.getMonthRates()) + 1;
                        int totalRatesCareer = orZero(careerRecord.getTotalRates()) + 1;
                        BigDecimal currentAvgMonth = monthRecord.getAvgRating() != null ? monthRecord.getAvgRating() : BigDecimal.ZERO;
                        BigDecimal currentAvgCareer = careerRecord.getAvgRating() != null ? careerRecord.getAvgRating() : BigDecimal.ZERO;
                        BigDecimal newAvgMonth = currentAvgMonth.multiply(BigDecimal.valueOf(totalRatesMonth - 1))
                                .add(amount).divide(BigDecimal.valueOf(totalRatesMonth), 2, RoundingMode.HALF_UP);
                        BigDecimal newAvgCareer = currentAvgCareer.multiply(BigDecimal.valueOf(totalRatesCareer - 1))
                                .add(amount).divide(BigDecimal.valueOf(totalRatesCareer), 2, RoundingMode.HALF_UP);
                        monthRecord.setMonthRates(totalRatesMonth);
                        monthRecord.setAvgRating(newAvgMonth);
                        careerRecord.setTotalRates(totalRatesCareer);
                        careerRecord.setAvgRating(newAvgCareer);
                    }
                }
                case CONTRACT_SIGNED -> {
                    monthRecord.setMonthContracts(orZero(monthRecord.getMonthContracts()) + 1);
                    careerRecord.setTotalContracts(orZero(careerRecord.getTotalContracts()) + 1);
                    monthRecord.setPerformancePoint(orZero(monthRecord.getPerformancePoint()) + 20);
                    careerRecord.setPerformancePoint(orZero(careerRecord.getPerformancePoint()) + 20);
                }
                case PROPERTY_ASSIGNED -> {
                    monthRecord.setMonthPropertiesAssigned(orZero(monthRecord.getMonthPropertiesAssigned()) + 1);
                    careerRecord.setPropertiesAssigned(orZero(careerRecord.getPropertiesAssigned()) + 1);
                }
                default -> log.warn("Unknown agent action: {}", actionType);
            }

            monthRecord.setPerformanceTier(calculateAgentTier(monthRecord.getPerformancePoint()));
            agentMonthRepo.save(monthRecord);
            agentCareerRepo.save(careerRecord);
        } catch (Exception e) {
            log.error("Error processing agent action {} for agent {}: {}", actionType, agentId, e.getMessage());
        }
    }

    @Override
    @Async
    public void customerAction(UUID customerId, Constants.CustomerActionEnum actionType, BigDecimal amount) {
        try {
            LocalDateTime now = LocalDateTime.now();
            int month = now.getMonthValue();
            int year = now.getYear();

            IndividualCustomerPotentialMonth monthRecord = getCustomerMonth(customerId, month, year);
            IndividualCustomerPotentialAll allRecord = getCustomerAll(customerId);

            switch (actionType) {
                case VIEWING_REQUESTED -> {
                    monthRecord.setMonthViewingsRequested(orZero(monthRecord.getMonthViewingsRequested()) + 1);
                    allRecord.setViewingsRequested(orZero(allRecord.getViewingsRequested()) + 1);
                    monthRecord.setLeadScore(orZero(monthRecord.getLeadScore()) + 2);
                    allRecord.setLeadScore(orZero(allRecord.getLeadScore()) + 2);
                }
                case VIEWING_ATTENDED -> {
                    monthRecord.setMonthViewingAttended(orZero(monthRecord.getMonthViewingAttended()) + 1);
                    allRecord.setViewingsAttended(orZero(allRecord.getViewingsAttended()) + 1);
                    monthRecord.setLeadScore(orZero(monthRecord.getLeadScore()) + 5);
                    allRecord.setLeadScore(orZero(allRecord.getLeadScore()) + 5);
                }
                case VIEWING_CANCELLED -> {
                    monthRecord.setLeadScore(Math.max(0, orZero(monthRecord.getLeadScore()) - 1));
                    allRecord.setLeadScore(Math.max(0, orZero(allRecord.getLeadScore()) - 1));
                }
                case SPENDING_MADE -> {
                    if (amount != null) {
                        monthRecord.setMonthSpending(orZeroDecimal(monthRecord.getMonthSpending()).add(amount));
                        allRecord.setSpending(orZeroDecimal(allRecord.getSpending()).add(amount));
                    }
                }
                case PURCHASE_MADE -> {
                    monthRecord.setMonthPurchases(orZero(monthRecord.getMonthPurchases()) + 1);
                    allRecord.setTotalPurchases(orZero(allRecord.getTotalPurchases()) + 1);
                }
                case RENTAL_MADE -> {
                    monthRecord.setMonthRentals(orZero(monthRecord.getMonthRentals()) + 1);
                    allRecord.setTotalRentals(orZero(allRecord.getTotalRentals()) + 1);
                }
                case CONTRACT_SIGNED -> {
                    monthRecord.setMonthContractsSigned(orZero(monthRecord.getMonthContractsSigned()) + 1);
                    allRecord.setTotalContractsSigned(orZero(allRecord.getTotalContractsSigned()) + 1);
                }
                default -> log.warn("Unknown customer action: {}", actionType);
            }

            monthRecord.setCustomerTier(calculateCustomerTier(monthRecord.getLeadScore()));
            customerMonthRepo.save(monthRecord);
            customerAllRepo.save(allRecord);
        } catch (Exception e) {
            log.error("Error processing customer action {} for customer {}: {}", actionType, customerId, e.getMessage());
        }
    }

    @Override
    @Async
    public void propertyOwnerAction(UUID ownerId, Constants.PropertyOwnerActionEnum actionType, BigDecimal amount) {
        try {
            LocalDateTime now = LocalDateTime.now();
            int month = now.getMonthValue();
            int year = now.getYear();

            IndividualPropertyOwnerContributionMonth monthRecord = getPropertyOwnerMonth(ownerId, month, year);
            IndividualPropertyOwnerContributionAll allRecord = getPropertyOwnerAll(ownerId);

            switch (actionType) {
                case PROPERTY_FOR_SALE_LISTED -> {
                    monthRecord.setMonthTotalForSales(orZero(monthRecord.getMonthTotalForSales()) + 1);
                    monthRecord.setMonthTotalProperties(orZero(monthRecord.getMonthTotalProperties()) + 1);
                    allRecord.setTotalProperties(orZero(allRecord.getTotalProperties()) + 1);
                }
                case PROPERTY_FOR_RENT_LISTED -> {
                    monthRecord.setMonthTotalForRents(orZero(monthRecord.getMonthTotalForRents()) + 1);
                    monthRecord.setMonthTotalProperties(orZero(monthRecord.getMonthTotalProperties()) + 1);
                    allRecord.setTotalProperties(orZero(allRecord.getTotalProperties()) + 1);
                }
                case PROPERTY_SOLD -> {
                    monthRecord.setMonthTotalPropertiesSold(orZero(monthRecord.getMonthTotalPropertiesSold()) + 1);
                    allRecord.setTotalPropertiesSold(orZero(allRecord.getTotalPropertiesSold()) + 1);
                }
                case PROPERTY_RENTED -> {
                    monthRecord.setMonthTotalPropertiesRented(orZero(monthRecord.getMonthTotalPropertiesRented()) + 1);
                    allRecord.setTotalPropertiesRented(orZero(allRecord.getTotalPropertiesRented()) + 1);
                }
                case MONEY_RECEIVED -> {
                    if (amount != null) {
                        monthRecord.setMonthContributionValue(orZeroDecimal(monthRecord.getMonthContributionValue()).add(amount));
                        allRecord.setContributionValue(orZeroDecimal(allRecord.getContributionValue()).add(amount));
                        monthRecord.setContributionPoint(orZero(monthRecord.getContributionPoint()) + 15);
                        allRecord.setContributionPoint(orZero(allRecord.getContributionPoint()) + 15);
                    }
                }
                default -> log.warn("Unknown property owner action: {}", actionType);
            }

            monthRecord.setContributionTier(calculateOwnerTier(monthRecord.getContributionPoint()));
            ownerMonthRepo.save(monthRecord);
            ownerAllRepo.save(allRecord);
        } catch (Exception e) {
            log.error("Error processing owner action {} for owner {}: {}", actionType, ownerId, e.getMessage());
        }
    }

    // ──────────────────────── Private helpers ────────────────────────

    private int orZero(Integer val) {
        return val != null ? val : 0;
    }

    private BigDecimal orZeroDecimal(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    private Constants.PerformanceTierEnum calculateAgentTier(Integer points) {
        if (points == null || points < 50) return Constants.PerformanceTierEnum.BRONZE;
        if (points < 150) return Constants.PerformanceTierEnum.SILVER;
        if (points < 300) return Constants.PerformanceTierEnum.GOLD;
        return Constants.PerformanceTierEnum.PLATINUM;
    }

    private Constants.CustomerTierEnum calculateCustomerTier(Integer score) {
        if (score == null || score < 20) return Constants.CustomerTierEnum.BRONZE;
        if (score < 60) return Constants.CustomerTierEnum.SILVER;
        if (score < 120) return Constants.CustomerTierEnum.GOLD;
        return Constants.CustomerTierEnum.PLATINUM;
    }

    private Constants.ContributionTierEnum calculateOwnerTier(Integer points) {
        if (points == null || points < 30) return Constants.ContributionTierEnum.BRONZE;
        if (points < 100) return Constants.ContributionTierEnum.SILVER;
        if (points < 200) return Constants.ContributionTierEnum.GOLD;
        return Constants.ContributionTierEnum.PLATINUM;
    }
}
