package com.airbus.optim.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class BorrowedLeasedDTO {
    private Float averageBorrowed;
    private Float averageLeased;
    private Float netDifference;
    private List<Float> borrowedMonthlyDistribution;
    private List<Float> leasedMonthlyDistribution;
}
