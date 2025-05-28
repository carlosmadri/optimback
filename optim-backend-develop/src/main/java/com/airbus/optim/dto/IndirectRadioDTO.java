package com.airbus.optim.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IndirectRadioDTO {
    private double direct;
    private double indirect;
}
