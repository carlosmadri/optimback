package com.airbus.optim.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorkloadFteDTO {
    Long monthsCount;
    int month;
    String exercise;
    Double kHrs;
    Double efficiency;
    Double fte;
}
