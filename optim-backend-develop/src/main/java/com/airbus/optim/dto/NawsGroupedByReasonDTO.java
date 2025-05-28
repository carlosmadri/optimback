package com.airbus.optim.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NawsGroupedByReasonDTO {
    private String availabilityReason;
    private Long employeeCount;
}
