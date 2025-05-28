package com.airbus.optim.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorkloadMonthlyDistributionExerciseDTO {
    private List<Double> op;
    private List<Double> fcii;
    private List<Double> wip;
    private String wipValue;
}
