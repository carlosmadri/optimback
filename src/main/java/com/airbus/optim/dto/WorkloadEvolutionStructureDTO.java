package com.airbus.optim.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorkloadEvolutionStructureDTO {
    List<WorkloadEvolutionDTO> workloadEvolutionList;
    String lastStatus;

    private void formatExerciseValidator() {
        for(WorkloadEvolutionDTO w : workloadEvolutionList) w.mapper();
    }

    private void setLastStatus() {
        this.lastStatus = workloadEvolutionList.get(0).getStatus();
    }

    public void buildContent() {
        if(workloadEvolutionList!= null && !workloadEvolutionList.isEmpty()) {
            formatExerciseValidator();
            setLastStatus();
        }
    }
}
