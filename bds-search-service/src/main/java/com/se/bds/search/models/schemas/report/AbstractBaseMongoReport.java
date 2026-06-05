package com.se.bds.search.models.schemas.report;

import com.se.bds.search.models.schemas.AbstractBaseMongoSchema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AbstractBaseMongoReport extends AbstractBaseMongoSchema {
    @Field("base_report_data")
    private BaseReportData baseReportData;
}
