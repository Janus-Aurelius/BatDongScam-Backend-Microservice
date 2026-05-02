package com.se100.bds.repositories.domains.mongo.report;

import com.se100.bds.models.schemas.report.CustomerAnalyticsReport;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerAnalyticsReportRepository extends MongoRepository<CustomerAnalyticsReport, String> {
    CustomerAnalyticsReport findByBaseReportData_MonthAndBaseReportData_Year(Integer baseReportDataMonth, Integer baseReportDataYear);

    List<CustomerAnalyticsReport> findAllByBaseReportData_Year(Integer baseReportDataYear);
}
