package microservices.appointmentservice.services.ranking.scheduler;

import microservices.appointmentservice.entities.user.User;
import microservices.appointmentservice.schemas.ranking.IndividualSalesAgentPerformanceCareer;
import microservices.appointmentservice.schemas.ranking.IndividualSalesAgentPerformanceMonth;
import microservices.appointmentservice.repositories.ranking.IndividualSalesAgentPerformanceCareerRepository;
import microservices.appointmentservice.repositories.ranking.IndividualSalesAgentPerformanceMonthRepository;
import microservices.appointmentservice.services.ranking.utils.RankingUtil;
import microservices.appointmentservice.services.user.UserService;
import microservices.appointmentservice.utils.Constants;
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
public class SaleAgentRankingScheduler {

    private final IndividualSalesAgentPerformanceMonthRepository individualSalesAgentPerformanceMonthRepository;
    private final IndividualSalesAgentPerformanceCareerRepository individualSalesAgentPerformanceCareerRepository;
    private final UserService userService;

    // Run every day at 00:00 AM (midnight)
    @Scheduled(cron = "0 0 0 * * ?")
    public void calculateRanking() {
        log.info("[SaleAgentRankingScheduler] Starting daily ranking calculation...");
        try {
            createIfNotExist();
            calculateRankingMonth();
            calculateRankingAll();
            calculateRankingPosition();
            log.info("[SaleAgentRankingScheduler] Daily ranking calculation completed successfully.");
        } catch (Exception e) {
            log.error("[SaleAgentRankingScheduler] Error during ranking calculation", e);
        }
    }

    private void calculateRankingMonth() {
        List<IndividualSalesAgentPerformanceMonth> agentPerformanceMonthList = individualSalesAgentPerformanceMonthRepository.findAll();
        for (IndividualSalesAgentPerformanceMonth agentPerformanceMonth : agentPerformanceMonthList) {
            updatePointMonth(agentPerformanceMonth);
        }
        individualSalesAgentPerformanceMonthRepository.saveAll(agentPerformanceMonthList);
    }

    private void calculateRankingAll() {
        List<IndividualSalesAgentPerformanceCareer> agentPerformanceCareerList = individualSalesAgentPerformanceCareerRepository.findAll();
        for (IndividualSalesAgentPerformanceCareer agentPerformanceCareer : agentPerformanceCareerList) {
            updatePointAll(agentPerformanceCareer);
        }
        individualSalesAgentPerformanceCareerRepository.saveAll(agentPerformanceCareerList);
    }

    private void updatePointMonth(IndividualSalesAgentPerformanceMonth individualSalesAgentPerformanceMonth) {
        int monthContracts = individualSalesAgentPerformanceMonth.getMonthContracts() != null ? individualSalesAgentPerformanceMonth.getMonthContracts() : 0;
        int completed = individualSalesAgentPerformanceMonth.getMonthAppointmentsCompleted() != null ? individualSalesAgentPerformanceMonth.getMonthAppointmentsCompleted() : 0;
        int assigned = individualSalesAgentPerformanceMonth.getMonthAppointmentsAssigned() != null ? individualSalesAgentPerformanceMonth.getMonthAppointmentsAssigned() : 0;

        int conversionScore = completed > 0 ? (monthContracts * 100 / completed) : 0;
        int completionScore = assigned > 0 ? (completed * 100 / assigned) : 0;
        int satisfactionScore = individualSalesAgentPerformanceMonth.getMonthCustomerSatisfactionAvg() != null ?
                individualSalesAgentPerformanceMonth.getMonthCustomerSatisfactionAvg().intValue() : 0;

        LocalDateTime previousMonthTime = RankingUtil.getPreviousMonth(
                individualSalesAgentPerformanceMonth.getMonth(),
                individualSalesAgentPerformanceMonth.getYear()
        );
        IndividualSalesAgentPerformanceMonth previousMonthDataRanking = individualSalesAgentPerformanceMonthRepository.findByAgentIdAndMonthAndYear(
                individualSalesAgentPerformanceMonth.getAgentId(),
                previousMonthTime.getMonthValue(), previousMonthTime.getYear()
        ).orElse(null);

        int extraPoint = 0;
        if (previousMonthDataRanking != null && previousMonthDataRanking.getPerformanceTier() != null) {
            extraPoint = RankingUtil.getExtraPoint(previousMonthDataRanking.getPerformanceTier().getValue());
        }

        int newPerformancePoint = (int) ((
                0.4 * conversionScore +
                0.3 * completionScore +
                0.3 * satisfactionScore
        ) + extraPoint);

        Constants.PerformanceTierEnum newPerformanceTier = Constants.PerformanceTierEnum.get(
                RankingUtil.getCustomerTier(newPerformancePoint)
        );

        individualSalesAgentPerformanceMonth.setPerformancePoint(newPerformancePoint);
        individualSalesAgentPerformanceMonth.setPerformanceTier(newPerformanceTier);
    }

    private void updatePointAll(IndividualSalesAgentPerformanceCareer individualSalesAgentPerformanceCareer) {
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();

        IndividualSalesAgentPerformanceMonth currentMonthData = individualSalesAgentPerformanceMonthRepository.findByAgentIdAndMonthAndYear(
                individualSalesAgentPerformanceCareer.getAgentId(),
                month, year
        ).orElse(null);

        if (currentMonthData != null) {
            int careerPoints = individualSalesAgentPerformanceCareer.getPerformancePoint() != null ? individualSalesAgentPerformanceCareer.getPerformancePoint() : 0;
            int currentPoints = currentMonthData.getPerformancePoint() != null ? currentMonthData.getPerformancePoint() : 0;
            individualSalesAgentPerformanceCareer.setPerformancePoint(careerPoints + currentPoints);

            int careerAssignedProps = individualSalesAgentPerformanceCareer.getPropertiesAssigned() != null ? individualSalesAgentPerformanceCareer.getPropertiesAssigned() : 0;
            int monthAssignedProps = currentMonthData.getMonthPropertiesAssigned() != null ? currentMonthData.getMonthPropertiesAssigned() : 0;
            individualSalesAgentPerformanceCareer.setPropertiesAssigned(careerAssignedProps + monthAssignedProps);

            int careerAssignedAppts = individualSalesAgentPerformanceCareer.getAppointmentAssigned() != null ? individualSalesAgentPerformanceCareer.getAppointmentAssigned() : 0;
            int monthAssignedAppts = currentMonthData.getMonthAppointmentsAssigned() != null ? currentMonthData.getMonthAppointmentsAssigned() : 0;
            individualSalesAgentPerformanceCareer.setAppointmentAssigned(careerAssignedAppts + monthAssignedAppts);

            int careerCompletedAppts = individualSalesAgentPerformanceCareer.getAppointmentCompleted() != null ? individualSalesAgentPerformanceCareer.getAppointmentCompleted() : 0;
            int monthCompletedAppts = currentMonthData.getMonthAppointmentsCompleted() != null ? currentMonthData.getMonthAppointmentsCompleted() : 0;
            individualSalesAgentPerformanceCareer.setAppointmentCompleted(careerCompletedAppts + monthCompletedAppts);

            int careerContracts = individualSalesAgentPerformanceCareer.getTotalContracts() != null ? individualSalesAgentPerformanceCareer.getTotalContracts() : 0;
            int monthContracts = currentMonthData.getMonthContracts() != null ? currentMonthData.getMonthContracts() : 0;
            individualSalesAgentPerformanceCareer.setTotalContracts(careerContracts + monthContracts);

            int careerRates = individualSalesAgentPerformanceCareer.getTotalRates() != null ? individualSalesAgentPerformanceCareer.getTotalRates() : 0;
            int monthRates = currentMonthData.getMonthRates() != null ? currentMonthData.getMonthRates() : 0;
            individualSalesAgentPerformanceCareer.setTotalRates(careerRates + monthRates);

            Long monthCount = individualSalesAgentPerformanceMonthRepository.countByAgentId(individualSalesAgentPerformanceCareer.getAgentId());

            if (monthCount > 0) {
                BigDecimal currentCareerAvgRating = individualSalesAgentPerformanceCareer.getAvgRating() != null ?
                        individualSalesAgentPerformanceCareer.getAvgRating() : BigDecimal.ZERO;
                BigDecimal monthAvgRating = currentMonthData.getAvgRating() != null ?
                        currentMonthData.getAvgRating() : BigDecimal.ZERO;
                BigDecimal newAvgRating = (currentCareerAvgRating.multiply(BigDecimal.valueOf(monthCount - 1)).add(monthAvgRating))
                        .divide(BigDecimal.valueOf(monthCount), 2, RoundingMode.HALF_UP);
                individualSalesAgentPerformanceCareer.setAvgRating(newAvgRating);

                BigDecimal currentCareerSatisfaction = individualSalesAgentPerformanceCareer.getCustomerSatisfactionAvg() != null ?
                        individualSalesAgentPerformanceCareer.getCustomerSatisfactionAvg() : BigDecimal.ZERO;
                BigDecimal monthSatisfaction = currentMonthData.getMonthCustomerSatisfactionAvg() != null ?
                        currentMonthData.getMonthCustomerSatisfactionAvg() : BigDecimal.ZERO;
                BigDecimal newCustomerSatisfactionAvg = (currentCareerSatisfaction.multiply(BigDecimal.valueOf(monthCount - 1)).add(monthSatisfaction))
                        .divide(BigDecimal.valueOf(monthCount), 2, RoundingMode.HALF_UP);
                individualSalesAgentPerformanceCareer.setCustomerSatisfactionAvg(newCustomerSatisfactionAvg);
            }
        }
    }

    private void calculateRankingPosition() {
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();

        // Calculate ranking for current month
        try {
            List<IndividualSalesAgentPerformanceMonth> agentPerformanceMonthList = individualSalesAgentPerformanceMonthRepository.findAll().stream()
                    .filter(m -> m.getMonth() != null && m.getMonth().equals(month) && m.getYear() != null && m.getYear().equals(year))
                    .sorted((a1, a2) -> {
                        int p1 = a1.getPerformancePoint() != null ? a1.getPerformancePoint() : 0;
                        int p2 = a2.getPerformancePoint() != null ? a2.getPerformancePoint() : 0;
                        return Integer.compare(p2, p1);
                    })
                    .collect(Collectors.toList());

            int ranking = 1;
            Integer previousPoint = null;
            int currentPosition = 1;

            for (IndividualSalesAgentPerformanceMonth agentPerformance : agentPerformanceMonthList) {
                int currentPoint = agentPerformance.getPerformancePoint() != null ? agentPerformance.getPerformancePoint() : 0;
                if (previousPoint != null && !previousPoint.equals(currentPoint)) {
                    ranking = currentPosition;
                }
                agentPerformance.setRankingPosition(ranking);
                previousPoint = currentPoint;
                currentPosition++;
            }

            individualSalesAgentPerformanceMonthRepository.saveAll(agentPerformanceMonthList);
        } catch (Exception e) {
            log.error("calculateRankingPosition for month - {}", e.getMessage());
        }

        // Calculate ranking for all time
        try {
            List<IndividualSalesAgentPerformanceCareer> agentPerformanceCareerList = individualSalesAgentPerformanceCareerRepository.findAll();

            agentPerformanceCareerList.sort((a1, a2) -> {
                int p1 = a1.getPerformancePoint() != null ? a1.getPerformancePoint() : 0;
                int p2 = a2.getPerformancePoint() != null ? a2.getPerformancePoint() : 0;
                return Integer.compare(p2, p1);
            });

            int ranking = 1;
            Integer previousPoint = null;
            int currentPosition = 1;

            for (IndividualSalesAgentPerformanceCareer agentPerformance : agentPerformanceCareerList) {
                int currentPoint = agentPerformance.getPerformancePoint() != null ? agentPerformance.getPerformancePoint() : 0;
                if (previousPoint != null && !previousPoint.equals(currentPoint)) {
                    ranking = currentPosition;
                }
                agentPerformance.setCareerRanking(ranking);
                previousPoint = currentPoint;
                currentPosition++;
            }

            individualSalesAgentPerformanceCareerRepository.saveAll(agentPerformanceCareerList);
        } catch (Exception e) {
            log.error("calculateRankingPosition for all - {}", e.getMessage());
        }
    }

    private void createIfNotExist() {
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();

        List<User> availableAgents = userService.findAllByRoleAndStillAvailable(Constants.RoleEnum.SALESAGENT);

        for (User availableAgent : availableAgents) {
            if (availableAgent.getId() == null) continue;

            if (individualSalesAgentPerformanceCareerRepository.findByAgentId(availableAgent.getId()).isEmpty()) {
                individualSalesAgentPerformanceCareerRepository.save(
                        IndividualSalesAgentPerformanceCareer.builder()
                                .agentId(availableAgent.getId())
                                .performancePoint(0)
                                .careerRanking(0)
                                .propertiesAssigned(0)
                                .appointmentAssigned(0)
                                .appointmentCompleted(0)
                                .totalContracts(0)
                                .totalRates(0)
                                .avgRating(BigDecimal.ZERO)
                                .customerSatisfactionAvg(BigDecimal.ZERO)
                                .build()
                );
            }

            if (individualSalesAgentPerformanceMonthRepository.findByAgentIdAndMonthAndYear(availableAgent.getId(), month, year).isEmpty()) {
                individualSalesAgentPerformanceMonthRepository.save(
                        IndividualSalesAgentPerformanceMonth.builder()
                                .agentId(availableAgent.getId())
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
                                .build()
                );
            }
        }
    }
}
