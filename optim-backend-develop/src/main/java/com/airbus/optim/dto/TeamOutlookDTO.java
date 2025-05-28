package com.airbus.optim.dto;

import lombok.Data;

@Data
public class TeamOutlookDTO {
    private Float fteActives;
    private Float fteNonActives;
    private Float leavers;
    private Float recoveries;
    private Float redeployment;
    private Float perimeterChanges;
    private Float filled;
    private Float opened;
    private Float validationProcess;
    private Float onHold;
    private Float hcCeiling;
    private Float internalMobility;
    private Float realisticView;
    private Float validationView;
    private Float optimisticView;
    private Float realisticViewAverage;
    private Float validationViewAverage;
    private Float optimisticViewAverage;
}
