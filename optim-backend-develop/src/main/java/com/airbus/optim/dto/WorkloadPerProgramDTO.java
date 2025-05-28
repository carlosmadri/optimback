package com.airbus.optim.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorkloadPerProgramDTO {
    private String programName;
    private double programKHrsSum;
    private double programsCount;
}
