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
public class DashboardCustomerRanking {
    List<CustomerItem> customers;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CustomerItem {
        private String firstName;
        private String lastName;
        private Integer rank;
        private String tier;
    }
}
