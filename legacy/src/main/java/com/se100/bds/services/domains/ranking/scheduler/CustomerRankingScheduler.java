package com.se100.bds.services.domains.ranking.scheduler;

import com.se100.bds.models.entities.user.User;
import com.se100.bds.models.schemas.ranking.IndividualCustomerPotentialAll;
import com.se100.bds.models.schemas.ranking.IndividualCustomerPotentialMonth;
import com.se100.bds.repositories.domains.mongo.ranking.IndividualCustomerPotentialAllRepository;
import com.se100.bds.repositories.domains.mongo.ranking.IndividualCustomerPotentialMonthRepository;
import com.se100.bds.services.domains.user.UserService;
import com.se100.bds.utils.Constants;
import com.se100.bds.services.domains.ranking.utils.RankingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerRankingScheduler {
    private final IndividualCustomerPotentialMonthRepository individualCustomerPotentialMonthRepository;
    private final IndividualCustomerPotentialAllRepository individualCustomerPotentialAllRepository;
    private final UserService userService;

    // Run every day at 00:00 AM (midnight)
     @Scheduled(cron = "0 0 0 * * ?")
     protected void calculateRanking() {
        createIfNotExist();

        calculateRankingMonth();
        calculateRankingAll();
        calculateRankingPosition();
    }
    
    private void calculateRankingMonth() {

        List<IndividualCustomerPotentialMonth> customerPerformanceMonthList = individualCustomerPotentialMonthRepository.findAll();
        for (IndividualCustomerPotentialMonth customerPerformanceMonth : customerPerformanceMonthList) {
            updatePointMonth(customerPerformanceMonth);
        }

        individualCustomerPotentialMonthRepository.saveAll(customerPerformanceMonthList);
    }

    private void calculateRankingAll() {
        List<IndividualCustomerPotentialAll> customerPerformanceCareerList = individualCustomerPotentialAllRepository.findAll();
        for (IndividualCustomerPotentialAll customerPerformanceCareer : customerPerformanceCareerList) {
            updatePointAll(customerPerformanceCareer);
        }

        individualCustomerPotentialAllRepository.saveAll(customerPerformanceCareerList);
    }
    
    private void updatePointMonth(IndividualCustomerPotentialMonth individualCustomerPotentialMonth) {
        // Get previous month data for ExtraPoint
        LocalDateTime previousMonthTime = RankingUtil.getPreviousMonth(
                individualCustomerPotentialMonth.getMonth(),
                individualCustomerPotentialMonth.getYear()
        );
        IndividualCustomerPotentialMonth previousMonthData = individualCustomerPotentialMonthRepository.findByCustomerIdAndMonthAndYear(
                individualCustomerPotentialMonth.getCustomerId(),
                previousMonthTime.getMonthValue(), previousMonthTime.getYear()
        );
        int extraPoint = 0;
        if (previousMonthData != null) {
            extraPoint = RankingUtil.getExtraPoint(previousMonthData.getCustomerTier().getValue());
        }

        // Calculate avg_spending_benchmark
        List<IndividualCustomerPotentialMonth> allMonthData = individualCustomerPotentialMonthRepository.findAll().stream()
                .filter(m -> m.getMonth().equals(individualCustomerPotentialMonth.getMonth()) && m.getYear().equals(individualCustomerPotentialMonth.getYear()))
                .toList();
        BigDecimal totalSpending = allMonthData.stream()
                .map(IndividualCustomerPotentialMonth::getMonthSpending)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalCustomers = allMonthData.size();
        BigDecimal avgSpendingBenchmark = totalCustomers > 0 ? totalSpending.divide(BigDecimal.valueOf(totalCustomers), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        // Calculate scores
        int leadScore = calculateLeadScore(individualCustomerPotentialMonth, avgSpendingBenchmark, extraPoint);

        // Set lead_score and tier
        individualCustomerPotentialMonth.setLeadScore(leadScore);
        Constants.CustomerTierEnum newTier = Constants.CustomerTierEnum.get(RankingUtil.getCustomerTier(leadScore));
        individualCustomerPotentialMonth.setCustomerTier(newTier);
    }

    private static int calculateLeadScore(IndividualCustomerPotentialMonth individualCustomerPotentialMonth, BigDecimal avgSpendingBenchmark, int extraPoint) {
        int viewingAttendedScore = individualCustomerPotentialMonth.getMonthViewingsRequested() > 0 ?
                (int) ((double) individualCustomerPotentialMonth.getMonthViewingAttended() / individualCustomerPotentialMonth.getMonthViewingsRequested() * 100) : 0;

        int totalContractsSigned = individualCustomerPotentialMonth.getMonthPurchases() + individualCustomerPotentialMonth.getMonthRentals();

        int conversionScore = individualCustomerPotentialMonth.getMonthViewingAttended() > 0 ?
                (int) ((double) totalContractsSigned / individualCustomerPotentialMonth.getMonthViewingAttended() * 100) : 0;

        BigDecimal spendingScore = avgSpendingBenchmark.compareTo(BigDecimal.ZERO) > 0 ?
                individualCustomerPotentialMonth.getMonthSpending().divide(avgSpendingBenchmark, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;
        spendingScore = spendingScore.min(BigDecimal.valueOf(150));

        int contractScore = Math.min(totalContractsSigned * 25, 100);

        // Calculate lead_score
        double leadScoreDouble = (0.2 * viewingAttendedScore) + (0.2 * conversionScore) + (0.4 * spendingScore.doubleValue()) + (0.2 * contractScore) + extraPoint;
        return (int) leadScoreDouble;
    }

    private void updatePointAll(IndividualCustomerPotentialAll individualCustomerPotentialAll) {
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();

        IndividualCustomerPotentialMonth currentMonthData = individualCustomerPotentialMonthRepository.findByCustomerIdAndMonthAndYear(
                individualCustomerPotentialAll.getCustomerId(),
                month, year
        );

        if (currentMonthData != null) {
            individualCustomerPotentialAll.setLeadScore(
                    individualCustomerPotentialAll.getLeadScore() +
                    currentMonthData.getLeadScore());
            individualCustomerPotentialAll.setViewingsRequested(
                    individualCustomerPotentialAll.getViewingsRequested() +
                    currentMonthData.getMonthViewingsRequested()
            );
            individualCustomerPotentialAll.setViewingsAttended(
                    individualCustomerPotentialAll.getViewingsAttended() +
                    currentMonthData.getMonthViewingAttended()
            );
            individualCustomerPotentialAll.setSpending(
                    individualCustomerPotentialAll.getSpending().add(
                            currentMonthData.getMonthSpending()
                    )
            );
            individualCustomerPotentialAll.setTotalPurchases(
                    individualCustomerPotentialAll.getTotalPurchases() +
                    currentMonthData.getMonthPurchases()
            );
            individualCustomerPotentialAll.setTotalRentals(
                    individualCustomerPotentialAll.getTotalRentals() +
                    currentMonthData.getMonthRentals()
            );
            individualCustomerPotentialAll.setTotalContractsSigned(
                    individualCustomerPotentialAll.getTotalContractsSigned() +
                    currentMonthData.getMonthPurchases() + currentMonthData.getMonthRentals() // assuming contracts signed = purchases + rentals
            );
        }
    }
    
    private void calculateRankingPosition() {
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();

        // Calculate ranking for current month
        try {
            List<IndividualCustomerPotentialMonth> customerPotentialMonthList = individualCustomerPotentialMonthRepository.findAll().stream()
                    .filter(m -> m.getMonth().equals(month) && m.getYear().equals(year))
                    .collect(Collectors.toList());

            // Sort by leadScore descending
            customerPotentialMonthList.sort((c1, c2) -> c2.getLeadScore().compareTo(c1.getLeadScore()));

            // Update ranking with handling for same points
            int ranking = 1;
            Integer previousPoint = null;
            int currentPosition = 1;

            for (IndividualCustomerPotentialMonth customerPotential : customerPotentialMonthList) {
                if (previousPoint != null && !previousPoint.equals(customerPotential.getLeadScore())) {
                    // Different point, update ranking to current position
                    ranking = currentPosition;
                }
                customerPotential.setLeadPosition(ranking);
                previousPoint = customerPotential.getLeadScore();
                currentPosition++;
            }

            // Save all
            individualCustomerPotentialMonthRepository.saveAll(customerPotentialMonthList);
        } catch (Exception e) {
            log.error("calculateRankingPosition for month - {}", e.getMessage());
        }

        // Calculate ranking for all time
        try {
            List<IndividualCustomerPotentialAll> customerPotentialAllList = individualCustomerPotentialAllRepository.findAll();

            // Sort by leadScore descending
            customerPotentialAllList.sort((c1, c2) -> c2.getLeadScore().compareTo(c1.getLeadScore()));

            // Update ranking with handling for same points
            int ranking = 1;
            Integer previousPoint = null;
            int currentPosition = 1;

            for (IndividualCustomerPotentialAll customerPotential : customerPotentialAllList) {
                if (previousPoint != null && !previousPoint.equals(customerPotential.getLeadScore())) {
                    // Different point, update ranking to current position
                    ranking = currentPosition;
                }
                customerPotential.setLeadPosition(ranking);
                previousPoint = customerPotential.getLeadScore();
                currentPosition++;
            }

            // Save all
            individualCustomerPotentialAllRepository.saveAll(customerPotentialAllList);
        } catch (Exception e) {
            log.error("calculateRankingPosition for all - {}", e.getMessage());
        }
    }

    private void createIfNotExist() {
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();

        List<User> availableCustomers = userService.findAllByRoleAndStillAvailable(Constants.RoleEnum.CUSTOMER);


        for (User availableCustomer : availableCustomers) {

            // Check if data exists for all history tracking
            if (individualCustomerPotentialAllRepository.findByCustomerId(
                    availableCustomer.getId()
            ) == null) {
                individualCustomerPotentialAllRepository.save(
                    IndividualCustomerPotentialAll.builder()
                            .customerId(availableCustomer.getId())
                            .leadScore(0)
                            .leadPosition(0)
                            .viewingsRequested(0)
                            .viewingsAttended(0)
                            .spending(BigDecimal.ZERO)
                            .totalPurchases(0)
                            .totalRentals(0)
                            .totalContractsSigned(0)
                            .build()
                );
            }

            // Check if data exists for current month tracking
            if (individualCustomerPotentialMonthRepository.findByCustomerIdAndMonthAndYear(
                    availableCustomer.getId(),
                    month, year
            ) == null) {
                individualCustomerPotentialMonthRepository.save(
                        IndividualCustomerPotentialMonth.builder()
                                .customerId(availableCustomer.getId())
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
                                .build()
                );
            }
        }
    }
}
