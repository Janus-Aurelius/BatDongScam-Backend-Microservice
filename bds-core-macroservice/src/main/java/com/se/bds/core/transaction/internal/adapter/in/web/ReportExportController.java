package com.se.bds.core.transaction.internal.adapter.in.web;

import com.se.bds.core.transaction.internal.application.port.in.ReportExportUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportExportController {

    private final ReportExportUseCase reportExportUseCase;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/sales/export")
    public ResponseEntity<String> exportSalesReport(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "format", defaultValue = "EXCEL") String format) {
        
        String downloadUrl = reportExportUseCase.exportSalesReport(startDate, endDate, format);
        return ResponseEntity.ok(downloadUrl);
    }
}
