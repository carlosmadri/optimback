package com.airbus.optim.dto;

import com.airbus.optim.utils.Constants;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkloadEvolutionStructureDTO {
    List<EvolutionDTO> workloadEvolutionList;
    String lastStatus;
    String wipValue;
    boolean multipleSiglums;

    private void formatExerciseValidator() {
        for(EvolutionDTO w : workloadEvolutionList) w.mapper();
    }

    private void cleanWorkloadEvolutionList() {
        for (int i=0; i<workloadEvolutionList.size(); i++) {
            if(Constants.WORKLOAD_EVOLUTION_STATUS_BU_SUBMIT.equals(workloadEvolutionList.get(i).getStatus())) {
                if((workloadEvolutionList.size()>i) && Constants.WORKLOAD_EVOLUTION_STATUS_OPENED.equals(workloadEvolutionList.get(i+1).getStatus())) {
                    workloadEvolutionList.remove(i+1);
                }
            }
        }
    }

    private void setLastStatus() {
        this.lastStatus = workloadEvolutionList.get(0).getStatus();
    }

    private void setWipValue() {
        this.wipValue = workloadEvolutionList.get(0).getExerciseName();
    }

    public void buildContent() {
        if(workloadEvolutionList!= null && !workloadEvolutionList.isEmpty()) {
            formatExerciseValidator();
            cleanWorkloadEvolutionList();
            setLastStatus();
            setWipValue();
        }
    }
}
