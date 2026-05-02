package com.se100.bds.services.domains.report;

import com.se100.bds.dtos.responses.statisticreport.*;
import com.se100.bds.dtos.responses.admindashboard.*;
import com.se100.bds.models.schemas.report.AgentSalaryItem;
import com.se100.bds.models.schemas.report.FinancialReport;

import java.util.List;

public interface ReportService {
    AgentPerformanceStats getAgentPerformanceStats(int year);
    CustomerStats getCustomerStats(int year);
    PropertyOwnerStats getPropertyOwnerStats(int year);
    FinancialStats getFinancialStats(int year);
    PropertyStats getPropertyStats(int year);
    ViolationReportStats getViolationStats(int year);
    List<AgentSalaryItem> getSalaryByMonthYear(int month, int year);

    /// ADMIN DASHBOARD
    DashboardTopStats getDashboardTopStats();
    DashboardRevenueAndContracts getDashboardRevenueAndContracts(int year);
    DashboardTotalProperties getDashboardTotalProperties(int year);
    DashboardPropertyDistribution getDashboardPropertyDistribution(int year);
    DashboardAgentRanking getDashboardAgentRanking(int month, int year);
    DashboardCustomerRanking getDashboardCustomerRanking(int month, int year);
}
