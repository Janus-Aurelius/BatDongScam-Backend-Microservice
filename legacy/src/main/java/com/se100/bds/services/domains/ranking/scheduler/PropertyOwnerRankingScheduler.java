package com.se100.bds.services.domains.ranking.scheduler;

import com.se100.bds.models.entities.user.User;
import com.se100.bds.models.schemas.ranking.IndividualPropertyOwnerContributionAll;
import com.se100.bds.models.schemas.ranking.IndividualPropertyOwnerContributionMonth;
import com.se100.bds.repositories.domains.mongo.ranking.IndividualPropertyOwnerContributionAllRepository;
import com.se100.bds.repositories.domains.mongo.ranking.IndividualPropertyOwnerContributionMonthRepository;
import com.se100.bds.services.domains.user.UserService;
import com.se100.bds.utils.Constants;
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

import com.se100.bds.services.domains.ranking.utils.RankingUtil;

@Service
@Slf4j
@RequiredArgsConstructor
public class PropertyOwnerRankingScheduler {
    private final IndividualPropertyOwnerContributionMonthRepository individualPropertyOwnerContributionMonthRepository;
    private final IndividualPropertyOwnerContributionAllRepository individualPropertyOwnerContributionAllRepository;
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

        List<IndividualPropertyOwnerContributionMonth> propertyOwnerContributionMonthList = individualPropertyOwnerContributionMonthRepository.findAll();
        for (IndividualPropertyOwnerContributionMonth propertyOwnerContributionMonth : propertyOwnerContributionMonthList) {
            updatePointMonth(propertyOwnerContributionMonth);
        }

        individualPropertyOwnerContributionMonthRepository.saveAll(propertyOwnerContributionMonthList);
    }

    private void calculateRankingAll() {
        List<IndividualPropertyOwnerContributionAll> propertyOwnerContributionAllList = individualPropertyOwnerContributionAllRepository.findAll();
        for (IndividualPropertyOwnerContributionAll propertyOwnerContributionAll : propertyOwnerContributionAllList) {
            updatePointAll(propertyOwnerContributionAll);
        }

        individualPropertyOwnerContributionAllRepository.saveAll(propertyOwnerContributionAllList);
    }

    private void updatePointMonth(IndividualPropertyOwnerContributionMonth individualPropertyOwnerContributionMonth) {
        // Get previous month data for ExtraPoint
        LocalDateTime previousMonthTime = RankingUtil.getPreviousMonth(
                individualPropertyOwnerContributionMonth.getMonth(),
                individualPropertyOwnerContributionMonth.getYear()
        );
        IndividualPropertyOwnerContributionMonth previousMonthData = individualPropertyOwnerContributionMonthRepository.findByOwnerIdAndMonthAndYear(
                individualPropertyOwnerContributionMonth.getOwnerId(),
                previousMonthTime.getMonthValue(), previousMonthTime.getYear()
        );
        int extraPoint = 0;
        if (previousMonthData != null) {
            extraPoint = RankingUtil.getExtraPoint(previousMonthData.getContributionTier().getValue());
        }

        // Calculate avg_transaction_benchmark and avg_revenue_benchmark
        List<IndividualPropertyOwnerContributionMonth> allMonthData = individualPropertyOwnerContributionMonthRepository.findAll().stream()
                .filter(m -> m.getMonth().equals(individualPropertyOwnerContributionMonth.getMonth()) && m.getYear().equals(individualPropertyOwnerContributionMonth.getYear()))
                .toList();

        int totalTransactions = allMonthData.stream()
                .mapToInt(m -> m.getMonthTotalPropertiesSold() + (m.getMonthTotalForRents() != null ? m.getMonthTotalForRents() : 0))
                .sum();
        int totalOwners = allMonthData.size();
        double avgTransactionBenchmark = totalOwners > 0 ? (double) totalTransactions / totalOwners : 0;

        BigDecimal totalRevenue = allMonthData.stream()
                .map(IndividualPropertyOwnerContributionMonth::getMonthContributionValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgRevenueBenchmark = totalOwners > 0 ? totalRevenue.divide(BigDecimal.valueOf(totalOwners), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        // Calculate scores
        double propertyUtilizationScore = individualPropertyOwnerContributionMonth.getMonthTotalProperties() > 0 ?
                ((double) (individualPropertyOwnerContributionMonth.getMonthTotalPropertiesSold() + individualPropertyOwnerContributionMonth.getMonthTotalForRents()) /
                individualPropertyOwnerContributionMonth.getMonthTotalProperties() * 100) : 0;

        double transactionSuccessScore = avgTransactionBenchmark > 0 ?
                ((double) (individualPropertyOwnerContributionMonth.getMonthTotalPropertiesSold() + individualPropertyOwnerContributionMonth.getMonthTotalForRents()) /
                avgTransactionBenchmark * 100) : 0;

        BigDecimal revenueScore = avgRevenueBenchmark.compareTo(BigDecimal.ZERO) > 0 ?
                individualPropertyOwnerContributionMonth.getMonthContributionValue().divide(avgRevenueBenchmark, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;
        revenueScore = revenueScore.min(BigDecimal.valueOf(150));

        // Calculate contribution_point
        double contributionPointDouble = (0.4 * propertyUtilizationScore) + (0.3 * transactionSuccessScore) + (0.3 * revenueScore.doubleValue()) + extraPoint;
        int contributionPoint = (int) contributionPointDouble;

        // Set contribution_point and tier
        individualPropertyOwnerContributionMonth.setContributionPoint(contributionPoint);
        Constants.ContributionTierEnum newTier = Constants.ContributionTierEnum.get(RankingUtil.getCustomerTier(contributionPoint));
        individualPropertyOwnerContributionMonth.setContributionTier(newTier);
    }

    private void updatePointAll(IndividualPropertyOwnerContributionAll individualPropertyOwnerContributionAll) {
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();

        IndividualPropertyOwnerContributionMonth currentMonthData = individualPropertyOwnerContributionMonthRepository.findByOwnerIdAndMonthAndYear(
                individualPropertyOwnerContributionAll.getOwnerId(),
                month, year
        );

        if (currentMonthData != null) {
            individualPropertyOwnerContributionAll.setContributionPoint(
                    individualPropertyOwnerContributionAll.getContributionPoint() +
                            currentMonthData.getContributionPoint());
            individualPropertyOwnerContributionAll.setContributionValue(
                    individualPropertyOwnerContributionAll.getContributionValue().add(
                            currentMonthData.getMonthContributionValue()
                    )
            );
            individualPropertyOwnerContributionAll.setTotalProperties(
                    individualPropertyOwnerContributionAll.getTotalProperties() +
                            currentMonthData.getMonthTotalProperties()
            );
            individualPropertyOwnerContributionAll.setTotalPropertiesSold(
                    individualPropertyOwnerContributionAll.getTotalPropertiesSold() +
                            currentMonthData.getMonthTotalForSales()
            );
            individualPropertyOwnerContributionAll.setTotalPropertiesRented(
                    individualPropertyOwnerContributionAll.getTotalPropertiesRented() +
                            currentMonthData.getMonthTotalForRents()
            );
        }
    }

    private void calculateRankingPosition() {
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();

        // Calculate ranking for current month
        try {
            List<IndividualPropertyOwnerContributionMonth> propertyOwnerContributionMonthList = individualPropertyOwnerContributionMonthRepository.findAll().stream()
                    .filter(m -> m.getMonth().equals(month) && m.getYear().equals(year))
                    .collect(Collectors.toList());

            // Sort by contributionPoint descending
            propertyOwnerContributionMonthList.sort((p1, p2) -> p2.getContributionPoint().compareTo(p1.getContributionPoint()));

            // Update ranking with handling for same points
            int ranking = 1;
            Integer previousPoint = null;
            int currentPosition = 1;

            for (IndividualPropertyOwnerContributionMonth propertyOwnerContribution : propertyOwnerContributionMonthList) {
                if (previousPoint != null && !previousPoint.equals(propertyOwnerContribution.getContributionPoint())) {
                    // Different point, update ranking to current position
                    ranking = currentPosition;
                }
                propertyOwnerContribution.setRankingPosition(ranking);
                previousPoint = propertyOwnerContribution.getContributionPoint();
                currentPosition++;
            }

            // Save all
            individualPropertyOwnerContributionMonthRepository.saveAll(propertyOwnerContributionMonthList);
        } catch (Exception e) {
            log.error("calculateRankingPosition for month - {}", e.getMessage());
        }

        // Calculate ranking for all time
        try {
            List<IndividualPropertyOwnerContributionAll> propertyOwnerContributionAllList = individualPropertyOwnerContributionAllRepository.findAll();

            // Sort by contributionPoint descending
            propertyOwnerContributionAllList.sort((p1, p2) -> p2.getContributionPoint().compareTo(p1.getContributionPoint()));

            // Update ranking with handling for same points
            int ranking = 1;
            Integer previousPoint = null;
            int currentPosition = 1;

            for (IndividualPropertyOwnerContributionAll propertyOwnerContribution : propertyOwnerContributionAllList) {
                if (previousPoint != null && !previousPoint.equals(propertyOwnerContribution.getContributionPoint())) {
                    // Different point, update ranking to current position
                    ranking = currentPosition;
                }
                propertyOwnerContribution.setRankingPosition(ranking);
                previousPoint = propertyOwnerContribution.getContributionPoint();
                currentPosition++;
            }

            // Save all
            individualPropertyOwnerContributionAllRepository.saveAll(propertyOwnerContributionAllList);
        } catch (Exception e) {
            log.error("calculateRankingPosition for all - {}", e.getMessage());
        }
    }

    private void createIfNotExist() {
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();

        List<User> availablePropertyOwners = userService.findAllByRoleAndStillAvailable(Constants.RoleEnum.PROPERTY_OWNER);


        for (User availablePropertyOwner : availablePropertyOwners) {

            // Check if data exists for all history tracking
            if (individualPropertyOwnerContributionAllRepository.findByOwnerId(
                    availablePropertyOwner.getId()
            ) == null) {
                individualPropertyOwnerContributionAllRepository.save(
                    IndividualPropertyOwnerContributionAll.builder()
                            .ownerId(availablePropertyOwner.getId())
                            .contributionPoint(0)
                            .rankingPosition(0)
                            .contributionValue(BigDecimal.ZERO)
                            .totalProperties(0)
                            .totalPropertiesSold(0)
                            .totalPropertiesRented(0)
                            .build()
                );
            }

            // Check if data exists for current month tracking
            if (individualPropertyOwnerContributionMonthRepository.findByOwnerIdAndMonthAndYear(
                    availablePropertyOwner.getId(),
                    month, year
            ) == null) {
                individualPropertyOwnerContributionMonthRepository.save(
                        IndividualPropertyOwnerContributionMonth.builder()
                                .ownerId(availablePropertyOwner.getId())
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
                                .build()
                );
            }
        }
    }
}
