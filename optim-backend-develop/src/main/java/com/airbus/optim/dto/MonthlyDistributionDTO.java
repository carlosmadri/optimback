package com.airbus.optim.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class MonthlyDistributionDTO {
    private List<Double> realisticView;
    private List<Double> validationView;
    private List<Double> optimisticView;
    private List<Double> op;
    private List<Double> fcii;
    private List<Double> wip;
    private String wipValue;
    private float hcCeiling;
}