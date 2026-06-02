package com.se100.bds.searchservice.models.schemas.report;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BaseReportData {
    private String reportType;
    private Integer month;
    private Integer year;
    private String title;
    private String description;
}
