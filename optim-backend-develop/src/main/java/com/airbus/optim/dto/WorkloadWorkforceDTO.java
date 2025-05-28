package com.airbus.optim.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorkloadWorkforceDTO {
    Double exerciseOP;
    Double exerciseFCII;
    Double exerciseFirstSubmission;
    //@JsonIgnore
    Double exerciseBU;
    Double exerciseQMC;
    Double exerciseT1Q;
    Float optimisticView;
    Float validationView;
    Float realisticView;
    Float hcCeiling;
    String wipValue;
    boolean fciiFirst;

    public String buildWIPwhenRejected() {
        if(exerciseQMC == null){
            wipValue = "exerciseQMC";
            exerciseQMC = exerciseBU;
        } else if(exerciseT1Q == null) {
            wipValue = "exerciseT1Q";
            exerciseT1Q = exerciseBU;
        }
        return wipValue;
    }
}
