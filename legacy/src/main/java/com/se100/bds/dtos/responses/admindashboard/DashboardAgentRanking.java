package com.se100.bds.dtos.responses.admindashboard;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class DashboardAgentRanking {
    List<AgentItem> agents;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class AgentItem {
        private String firstName;
        private String lastName;
        private Integer rank;
        private String tier;
        private Double rating;
        private Integer totalAppointmentsCompleted;
        private Integer totalContractsSigned;
    }
}
