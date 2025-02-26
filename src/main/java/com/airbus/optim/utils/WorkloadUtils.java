package com.airbus.optim.utils;

import com.airbus.optim.dto.WorkloadEvolutionDTO;
import com.airbus.optim.entity.Siglum;
import com.airbus.optim.entity.Workload;
import com.airbus.optim.repository.WorkloadRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkloadUtils {
    @Autowired
    WorkloadRepository workloadRepository;

    @Autowired
    private Utils utils;

    public double getBotomup(List<Workload> workloads) {
        return workloads.stream()
                .filter(workload -> (Constants.WORKLOAD_STATUS_BOTOM_UP.equalsIgnoreCase(workload.getExercise())))
                .mapToDouble(Workload::getKHrs)
                .sum();
    }

    public double getIndirectRatioKhrs(List<Workload> workloads, String exercise, String direct) {
        return workloads.stream()
                .filter(workload -> direct.equalsIgnoreCase(workload.getDirect()) &&
                        exercise.equalsIgnoreCase(workload.getExercise()))
                .mapToDouble(Workload::getKHrs)
                .sum();
    }

    public double getOwnRatioKhrs(List<Workload> workloads, String exercise, String own) {
        return workloads.stream()
                .filter(workload -> own.equalsIgnoreCase(workload.getOwn()) &&
                        exercise.equalsIgnoreCase(workload.getExercise()))
                .mapToDouble(Workload::getKHrs)
                .sum();
    }

    public WorkloadEvolutionDTO dbBuildWorkloadEvolutionDTO(List<Siglum> siglumList, String exercise, int yearFilter) {
        return new WorkloadEvolutionDTO(
                exercise, "",
                workloadRepository.getWorkloadEvolutionLastExerciseApproved(siglumList, exercise, Constants.WORKLOAD_STATUS_OWN, Constants.WORKLOAD_STATUS_DIRECT, yearFilter),
                workloadRepository.getWorkloadEvolutionLastExerciseApproved(siglumList, exercise, Constants.WORKLOAD_STATUS_OWN, Constants.WORKLOAD_STATUS_INDIRECT, yearFilter),
                workloadRepository.getWorkloadEvolutionLastExerciseApproved(siglumList, exercise, Constants.WORKLOAD_STATUS_SUB, Constants.WORKLOAD_STATUS_DIRECT, yearFilter),
                workloadRepository.getWorkloadEvolutionLastExerciseApproved(siglumList, exercise, Constants.WORKLOAD_STATUS_SUB, Constants.WORKLOAD_STATUS_INDIRECT, yearFilter)
        );
    }

    public List<WorkloadEvolutionDTO> formatExerciseValidator(List<WorkloadEvolutionDTO> workloadEvolutionList) {
        for(WorkloadEvolutionDTO w : workloadEvolutionList) w.mapper();
        return workloadEvolutionList;
    }

}
