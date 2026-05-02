package com.se100.bds.services.domains.ranking.scheduler;

import com.se100.bds.models.entities.user.User;
import com.se100.bds.models.schemas.ranking.IndividualSalesAgentPerformanceCareer;
import com.se100.bds.models.schemas.ranking.IndividualSalesAgentPerformanceMonth;
import com.se100.bds.repositories.domains.mongo.ranking.IndividualSalesAgentPerformanceCareerRepository;
import com.se100.bds.repositories.domains.mongo.ranking.IndividualSalesAgentPerformanceMonthRepository;
import com.se100.bds.services.domains.ranking.utils.RankingUtil;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class SaleAgentRankingScheduler {
    private final IndividualSalesAgentPerformanceMonthRepository individualSalesAgentPerformanceMonthRepository;
    private final IndividualSalesAgentPerformanceCareerRepository individualSalesAgentPerformanceCareerRepository;
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
        int conversionScore = (individualSalesAgentPerformanceMonth.getMonthContracts() / individualSalesAgentPerformanceMonth.getMonthAppointmentsCompleted()) * 100;
        int completionScore = (individualSalesAgentPerformanceMonth.getMonthAppointmentsCompleted() / individualSalesAgentPerformanceMonth.getMonthAppointmentsAssigned()) * 100;
        int satisfactionScore = individualSalesAgentPerformanceMonth.getMonthCustomerSatisfactionAvg().intValue();

        LocalDateTime previousMonthTime = RankingUtil.getPreviousMonth(
                individualSalesAgentPerformanceMonth.getMonth(),
                individualSalesAgentPerformanceMonth.getYear()
        );
        IndividualSalesAgentPerformanceMonth previousMonthDataRanking = individualSalesAgentPerformanceMonthRepository.findByAgentIdAndMonthAndYear(
                individualSalesAgentPerformanceMonth.getAgentId(),
                previousMonthTime.getMonthValue(), previousMonthTime.getYear()
        );
        int extraPoint = 0;
        if (previousMonthDataRanking != null) {
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
        );

        if (currentMonthData != null) {
            individualSalesAgentPerformanceCareer.setPerformancePoint(
                    individualSalesAgentPerformanceCareer.getPerformancePoint() +
                    currentMonthData.getPerformancePoint());
            individualSalesAgentPerformanceCareer.setPropertiesAssigned(
                    individualSalesAgentPerformanceCareer.getPropertiesAssigned() +
                    currentMonthData.getMonthPropertiesAssigned()
            );
            individualSalesAgentPerformanceCareer.setAppointmentAssigned(
                    individualSalesAgentPerformanceCareer.getAppointmentAssigned() +
                    currentMonthData.getMonthAppointmentsAssigned()
            );
            individualSalesAgentPerformanceCareer.setAppointmentCompleted(
                    individualSalesAgentPerformanceCareer.getAppointmentCompleted() +
                    currentMonthData.getMonthAppointmentsCompleted()
            );
            individualSalesAgentPerformanceCareer.setTotalContracts(
                    individualSalesAgentPerformanceCareer.getTotalContracts() +
                    currentMonthData.getMonthContracts()
            );
            individualSalesAgentPerformanceCareer.setTotalRates(
                    individualSalesAgentPerformanceCareer.getTotalRates() +
                    currentMonthData.getMonthRates()
            );

            // Calculate average rating and customer satisfaction
            Long monthCount = individualSalesAgentPerformanceMonthRepository.countByAgentId(individualSalesAgentPerformanceCareer.getAgentId());

            if (monthCount > 0) {
                BigDecimal newAvgRating = (individualSalesAgentPerformanceCareer.getAvgRating()
                        .multiply(BigDecimal.valueOf(monthCount - 1))
                        .add(currentMonthData.getAvgRating()))
                        .divide(BigDecimal.valueOf(monthCount), 2, RoundingMode.HALF_UP);
                individualSalesAgentPerformanceCareer.setAvgRating(newAvgRating);

                BigDecimal newCustomerSatisfactionAvg = (individualSalesAgentPerformanceCareer.getCustomerSatisfactionAvg()
                        .multiply(BigDecimal.valueOf(monthCount - 1))
                        .add(currentMonthData.getMonthCustomerSatisfactionAvg()))
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
                    .filter(m -> m.getMonth().equals(month) && m.getYear().equals(year)).sorted((a1, a2) -> a2.getPerformancePoint().compareTo(a1.getPerformancePoint())).collect(Collectors.toList());

            // Sort by performancePoint descending

            // Update ranking with handling for same points
            int ranking = 1;
            Integer previousPoint = null;
            int currentPosition = 1;

            for (IndividualSalesAgentPerformanceMonth agentPerformance : agentPerformanceMonthList) {
                if (previousPoint != null && !previousPoint.equals(agentPerformance.getPerformancePoint())) {
                    // Different point, update ranking to current position
                    ranking = currentPosition;
                }
                agentPerformance.setRankingPosition(ranking);
                previousPoint = agentPerformance.getPerformancePoint();
                currentPosition++;
            }

            // Save all
            individualSalesAgentPerformanceMonthRepository.saveAll(agentPerformanceMonthList);
        } catch (Exception e) {
            log.error("calculateRankingPosition for month - {}", e.getMessage());
        }

        // Calculate ranking for all time
        try {
            List<IndividualSalesAgentPerformanceCareer> agentPerformanceCareerList = individualSalesAgentPerformanceCareerRepository.findAll();

            // Sort by performancePoint descending
            agentPerformanceCareerList.sort((a1, a2) -> a2.getPerformancePoint().compareTo(a1.getPerformancePoint()));

            // Update ranking with handling for same points
            int ranking = 1;
            Integer previousPoint = null;
            int currentPosition = 1;

            for (IndividualSalesAgentPerformanceCareer agentPerformance : agentPerformanceCareerList) {
                if (previousPoint != null && !previousPoint.equals(agentPerformance.getPerformancePoint())) {
                    // Different point, update ranking to current position
                    ranking = currentPosition;
                }
                agentPerformance.setCareerRanking(ranking);
                previousPoint = agentPerformance.getPerformancePoint();
                currentPosition++;
            }

            // Save all
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

            // Check if data exists for all history tracking
            if (individualSalesAgentPerformanceCareerRepository.findByAgentId(
                    availableAgent.getId()
            ) != null) {
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

            // Check if data exists for current month tracking
            if (individualSalesAgentPerformanceMonthRepository.findByAgentIdAndMonthAndYear(
                    availableAgent.getId(),
                    month, year
            ) != null) {
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
