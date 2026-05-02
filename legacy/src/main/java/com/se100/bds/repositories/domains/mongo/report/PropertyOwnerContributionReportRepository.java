package com.se100.bds.repositories.domains.mongo.report;

import com.se100.bds.models.schemas.report.PropertyOwnerContributionReport;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertyOwnerContributionReportRepository extends MongoRepository<PropertyOwnerContributionReport, String> {
    PropertyOwnerContributionReport findByBaseReportData_MonthAndBaseReportData_Year(Integer baseReportDataMonth, Integer baseReportDataYear);

    List<PropertyOwnerContributionReport> findAllByBaseReportData_Year(Integer baseReportDataYear);
}
