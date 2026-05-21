package com.se.bds.core.transaction.internal.application.service;

import com.se.bds.core.transaction.internal.application.port.in.ReportExportUseCase;
import com.se.bds.core.transaction.internal.application.port.out.ContractRepository;
import com.se.bds.core.transaction.internal.application.port.out.FileStoragePort;
import com.se.bds.core.transaction.internal.domain.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportExportService implements ReportExportUseCase {

    private final ContractRepository contractRepository;
    private final FileStoragePort fileStoragePort;

    @Override
    public String exportSalesReport(LocalDate startDate, LocalDate endDate, String format) {
        log.info("[ACCOUNTS] Admin initiated report export: type=SALES, format={}, dateRange={} to {}", 
                format, startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Contract> contracts = contractRepository.findContractsBySignedAtBetween(startDateTime, endDateTime);
        log.info("[EVENT] Query completed, found {} contracts for report range", contracts.size());

        byte[] fileBytes;
        String extension;

        if ("EXCEL".equalsIgnoreCase(format)) {
            fileBytes = generateExcelReport(contracts);
            extension = "xlsx";
        } else {
            fileBytes = generateCsvReport(contracts);
            extension = "csv";
        }

        String fileName = "sales_report_" + startDate + "_to_" + endDate + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;
        
        // TODO: integrate with Cloudinary for PDF/Report upload
        String downloadUrl = fileStoragePort.uploadFile(fileBytes, "reports", fileName);
        log.info("[EVENT] Sales report generated and uploaded successfully: url={}", downloadUrl);

        return downloadUrl;
    }

    private byte[] generateCsvReport(List<Contract> contracts) {
        StringBuilder csv = new StringBuilder();
        // Exclude customer personal info like names, phone, email, bank details for PII security
        csv.append("Contract ID,Property ID,Customer ID,Agent ID,Contract Type,Status,Signed At,Start Date,End Date,Financial Amount,Commission Amount\n");

        for (Contract contract : contracts) {
            BigDecimal amount = BigDecimal.ZERO;
            BigDecimal commission = BigDecimal.ZERO;

            if (contract instanceof RentalContract) {
                RentalContract rental = (RentalContract) contract;
                amount = rental.getMonthlyRentAmount() != null ? rental.getMonthlyRentAmount() : BigDecimal.ZERO;
                commission = rental.getCommissionAmount() != null ? rental.getCommissionAmount() : BigDecimal.ZERO;
            } else if (contract instanceof DepositContract) {
                DepositContract deposit = (DepositContract) contract;
                amount = deposit.getDepositAmount() != null ? deposit.getDepositAmount() : BigDecimal.ZERO;
                commission = BigDecimal.ZERO;
            } else if (contract instanceof PurchaseContract) {
                PurchaseContract purchase = (PurchaseContract) contract;
                amount = purchase.getPropertyValue() != null ? purchase.getPropertyValue() : BigDecimal.ZERO;
                commission = purchase.getCommissionAmount() != null ? purchase.getCommissionAmount() : BigDecimal.ZERO;
            }

            csv.append(contract.getId()).append(",")
                    .append(contract.getPropertyId()).append(",")
                    .append(contract.getCustomerId()).append(",")
                    .append(contract.getAgentId() != null ? contract.getAgentId() : "").append(",")
                    .append(contract.getContractType()).append(",")
                    .append(contract.getStatus()).append(",")
                    .append(contract.getSignedAt() != null ? contract.getSignedAt() : "").append(",")
                    .append(contract.getStartDate()).append(",")
                    .append(contract.getEndDate() != null ? contract.getEndDate() : "").append(",")
                    .append(amount).append(",")
                    .append(commission).append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] generateExcelReport(List<Contract> contracts) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Sales Report");
            
            // Create headers
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Contract ID", "Property ID", "Customer ID", "Agent ID", "Contract Type", "Status", "Signed At", "Start Date", "End Date", "Financial Amount", "Commission Amount"};
            
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Contract contract : contracts) {
                Row row = sheet.createRow(rowIdx++);
                
                BigDecimal amount = BigDecimal.ZERO;
                BigDecimal commission = BigDecimal.ZERO;

                if (contract instanceof RentalContract) {
                    RentalContract rental = (RentalContract) contract;
                    amount = rental.getMonthlyRentAmount() != null ? rental.getMonthlyRentAmount() : BigDecimal.ZERO;
                    commission = rental.getCommissionAmount() != null ? rental.getCommissionAmount() : BigDecimal.ZERO;
                } else if (contract instanceof DepositContract) {
                    DepositContract deposit = (DepositContract) contract;
                    amount = deposit.getDepositAmount() != null ? deposit.getDepositAmount() : BigDecimal.ZERO;
                    commission = BigDecimal.ZERO;
                } else if (contract instanceof PurchaseContract) {
                    PurchaseContract purchase = (PurchaseContract) contract;
                    amount = purchase.getPropertyValue() != null ? purchase.getPropertyValue() : BigDecimal.ZERO;
                    commission = purchase.getCommissionAmount() != null ? purchase.getCommissionAmount() : BigDecimal.ZERO;
                }

                row.createCell(0).setCellValue(contract.getId().toString());
                row.createCell(1).setCellValue(contract.getPropertyId().toString());
                row.createCell(2).setCellValue(contract.getCustomerId().toString());
                row.createCell(3).setCellValue(contract.getAgentId() != null ? contract.getAgentId().toString() : "");
                row.createCell(4).setCellValue(contract.getContractType().toString());
                row.createCell(5).setCellValue(contract.getStatus().toString());
                row.createCell(6).setCellValue(contract.getSignedAt() != null ? contract.getSignedAt().toString() : "");
                row.createCell(7).setCellValue(contract.getStartDate().toString());
                row.createCell(8).setCellValue(contract.getEndDate() != null ? contract.getEndDate().toString() : "");
                row.createCell(9).setCellValue(amount.doubleValue());
                row.createCell(10).setCellValue(commission.doubleValue());
            }

            workbook.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            log.error("[EVENT] Excel report generation FAILED", e);
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }
}
