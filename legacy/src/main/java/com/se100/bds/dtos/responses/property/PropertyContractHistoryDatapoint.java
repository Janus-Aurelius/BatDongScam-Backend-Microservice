package com.se100.bds.dtos.responses.property;

import com.se100.bds.utils.Constants;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class PropertyContractHistoryDatapoint {
    LocalDate startDate;
    LocalDate endDate;
    Constants.ContractStatusEnum status;
}
