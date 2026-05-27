package com.se100.bds.models.schemas.report;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Map;

@Document(collection = "violation_report_details")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ViolationReportDetails extends AbstractBaseMongoReport {
    @Field("total_violation_reports")
    private Integer totalViolationReports;

    @Field("avg_resolution_time_hours")
    private Integer avgResolutionTimeHours;

    @Field("accounts_suspended")
    private Integer accountsSuspended;

    @Field("properties_removed")
    private Integer propertiesRemoved;

    @Field("violation_type_counts")
    private Map<String, Integer> violationTypeCounts;
}
