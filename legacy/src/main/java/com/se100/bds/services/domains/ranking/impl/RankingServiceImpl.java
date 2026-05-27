package com.se100.bds.services.domains.ranking.impl;

import com.se100.bds.models.schemas.ranking.*;
import com.se100.bds.repositories.domains.mongo.ranking.*;
import com.se100.bds.services.domains.ranking.RankingService;
import com.se100.bds.services.domains.user.UserService;
import com.se100.bds.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {
    private final IndividualCustomerPotentialAllRepository individualCustomerPotentialAllRepository;
    private final IndividualCustomerPotentialMonthRepository  individualCustomerPotentialMonthRepository;
    private final IndividualSalesAgentPerformanceCareerRepository individualSalesAgentPerformanceCareerRepository;
    private final IndividualSalesAgentPerformanceMonthRepository individualSalesAgentPerformanceMonthRepository;
    private final IndividualPropertyOwnerContributionAllRepository individualPropertyOwnerContributionAllRepository;
    private final IndividualPropertyOwnerContributionMonthRepository individualPropertyOwnerContributionMonthRepository;
    private final UserService userService;

    @Override
    public String getTier(UUID userId, Constants.RoleEnum role, int month, int year) {
        switch(role){
            case CUSTOMER -> {
                var ranking = individualCustomerPotentialMonthRepository.findByCustomerIdAndMonthAndYear(
                        userId, month, year
                );
                return ranking != null && ranking.getCustomerTier() != null
                        ? ranking.getCustomerTier().name()
                        : null;
            }
            case SALESAGENT -> {
                var ranking = individualSalesAgentPerformanceMonthRepository.findByAgentIdAndMonthAndYear(
                        userId, month, year
                );
                return ranking != null && ranking.getPerformanceTier() != null
                        ? ranking.getPerformanceTier().name()
                        : null;
            }
            case PROPERTY_OWNER -> {
                var ranking = individualPropertyOwnerContributionMonthRepository.findByOwnerIdAndMonthAndYear(
                        userId, month, year
                );
                return ranking != null && ranking.getContributionTier() != null
                        ? ranking.getContributionTier().name()
                        : null;
            }
            default -> {
                return null;
            }
        }
    }

    @Override
    public String getCurrentTier(UUID userId, Constants.RoleEnum role) {
        int month = LocalDateTime.now().getMonthValue();
        int year = LocalDateTime.now().getYear();
        return getTier(userId, role, month, year);
    }

    @Override
    public IndividualSalesAgentPerformanceMonth getSaleAgentMonth(UUID agentId, int month, int year) {
        return individualSalesAgentPerformanceMonthRepository.findByAgentIdAndMonthAndYear(
                agentId, month, year
        );
    }

    @Override
    public IndividualSalesAgentPerformanceMonth getMySaleAgentMonth(int month, int year) {
        return getSaleAgentMonth(userService.getUserId(), month, year);
    }

    @Override
    public IndividualSalesAgentPerformanceMonth getSaleAgentCurrentMonth(UUID agentId) {
        int month = LocalDateTime.now().getMonthValue();
        int year = LocalDateTime.now().getYear();

        return getSaleAgentMonth(agentId, month, year);
    }

    @Override
    public IndividualSalesAgentPerformanceCareer getSaleAgentCareer(UUID agentId) {
        return individualSalesAgentPerformanceCareerRepository.findByAgentId(agentId);
    }

    @Override
    public IndividualSalesAgentPerformanceCareer getMySaleAgentCareer() {
        return getSaleAgentCareer(userService.getUserId());
    }

    @Override
    public IndividualCustomerPotentialMonth getCustomerMonth(UUID customerId, int month, int year) {
        return individualCustomerPotentialMonthRepository.findByCustomerIdAndMonthAndYear(
                customerId, month, year
        );
    }

    @Override
    public IndividualCustomerPotentialMonth getMyCustomerMonth(int month, int year) {
        return getCustomerMonth(userService.getUserId(), month, year);
    }

    @Override
    public IndividualCustomerPotentialMonth getCustomerCurrentMonth(UUID customerId) {
        int month = LocalDateTime.now().getMonthValue();
        int year = LocalDateTime.now().getYear();

        return getCustomerMonth(customerId, month, year);
    }

    @Override
    public IndividualCustomerPotentialAll getCustomerAll(UUID customerId) {
        return individualCustomerPotentialAllRepository.findByCustomerId(customerId);
    }

    @Override
    public IndividualCustomerPotentialAll getMyCustomerAll() {
        return getCustomerAll(userService.getUserId());
    }

    @Override
    public IndividualPropertyOwnerContributionMonth getPropertyOwnerMonth(UUID propertyOwnerId, int month, int year) {
        return individualPropertyOwnerContributionMonthRepository.findByOwnerIdAndMonthAndYear(
                propertyOwnerId, month, year
        );
    }

    @Override
    public IndividualPropertyOwnerContributionMonth getMyPropertyOwnerMonth(int month, int year) {
        return getPropertyOwnerMonth(userService.getUserId(), month, year);
    }

    @Override
    public IndividualPropertyOwnerContributionMonth getPropertyOwnerCurrentMonth(UUID propertyOwnerId) {
        int month = LocalDateTime.now().getMonthValue();
        int year = LocalDateTime.now().getYear();
        return getPropertyOwnerMonth(propertyOwnerId, month, year);
    }

    @Override
    public IndividualPropertyOwnerContributionAll getPropertyOwnerAll(UUID propertyOwnerId) {
        return individualPropertyOwnerContributionAllRepository.findByOwnerId(propertyOwnerId);
    }

    @Override
    public IndividualPropertyOwnerContributionAll getMyPropertyOwnerAll() {
        return getPropertyOwnerAll(userService.getUserId());
    }

    @Override
    @Async
    public void agentAction(UUID agentId, Constants.AgentActionEnum actionType, BigDecimal amount) {
        int month = LocalDateTime.now().getMonthValue();
        int year = LocalDateTime.now().getYear();

        IndividualSalesAgentPerformanceMonth performance =
            individualSalesAgentPerformanceMonthRepository.findByAgentIdAndMonthAndYear(agentId, month, year);

        if (performance == null) {
            // Create new record if not exists
            performance = IndividualSalesAgentPerformanceMonth.builder()
                .agentId(agentId)
                .month(month)
                .year(year)
                .monthPropertiesAssigned(0)
                .monthAppointmentsAssigned(0)
                .monthAppointmentsCompleted(0)
                .monthContracts(0)
                .monthRates(0)
                .build();
        }

        switch (actionType) {
            case PROPERTY_ASSIGNED -> {
                performance.setMonthPropertiesAssigned(
                    (performance.getMonthPropertiesAssigned() != null ? performance.getMonthPropertiesAssigned() : 0) + 1
                );
                performance.setHandlingProperties(
                    (performance.getHandlingProperties() != null ? performance.getHandlingProperties() : 0) + 1
                );
            }
            case APPOINTMENT_ASSIGNED -> {
                performance.setMonthAppointmentsAssigned(
                    (performance.getMonthAppointmentsAssigned() != null ? performance.getMonthAppointmentsAssigned() : 0) + 1
                );
                performance.setHandlingProperties(
                    (performance.getHandlingProperties() != null ? performance.getHandlingProperties() : 0) + 1
                );
            }
            case APPOINTMENT_COMPLETED ->
                performance.setMonthAppointmentsCompleted(
                    (performance.getMonthAppointmentsCompleted() != null ? performance.getMonthAppointmentsCompleted() : 0) + 1
                );
            case APPOINTMENT_CANCELLED -> {
                performance.setHandlingProperties(
                    Math.max(0, (performance.getHandlingProperties() != null ? performance.getHandlingProperties() : 0) - 1)
                );
            }
            case CONTRACT_SIGNED ->
                performance.setMonthContracts(
                    (performance.getMonthContracts() != null ? performance.getMonthContracts() : 0) + 1
                );
            case RATED -> {
                int currentRates = (performance.getMonthRates() != null ? performance.getMonthRates() : 0);
                BigDecimal currentAvgRating = (performance.getAvgRating() != null ? performance.getAvgRating() : BigDecimal.ZERO);

                // Calculate new average rating
                BigDecimal totalRating = currentAvgRating.multiply(BigDecimal.valueOf(currentRates));
                totalRating = totalRating.add(amount);
                int newRatesCount = currentRates + 1;
                BigDecimal newAvgRating = totalRating.divide(BigDecimal.valueOf(newRatesCount), 2, RoundingMode.HALF_UP);

                performance.setMonthRates(newRatesCount);
                performance.setAvgRating(newAvgRating);

                // Calculate customer satisfaction percentage (ratings > 3 stars are satisfied)
                BigDecimal currentSatisfactionAvg = (performance.getMonthCustomerSatisfactionAvg() != null
                    ? performance.getMonthCustomerSatisfactionAvg()
                    : BigDecimal.ZERO);

                int satisfiedCount = currentSatisfactionAvg.multiply(BigDecimal.valueOf(currentRates))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                    .intValue();

                if (amount.compareTo(BigDecimal.valueOf(3)) > 0) {
                    satisfiedCount++;
                }

                BigDecimal newSatisfactionAvg = BigDecimal.valueOf(satisfiedCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(newRatesCount), 2, RoundingMode.HALF_UP);

                performance.setMonthCustomerSatisfactionAvg(newSatisfactionAvg);
            }
        }

        individualSalesAgentPerformanceMonthRepository.save(performance);
    }

    @Override
    @Async
    public void customerAction(UUID customerId, Constants.CustomerActionEnum actionType, BigDecimal amount) {
        int month = LocalDateTime.now().getMonthValue();
        int year = LocalDateTime.now().getYear();

        IndividualCustomerPotentialMonth potential =
            individualCustomerPotentialMonthRepository.findByCustomerIdAndMonthAndYear(customerId, month, year);

        if (potential == null) {
            // Create new record if not exists
            potential = IndividualCustomerPotentialMonth.builder()
                .customerId(customerId)
                .month(month)
                .year(year)
                .monthViewingsRequested(0)
                .monthViewingAttended(0)
                .monthPurchases(0)
                .monthRentals(0)
                .monthContractsSigned(0)
                .build();
        }

        switch (actionType) {
            case VIEWING_REQUESTED -> {
                potential.setMonthViewingsRequested(
                    (potential.getMonthViewingsRequested() != null ? potential.getMonthViewingsRequested() : 0) + 1
                );
            }
            case VIEWING_ATTENDED -> {
                potential.setMonthViewingAttended(
                    (potential.getMonthViewingAttended() != null ? potential.getMonthViewingAttended() : 0) + 1
                );
            }
            case VIEWING_CANCELLED -> {
                potential.setMonthViewingsRequested(
                    Math.max(0, (potential.getMonthViewingsRequested() != null ? potential.getMonthViewingsRequested() : 0) - 1)
                );
            }
            case PURCHASE_MADE -> {
                potential.setMonthPurchases(
                    (potential.getMonthPurchases() != null ? potential.getMonthPurchases() : 0) + 1
                );
            }
            case SPENDING_MADE -> {
                BigDecimal currentSpending = (potential.getMonthSpending() != null ? potential.getMonthSpending() : BigDecimal.ZERO);
                potential.setMonthSpending(currentSpending.add(amount));
            }
            case RENTAL_MADE -> {
                potential.setMonthRentals(
                    (potential.getMonthRentals() != null ? potential.getMonthRentals() : 0) + 1
                );
            }
            case CONTRACT_SIGNED -> {
                potential.setMonthContractsSigned(
                    (potential.getMonthContractsSigned() != null ? potential.getMonthContractsSigned() : 0) + 1
                );
            }
        }

        individualCustomerPotentialMonthRepository.save(potential);
    }

    @Override
    @Async
    public void propertyOwnerAction(UUID ownerId, Constants.PropertyOwnerActionEnum actionType, BigDecimal amount) {
        int month = LocalDateTime.now().getMonthValue();
        int year = LocalDateTime.now().getYear();

        IndividualPropertyOwnerContributionMonth contribution =
            individualPropertyOwnerContributionMonthRepository.findByOwnerIdAndMonthAndYear(ownerId, month, year);

        if (contribution == null) {
            // Create new record if not exists
            contribution = IndividualPropertyOwnerContributionMonth.builder()
                .ownerId(ownerId)
                .month(month)
                .year(year)
                .monthTotalProperties(0)
                .monthTotalForSales(0)
                .monthTotalForRents(0)
                .monthTotalPropertiesSold(0)
                .monthTotalPropertiesRented(0)
                .build();
        }

        switch (actionType) {
            case PROPERTY_FOR_SALE_LISTED -> {
                contribution.setMonthTotalProperties(
                    (contribution.getMonthTotalProperties() != null ? contribution.getMonthTotalProperties() : 0) + 1
                );
                contribution.setMonthTotalForSales(
                    (contribution.getMonthTotalForSales() != null ? contribution.getMonthTotalForSales() : 0) + 1
                );
            }
            case PROPERTY_FOR_RENT_LISTED -> {
                contribution.setMonthTotalProperties(
                    (contribution.getMonthTotalProperties() != null ? contribution.getMonthTotalProperties() : 0) + 1
                );
                contribution.setMonthTotalForRents(
                    (contribution.getMonthTotalForRents() != null ? contribution.getMonthTotalForRents() : 0) + 1
                );
            }
            case PROPERTY_SOLD -> {
                contribution.setMonthTotalPropertiesSold(
                    (contribution.getMonthTotalPropertiesSold() != null ? contribution.getMonthTotalPropertiesSold() : 0) + 1
                );
            }
            case PROPERTY_RENTED -> {
                contribution.setMonthTotalPropertiesRented(
                    (contribution.getMonthTotalPropertiesRented() != null ? contribution.getMonthTotalPropertiesRented() : 0) + 1
                );
            }
            case MONEY_RECEIVED -> {
                BigDecimal currentContribution = (contribution.getMonthContributionValue() != null
                    ? contribution.getMonthContributionValue()
                    : BigDecimal.ZERO);
                contribution.setMonthContributionValue(currentContribution.add(amount));
            }
        }

        individualPropertyOwnerContributionMonthRepository.save(contribution);
    }
}
