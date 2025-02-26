package com.airbus.optim.dto;

import com.airbus.optim.utils.Constants;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorkloadEvolutionDTO {
    private String exercise;
    @JsonIgnore
    private String status;
    private double kHrsOwnDirect;
    private double kHrsOwnIndirect;
    private double kHrsSubDirect;
    private double kHrsSubIndirect;

    public void mapper() {
        switch (status) {
            case Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT:
                this.exercise = Constants.WORKLOAD_STATUS_BOTOM_UP_SUBTISSION;
                break;
            case Constants.WORKLOAD_EVOLUTION_STATUS_QMC_APPROVED:
                this.exercise = Constants.WORKLOAD_STATUS_QMC;
                this.status = Constants.WORKLOAD_STATUS_APPROVED;
                break;
            case Constants.WORKLOAD_EVOLUTION_STATUS_QMC_REJECTED:
                this.exercise = Constants.WORKLOAD_STATUS_QMC;
                this.status = Constants.WORKLOAD_STATUS_REJECTED;
                break;
            case Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_APROVED:
                this.exercise = Constants.WORKLOAD_STATUS_HOT1Q;
                this.status = Constants.WORKLOAD_STATUS_APPROVED;
                break;
            case Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_REJECTED:
                this.exercise = Constants.WORKLOAD_STATUS_HOT1Q;
                this.status = Constants.WORKLOAD_STATUS_REJECTED;
            case Constants.WORKLOAD_EVOLUTION_STATUS_HO_SIGLUM_APROVED:
                this.exercise = Constants.WORKLOAD_STATUS_HO_SIGLUM;
                this.status = Constants.WORKLOAD_STATUS_APPROVED;
                break;
            case Constants.WORKLOAD_EVOLUTION_STATUS_HO_SIGLUM_REJECTED:
                this.exercise = Constants.WORKLOAD_STATUS_HO_SIGLUM;
                this.status = Constants.WORKLOAD_STATUS_REJECTED;
                break;
            default:
                break;
        }
    }

}
