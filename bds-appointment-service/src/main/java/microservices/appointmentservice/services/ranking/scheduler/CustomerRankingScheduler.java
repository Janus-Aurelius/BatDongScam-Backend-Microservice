package microservices.appointmentservice.services.ranking.scheduler;

import microservices.appointmentservice.entities.user.User;
import microservices.appointmentservice.schemas.ranking.IndividualCustomerPotentialAll;
import microservices.appointmentservice.schemas.ranking.IndividualCustomerPotentialMonth;
import microservices.appointmentservice.repositories.ranking.IndividualCustomerPotentialAllRepository;
import microservices.appointmentservice.repositories.ranking.IndividualCustomerPotentialMonthRepository;
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
public class CustomerRankingScheduler {
    private final IndividualCustomerPotentialMonthRepository individualCustomerPotentialMonthRepository;
    private final IndividualCustomerPotentialAllRepository individualCustomerPotentialAllRepository;
    private final UserService userService;

    // Run every day at 00:00 AM (midnight)
    @Scheduled(cron = "0 0 0 * * ?")
    public void calculateRanking() {
        log.info("[CustomerRankingScheduler] Starting daily customer ranking calculation...");
        try {
            createIfNotExist();
            calculateRankingMonth();
            calculateRankingAll();
            calculateRankingPosition();
            log.info("[CustomerRankingScheduler] Daily customer ranking calculation completed successfully.");
        } catch (Exception e) {
            log.error("[CustomerRankingScheduler] Error during ranking calculation", e);
        }
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
        LocalDateTime previousMonthTime = RankingUtil.getPreviousMonth(
                individualCustomerPotentialMonth.getMonth(),
                individualCustomerPotentialMonth.getYear()
        );
        IndividualCustomerPotentialMonth previousMonthData = individualCustomerPotentialMonthRepository.findByCustomerIdAndMonthAndYear(
                individualCustomerPotentialMonth.getCustomerId(),
                previousMonthTime.getMonthValue(), previousMonthTime.getYear()
        ).orElse(null);

        int extraPoint = 0;
        if (previousMonthData != null && previousMonthData.getCustomerTier() != null) {
            extraPoint = RankingUtil.getExtraPoint(previousMonthData.getCustomerTier().getValue());
        }

        List<IndividualCustomerPotentialMonth> allMonthData = individualCustomerPotentialMonthRepository.findAll().stream()
                .filter(m -> m.getMonth() != null && m.getMonth().equals(individualCustomerPotentialMonth.getMonth())
                        && m.getYear() != null && m.getYear().equals(individualCustomerPotentialMonth.getYear()))
                .toList();

        BigDecimal totalSpending = allMonthData.stream()
                .map(m -> m.getMonthSpending() != null ? m.getMonthSpending() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalCustomers = allMonthData.size();
        BigDecimal avgSpendingBenchmark = totalCustomers > 0 ? totalSpending.divide(BigDecimal.valueOf(totalCustomers), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        int leadScore = calculateLeadScore(individualCustomerPotentialMonth, avgSpendingBenchmark, extraPoint);

        individualCustomerPotentialMonth.setLeadScore(leadScore);
        Constants.CustomerTierEnum newTier = Constants.CustomerTierEnum.get(RankingUtil.getCustomerTier(leadScore));
        individualCustomerPotentialMonth.setCustomerTier(newTier);
    }

    private static int calculateLeadScore(IndividualCustomerPotentialMonth individualCustomerPotentialMonth, BigDecimal avgSpendingBenchmark, int extraPoint) {
        int viewingsRequested = individualCustomerPotentialMonth.getMonthViewingsRequested() != null ? individualCustomerPotentialMonth.getMonthViewingsRequested() : 0;
        int viewingsAttended = individualCustomerPotentialMonth.getMonthViewingAttended() != null ? individualCustomerPotentialMonth.getMonthViewingAttended() : 0;
        int purchases = individualCustomerPotentialMonth.getMonthPurchases() != null ? individualCustomerPotentialMonth.getMonthPurchases() : 0;
        int rentals = individualCustomerPotentialMonth.getMonthRentals() != null ? individualCustomerPotentialMonth.getMonthRentals() : 0;
        BigDecimal spending = individualCustomerPotentialMonth.getMonthSpending() != null ? individualCustomerPotentialMonth.getMonthSpending() : BigDecimal.ZERO;

        int viewingAttendedScore = viewingsRequested > 0 ? (int) ((double) viewingsAttended / viewingsRequested * 100) : 0;
        int totalContractsSigned = purchases + rentals;
        int conversionScore = viewingsAttended > 0 ? (int) ((double) totalContractsSigned / viewingsAttended * 100) : 0;

        BigDecimal spendingScore = avgSpendingBenchmark.compareTo(BigDecimal.ZERO) > 0 ?
                spending.divide(avgSpendingBenchmark, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;
        spendingScore = spendingScore.min(BigDecimal.valueOf(150));

        int contractScore = Math.min(totalContractsSigned * 25, 100);

        double leadScoreDouble = (0.2 * viewingAttendedScore) + (0.2 * conversionScore) + (0.4 * spendingScore.doubleValue()) + (0.2 * contractScore) + extraPoint;
        return (int) leadScoreDouble;
    }

    private void updatePointAll(IndividualCustomerPotentialAll individualCustomerPotentialAll) {
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();

        IndividualCustomerPotentialMonth currentMonthData = individualCustomerPotentialMonthRepository.findByCustomerIdAndMonthAndYear(
                individualCustomerPotentialAll.getCustomerId(),
                month, year
        ).orElse(null);

        if (currentMonthData != null) {
            int leadScore = individualCustomerPotentialAll.getLeadScore() != null ? individualCustomerPotentialAll.getLeadScore() : 0;
            int currentLeadScore = currentMonthData.getLeadScore() != null ? currentMonthData.getLeadScore() : 0;
            individualCustomerPotentialAll.setLeadScore(leadScore + currentLeadScore);

            int viewingsRequested = individualCustomerPotentialAll.getViewingsRequested() != null ? individualCustomerPotentialAll.getViewingsRequested() : 0;
            int currentViewingsRequested = currentMonthData.getMonthViewingsRequested() != null ? currentMonthData.getMonthViewingsRequested() : 0;
            individualCustomerPotentialAll.setViewingsRequested(viewingsRequested + currentViewingsRequested);

            int viewingsAttended = individualCustomerPotentialAll.getViewingsAttended() != null ? individualCustomerPotentialAll.getViewingsAttended() : 0;
            int currentViewingsAttended = currentMonthData.getMonthViewingAttended() != null ? currentMonthData.getMonthViewingAttended() : 0;
            individualCustomerPotentialAll.setViewingsAttended(viewingsAttended + currentViewingsAttended);

            BigDecimal spending = individualCustomerPotentialAll.getSpending() != null ? individualCustomerPotentialAll.getSpending() : BigDecimal.ZERO;
            BigDecimal currentSpending = currentMonthData.getMonthSpending() != null ? currentMonthData.getMonthSpending() : BigDecimal.ZERO;
            individualCustomerPotentialAll.setSpending(spending.add(currentSpending));

            int purchases = individualCustomerPotentialAll.getTotalPurchases() != null ? individualCustomerPotentialAll.getTotalPurchases() : 0;
            int currentPurchases = currentMonthData.getMonthPurchases() != null ? currentMonthData.getMonthPurchases() : 0;
            individualCustomerPotentialAll.setTotalPurchases(purchases + currentPurchases);

            int rentals = individualCustomerPotentialAll.getTotalRentals() != null ? individualCustomerPotentialAll.getTotalRentals() : 0;
            int currentRentals = currentMonthData.getMonthRentals() != null ? currentMonthData.getMonthRentals() : 0;
            individualCustomerPotentialAll.setTotalRentals(rentals + currentRentals);

            int totalContracts = individualCustomerPotentialAll.getTotalContractsSigned() != null ? individualCustomerPotentialAll.getTotalContractsSigned() : 0;
            individualCustomerPotentialAll.setTotalContractsSigned(totalContracts + currentPurchases + currentRentals);
        }
    }

    private void calculateRankingPosition() {
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();

        // Calculate ranking for current month
        try {
            List<IndividualCustomerPotentialMonth> customerPotentialMonthList = individualCustomerPotentialMonthRepository.findAll().stream()
                    .filter(m -> m.getMonth() != null && m.getMonth().equals(month) && m.getYear() != null && m.getYear().equals(year))
                    .sorted((c1, c2) -> {
                        int s1 = c1.getLeadScore() != null ? c1.getLeadScore() : 0;
                        int s2 = c2.getLeadScore() != null ? c2.getLeadScore() : 0;
                        return Integer.compare(s2, s1);
                    })
                    .collect(Collectors.toList());

            int ranking = 1;
            Integer previousPoint = null;
            int currentPosition = 1;

            for (IndividualCustomerPotentialMonth customerPotential : customerPotentialMonthList) {
                int currentPoint = customerPotential.getLeadScore() != null ? customerPotential.getLeadScore() : 0;
                if (previousPoint != null && !previousPoint.equals(currentPoint)) {
                    ranking = currentPosition;
                }
                customerPotential.setLeadPosition(ranking);
                previousPoint = currentPoint;
                currentPosition++;
            }

            individualCustomerPotentialMonthRepository.saveAll(customerPotentialMonthList);
        } catch (Exception e) {
            log.error("calculateRankingPosition for month - {}", e.getMessage());
        }

        // Calculate ranking for all time
        try {
            List<IndividualCustomerPotentialAll> customerPotentialAllList = individualCustomerPotentialAllRepository.findAll();

            customerPotentialAllList.sort((c1, c2) -> {
                int s1 = c1.getLeadScore() != null ? c1.getLeadScore() : 0;
                int s2 = c2.getLeadScore() != null ? c2.getLeadScore() : 0;
                return Integer.compare(s2, s1);
            });

            int ranking = 1;
            Integer previousPoint = null;
            int currentPosition = 1;

            for (IndividualCustomerPotentialAll customerPotential : customerPotentialAllList) {
                int currentPoint = customerPotential.getLeadScore() != null ? customerPotential.getLeadScore() : 0;
                if (previousPoint != null && !previousPoint.equals(currentPoint)) {
                    ranking = currentPosition;
                }
                customerPotential.setLeadPosition(ranking);
                previousPoint = currentPoint;
                currentPosition++;
            }

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
            if (availableCustomer.getId() == null) continue;

            if (individualCustomerPotentialAllRepository.findByCustomerId(availableCustomer.getId()) == null) {
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

            if (individualCustomerPotentialMonthRepository.findByCustomerIdAndMonthAndYear(availableCustomer.getId(), month, year).isEmpty()) {
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
                                .monthContractsSigned(0)
                                .build()
                );
            }
        }
    }
}
