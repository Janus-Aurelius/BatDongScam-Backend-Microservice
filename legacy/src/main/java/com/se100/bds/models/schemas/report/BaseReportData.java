package com.se100.bds.models.schemas.report;

import com.se100.bds.utils.Constants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BaseReportData {
    private Constants.ReportTypeEnum reportType;
    private Integer month;
    private Integer year;
    private String title;
    private String description;
}
