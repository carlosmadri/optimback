package com.airbus.optim.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorkloadMonthlyDistributionDTO {
    int mes;
    double fte;
}
