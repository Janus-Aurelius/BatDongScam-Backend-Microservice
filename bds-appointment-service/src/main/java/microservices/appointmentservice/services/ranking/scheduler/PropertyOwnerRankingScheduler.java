package microservices.appointmentservice.services.ranking.scheduler;

import microservices.appointmentservice.entities.user.User;
import microservices.appointmentservice.schemas.ranking.IndividualPropertyOwnerContributionAll;
import microservices.appointmentservice.schemas.ranking.IndividualPropertyOwnerContributionMonth;
import microservices.appointmentservice.repositories.ranking.IndividualPropertyOwnerContributionAllRepository;
import microservices.appointmentservice.repositories.ranking.IndividualPropertyOwnerContributionMonthRepository;
import microservices.appointmentservice.services.user.UserService;
import microservices.appointmentservice.utils.Constants;
import microservices.appointmentservice.services.ranking.utils.RankingUtil;
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
public class PropertyOwnerRankingScheduler {
    private final IndividualPropertyOwnerContributionMonthRepository individualPropertyOwnerContributionMonthRepository;
    private final IndividualPropertyOwnerContributionAllRepository individualPropertyOwnerContributionAllRepository;
    private final UserService userService;

    // Run every day at 00:00 AM (midnight)
    @Scheduled(cron = "0 0 0 * * ?")
    public void calculateRanking() {
        log.info("[PropertyOwnerRankingScheduler] Starting daily owner ranking calculation...");
        try {
            createIfNotExist();
            calculateRankingMonth();
            calculateRankingAll();
            calculateRankingPosition();
            log.info("[PropertyOwnerRankingScheduler] Daily owner ranking calculation completed successfully.");
        } catch (Exception e) {
            log.error("[PropertyOwnerRankingScheduler] Error during ranking calculation", e);
        }
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
        LocalDateTime previousMonthTime = RankingUtil.getPreviousMonth(
                individualPropertyOwnerContributionMonth.getMonth(),
                individualPropertyOwnerContributionMonth.getYear()
        );
        IndividualPropertyOwnerContributionMonth previousMonthData = individualPropertyOwnerContributionMonthRepository.findByOwnerIdAndMonthAndYear(
                individualPropertyOwnerContributionMonth.getOwnerId(),
                previousMonthTime.getMonthValue(), previousMonthTime.getYear()
        ).orElse(null);

        int extraPoint = 0;
        if (previousMonthData != null && previousMonthData.getContributionTier() != null) {
            extraPoint = RankingUtil.getExtraPoint(previousMonthData.getContributionTier().getValue());
        }

        List<IndividualPropertyOwnerContributionMonth> allMonthData = individualPropertyOwnerContributionMonthRepository.findAll().stream()
                .filter(m -> m.getMonth() != null && m.getMonth().equals(individualPropertyOwnerContributionMonth.getMonth())
                        && m.getYear() != null && m.getYear().equals(individualPropertyOwnerContributionMonth.getYear()))
                .toList();

        int totalTransactions = allMonthData.stream()
                .mapToInt(m -> (m.getMonthTotalPropertiesSold() != null ? m.getMonthTotalPropertiesSold() : 0)
                        + (m.getMonthTotalPropertiesRented() != null ? m.getMonthTotalPropertiesRented() : 0))
                .sum();
        int totalOwners = allMonthData.size();
        double avgTransactionBenchmark = totalOwners > 0 ? (double) totalTransactions / totalOwners : 0;

        BigDecimal totalRevenue = allMonthData.stream()
                .map(m -> m.getMonthContributionValue() != null ? m.getMonthContributionValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgRevenueBenchmark = totalOwners > 0 ? totalRevenue.divide(BigDecimal.valueOf(totalOwners), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        int totalProperties = individualPropertyOwnerContributionMonth.getMonthTotalProperties() != null ? individualPropertyOwnerContributionMonth.getMonthTotalProperties() : 0;
        int monthSold = individualPropertyOwnerContributionMonth.getMonthTotalPropertiesSold() != null ? individualPropertyOwnerContributionMonth.getMonthTotalPropertiesSold() : 0;
        int monthRented = individualPropertyOwnerContributionMonth.getMonthTotalPropertiesRented() != null ? individualPropertyOwnerContributionMonth.getMonthTotalPropertiesRented() : 0;
        BigDecimal revenue = individualPropertyOwnerContributionMonth.getMonthContributionValue() != null ? individualPropertyOwnerContributionMonth.getMonthContributionValue() : BigDecimal.ZERO;

        double propertyUtilizationScore = totalProperties > 0 ?
                ((double) (monthSold + monthRented) / totalProperties * 100) : 0;

        double transactionSuccessScore = avgTransactionBenchmark > 0 ?
                ((double) (monthSold + monthRented) / avgTransactionBenchmark * 100) : 0;

        BigDecimal revenueScore = avgRevenueBenchmark.compareTo(BigDecimal.ZERO) > 0 ?
                revenue.divide(avgRevenueBenchmark, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;
        revenueScore = revenueScore.min(BigDecimal.valueOf(150));

        double contributionPointDouble = (0.4 * propertyUtilizationScore) + (0.3 * transactionSuccessScore) + (0.3 * revenueScore.doubleValue()) + extraPoint;
        int contributionPoint = (int) contributionPointDouble;

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
        ).orElse(null);

        if (currentMonthData != null) {
            int point = individualPropertyOwnerContributionAll.getContributionPoint() != null ? individualPropertyOwnerContributionAll.getContributionPoint() : 0;
            int currentPoint = currentMonthData.getContributionPoint() != null ? currentMonthData.getContributionPoint() : 0;
            individualPropertyOwnerContributionAll.setContributionPoint(point + currentPoint);

            BigDecimal contributionVal = individualPropertyOwnerContributionAll.getContributionValue() != null ? individualPropertyOwnerContributionAll.getContributionValue() : BigDecimal.ZERO;
            BigDecimal currentContributionVal = currentMonthData.getMonthContributionValue() != null ? currentMonthData.getMonthContributionValue() : BigDecimal.ZERO;
            individualPropertyOwnerContributionAll.setContributionValue(contributionVal.add(currentContributionVal));

            int totalProperties = individualPropertyOwnerContributionAll.getTotalProperties() != null ? individualPropertyOwnerContributionAll.getTotalProperties() : 0;
            int currentTotalProperties = currentMonthData.getMonthTotalProperties() != null ? currentMonthData.getMonthTotalProperties() : 0;
            individualPropertyOwnerContributionAll.setTotalProperties(totalProperties + currentTotalProperties);

            int sold = individualPropertyOwnerContributionAll.getTotalPropertiesSold() != null ? individualPropertyOwnerContributionAll.getTotalPropertiesSold() : 0;
            int currentSold = currentMonthData.getMonthTotalPropertiesSold() != null ? currentMonthData.getMonthTotalPropertiesSold() : 0;
            individualPropertyOwnerContributionAll.setTotalPropertiesSold(sold + currentSold);

            int rented = individualPropertyOwnerContributionAll.getTotalPropertiesRented() != null ? individualPropertyOwnerContributionAll.getTotalPropertiesRented() : 0;
            int currentRented = currentMonthData.getMonthTotalPropertiesRented() != null ? currentMonthData.getMonthTotalPropertiesRented() : 0;
            individualPropertyOwnerContributionAll.setTotalPropertiesRented(rented + currentRented);
        }
    }

    private void calculateRankingPosition() {
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();

        // Calculate ranking for current month
        try {
            List<IndividualPropertyOwnerContributionMonth> propertyOwnerContributionMonthList = individualPropertyOwnerContributionMonthRepository.findAll().stream()
                    .filter(m -> m.getMonth() != null && m.getMonth().equals(month) && m.getYear() != null && m.getYear().equals(year))
                    .sorted((p1, p2) -> {
                        int s1 = p1.getContributionPoint() != null ? p1.getContributionPoint() : 0;
                        int s2 = p2.getContributionPoint() != null ? p2.getContributionPoint() : 0;
                        return Integer.compare(s2, s1);
                    })
                    .collect(Collectors.toList());

            int ranking = 1;
            Integer previousPoint = null;
            int currentPosition = 1;

            for (IndividualPropertyOwnerContributionMonth propertyOwnerContribution : propertyOwnerContributionMonthList) {
                int currentPoint = propertyOwnerContribution.getContributionPoint() != null ? propertyOwnerContribution.getContributionPoint() : 0;
                if (previousPoint != null && !previousPoint.equals(currentPoint)) {
                    ranking = currentPosition;
                }
                propertyOwnerContribution.setRankingPosition(ranking);
                previousPoint = currentPoint;
                currentPosition++;
            }

            individualPropertyOwnerContributionMonthRepository.saveAll(propertyOwnerContributionMonthList);
        } catch (Exception e) {
            log.error("calculateRankingPosition for month - {}", e.getMessage());
        }

        // Calculate ranking for all time
        try {
            List<IndividualPropertyOwnerContributionAll> propertyOwnerContributionAllList = individualPropertyOwnerContributionAllRepository.findAll();

            propertyOwnerContributionAllList.sort((p1, p2) -> {
                int s1 = p1.getContributionPoint() != null ? p1.getContributionPoint() : 0;
                int s2 = p2.getContributionPoint() != null ? p2.getContributionPoint() : 0;
                return Integer.compare(s2, s1);
            });

            int ranking = 1;
            Integer previousPoint = null;
            int currentPosition = 1;

            for (IndividualPropertyOwnerContributionAll propertyOwnerContribution : propertyOwnerContributionAllList) {
                int currentPoint = propertyOwnerContribution.getContributionPoint() != null ? propertyOwnerContribution.getContributionPoint() : 0;
                if (previousPoint != null && !previousPoint.equals(currentPoint)) {
                    ranking = currentPosition;
                }
                propertyOwnerContribution.setRankingPosition(ranking);
                previousPoint = currentPoint;
                currentPosition++;
            }

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
            if (availablePropertyOwner.getId() == null) continue;

            if (individualPropertyOwnerContributionAllRepository.findByOwnerId(availablePropertyOwner.getId()) == null) {
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

            if (individualPropertyOwnerContributionMonthRepository.findByOwnerIdAndMonthAndYear(availablePropertyOwner.getId(), month, year).isEmpty()) {
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
                                .monthTotalPropertiesRented(0)
                                .build()
                );
            }
        }
    }
}
