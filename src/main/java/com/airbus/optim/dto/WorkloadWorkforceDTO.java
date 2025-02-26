package com.airbus.optim.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorkloadWorkforceDTO {
    double exerciseOP;
    double exerciseFCII;
    double exerciseBU;
    double exerciseQMC;
    double exerciseT1Q;
    Float optimisticView;
    Float validationView;
    Float realisticView;
    Float hcCeiling;
}
