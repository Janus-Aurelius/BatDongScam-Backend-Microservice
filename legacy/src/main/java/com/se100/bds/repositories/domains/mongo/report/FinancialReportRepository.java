package com.se100.bds.repositories.domains.mongo.report;

import com.se100.bds.models.schemas.report.FinancialReport;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FinancialReportRepository extends MongoRepository<FinancialReport, String> {
    FinancialReport findByBaseReportData_MonthAndBaseReportData_Year(Integer baseReportDataMonth, Integer baseReportDataYear);

    List<FinancialReport> findAllByBaseReportData_Year(Integer baseReportDataYear);

    FinancialReport getFinancialReportByBaseReportData_MonthAndBaseReportData_Year(Integer baseReportDataMonth, Integer baseReportDataYear);
}

