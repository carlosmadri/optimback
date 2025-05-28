package com.airbus.optim.dto;

import com.airbus.optim.utils.Constants;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EvolutionDTO {
    @JsonIgnore
    private Long id;
    private String exercise;
    @JsonIgnore
    private String exerciseName;
    private String status;
    private double kHrsOwnDirect;
    private double kHrsOwnIndirect;
    private double kHrsSubDirect;
    private double kHrsSubIndirect;

    public EvolutionDTO(String status) {
        this.status = status;
    }

    public void mapper() {
        switch (status) {
            case Constants.WORKLOAD_EVOLUTION_STATUS_OPENED:
                this.exercise = Constants.WORKLOAD_STATUS_BOTTOM_UP;
                break;
            case Constants.WORKLOAD_EVOLUTION_STATUS_BU_SUBMIT:
                this.exercise = Constants.WORKLOAD_STATUS_BOTTOM_UP_SUBMISSION;
                break;
            case Constants.WORKLOAD_EVOLUTION_STATUS_QMC_APPROVED:
                this.exercise = Constants.WORKLOAD_STATUS_QMC;
                this.status = Constants.STATUS_APPROVED;
                break;
            case Constants.WORKLOAD_EVOLUTION_STATUS_QMC_REJECTED:
                this.exercise = Constants.WORKLOAD_STATUS_WORK_IN_PROGRES;
                this.status = Constants.STATUS_REJECTED;
                this.exerciseName = Constants.WORKLOAD_STATUS_QMC;
                break;
            case Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_APPROVED:
                this.exercise = Constants.WORKLOAD_STATUS_HOT1Q;
                this.status = Constants.STATUS_APPROVED;
                break;
            case Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_REJECTED:
                this.exercise = Constants.WORKLOAD_STATUS_WORK_IN_PROGRES;
                this.status = Constants.STATUS_REJECTED;
                this.exerciseName = Constants.WORKLOAD_STATUS_HOT1Q;
                break;
            default:
                break;
        }
    }

}