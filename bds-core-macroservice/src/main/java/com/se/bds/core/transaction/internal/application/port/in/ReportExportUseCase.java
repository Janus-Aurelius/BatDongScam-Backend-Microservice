package com.se.bds.core.transaction.internal.application.port.in;

import java.time.LocalDate;

/**
 * Use case for exporting sales and financial reports as CSV or Excel (US-016).
 */
public interface ReportExportUseCase {

    /**
     * Generates a sales report for the specified date range and format, uploads it to storage, and returns the download URL.
     * PII is excluded to comply with security requirements.
     *
     * @param startDate the start date of the report range
     * @param endDate the end date of the report range
     * @param format the export format (e.g. "CSV", "EXCEL")
     * @return the download URL for the generated report
     */
    String exportSalesReport(LocalDate startDate, LocalDate endDate, String format);
}
