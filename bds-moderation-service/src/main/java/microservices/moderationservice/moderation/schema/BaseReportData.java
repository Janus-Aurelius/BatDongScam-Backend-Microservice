package microservices.moderationservice.moderation.schema;

import com.se.bds.common.enums.ReportTypeEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BaseReportData {
    private ReportTypeEnum reportType;
    private Integer month;
    private Integer year;
    private String title;
    private String description;
}
