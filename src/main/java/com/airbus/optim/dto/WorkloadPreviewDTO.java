package com.airbus.optim.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorkloadPreviewDTO {
    String exercise;
    double kHrs;
    double fTE;
    double previsionkHrs;
    double previsionFte;

    public WorkloadPreviewDTO(String exercise, double kHrs, double fTE) {
        this.exercise = exercise;
        this.kHrs = kHrs;
        this.fTE = fTE;
    }
}
