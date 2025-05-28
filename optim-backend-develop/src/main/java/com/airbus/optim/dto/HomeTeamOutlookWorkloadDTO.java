package com.airbus.optim.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HomeTeamOutlookWorkloadDTO {
    double current;
    String lastExerciseName;
    double lastExerciseKhrs;
    private Float hcCeilingFormerRefference;
}
