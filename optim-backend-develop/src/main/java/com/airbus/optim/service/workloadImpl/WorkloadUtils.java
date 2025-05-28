package com.airbus.optim.service.workloadImpl;

import com.airbus.optim.dto.EvolutionDTO;
import com.airbus.optim.dto.WorkloadFteDTO;
import com.airbus.optim.dto.WorkloadFteKhrsDTO;
import com.airbus.optim.dto.WorkloadMonthlyDistributionDTO;
import com.airbus.optim.entity.Siglum;
import com.airbus.optim.entity.Workload;
import com.airbus.optim.repository.WorkloadEvolutionRepository;
import com.airbus.optim.repository.WorkloadRepository;
import com.airbus.optim.utils.Constants;
import com.airbus.optim.utils.Utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkloadUtils {
    @Autowired
    WorkloadRepository workloadRepository;

    @Autowired
    WorkloadEvolutionRepository workloadEvolutionRepository;

    @Autowired
    private Utils utils;

    public double getBottomup(List<Workload> workloads) {
        return workloads.stream()
                .filter(workload -> (Constants.WORKLOAD_STATUS_BOTTOM_UP.equalsIgnoreCase(workload.getExercise())))
                .mapToDouble(Workload::getKHrs)
                .sum();
    }

    public EvolutionDTO dbBuildWorkloadEvolutionDTO(List<Siglum> siglumList, String exercise) {
        return new EvolutionDTO(
                null,
                exercise, "", "",
                workloadRepository.getWorkloadEvolutionLastExerciseApproved(siglumList, exercise, Constants.WORKLOAD_STATUS_OWN, Constants.WORKLOAD_STATUS_DIRECT),
                workloadRepository.getWorkloadEvolutionLastExerciseApproved(siglumList, exercise, Constants.WORKLOAD_STATUS_OWN, Constants.WORKLOAD_STATUS_INDIRECT),
                workloadRepository.getWorkloadEvolutionLastExerciseApproved(siglumList, exercise, Constants.WORKLOAD_STATUS_SUB, Constants.WORKLOAD_STATUS_DIRECT),
                workloadRepository.getWorkloadEvolutionLastExerciseApproved(siglumList, exercise, Constants.WORKLOAD_STATUS_SUB, Constants.WORKLOAD_STATUS_INDIRECT)
        );
    }

    public List<EvolutionDTO> formatExerciseValidator(List<EvolutionDTO> workloadEvolutionList) {
        for(EvolutionDTO w : workloadEvolutionList) w.mapper();
        return workloadEvolutionList;
    }

    public List<Double> fillMontlyDistribution(List<WorkloadMonthlyDistributionDTO> workloadMonthlyDistributionDTOList) {
        Double [] fteList = {0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};
        for (WorkloadMonthlyDistributionDTO w :workloadMonthlyDistributionDTOList) {
            fteList[w.getMes()-1] = w.getFte();
        }

        return Arrays.asList(fteList);
    }

    public WorkloadFteKhrsDTO fteFromWorkload(List<WorkloadFteDTO> workloadFteDTOList) {
        // FTEs = Hrs / ( Efficiency x (duración de la actividad/ 12)
        WorkloadFteKhrsDTO workloadFteKhrsDTO = new WorkloadFteKhrsDTO(0.0,0.0);

        double kHrs = 0.0;
        double fTEs = 0.0;

        for (WorkloadFteDTO w : workloadFteDTOList) {
            fTEs += w.getKHrs() * 1000/ (w.getEfficiency());
            kHrs += w.getKHrs();
        }

        return new WorkloadFteKhrsDTO(kHrs,fTEs);
    }

    public WorkloadFteKhrsDTO fteFromWorkload(List<Workload> workloadList, List<WorkloadFteDTO> workloadFteDTOList, int yearFilter) {
        WorkloadFteKhrsDTO workloadFteKhrsDTO = new WorkloadFteKhrsDTO(0.0, 0.0);

        double kHrs = 0.0;
        double fTEs = 0.0;

        for (Workload employee : workloadList) {
            LocalDate startDate = convertInstantToLocalDate(employee.getStartDate());
            LocalDate endDate = convertInstantToLocalDate(employee.getEndDate());
            long totalMonths = ChronoUnit.MONTHS.between(startDate, endDate) + 1;

            long monthsInYear = 0;
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusMonths(1)) {
                if (date.getYear() == yearFilter) {
                    monthsInYear++;
                }
            }

            double kHrsPerMonth = employee.getKHrs() / totalMonths;
            double proportionalKHrs = kHrsPerMonth * monthsInYear;

            kHrs += proportionalKHrs;
        }

        for (WorkloadFteDTO w : workloadFteDTOList) {
            fTEs += w.getKHrs() * 1000 / w.getEfficiency();
        }

        return new WorkloadFteKhrsDTO(kHrs, fTEs);
    }

    private LocalDate convertInstantToLocalDate(Instant instant) {
        LocalDate localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        return localDate;
    }

    public List<Workload> getLastExercise(String statusExercise, String userSelected, List<Workload> workloadList) {
        List<String> latestExercises = Collections.singletonList(statusExercise);
        List<Siglum> siglumFiltered = utils.getVisibleSiglums(null, userSelected);

        if (Constants.WORKLOAD_EVOLUTION_STATUS_CLOSED.equals(statusExercise)) {
            latestExercises = getLastClosedExercise(siglumFiltered);
        } else if (!Constants.WORKLOAD_STATUS_BOTTOM_UP.equals(statusExercise)) {
            latestExercises = getLastWorkInProgresExercise(siglumFiltered);
        }

        List<Workload> workloadsLast = workloadRepository.findWorkloadsByExerciseAndSiglums(latestExercises, siglumFiltered, workloadList);

        workloadsLast = workloadsLast.stream()
                .filter(workload -> "Direct".equalsIgnoreCase(workload.getDirect()))
                .collect(Collectors.toList());

        if (Constants.WORKLOAD_STATUS_BOTTOM_UP.equals(statusExercise)) {
            workloadsLast = workloadsLast.stream()
                    .filter(workload -> Boolean.TRUE.equals(workload.getGo()))
                    .collect(Collectors.toList());
        }

        return workloadsLast;
    }

    public List<String> getLastClosedExercise(List<Siglum> siglumFiltered) {
        return new LinkedList<>(workloadEvolutionRepository.findLastClosedExercise());
    }

    public List<Workload> getLastExerciseByYear(String statusExercise, String userSelected, List<Workload> workloadList, int yearFilter) {
        List<String> latestExercises = List.of(Constants.WORKLOAD_STATUS_BOTTOM_UP);
        List<Siglum> siglumFiltered = utils.getVisibleSiglums(null, userSelected);
        latestExercises = (Constants.WORKLOAD_EVOLUTION_STATUS_OPENED.equals(statusExercise) ?
                getLastClosedExerciseByYear(siglumFiltered, yearFilter) : getLastWorkInProgresExercise(siglumFiltered));
        List<Workload> workloadsLast =
                workloadRepository.findWorkloadsByExerciseAndSiglums(latestExercises, siglumFiltered, workloadList);
        return workloadsLast;
    }

    public List<String> getLastClosedExerciseByYear(List<Siglum> siglumFiltered, int yearFilter) {
        return new LinkedList<>(workloadEvolutionRepository.findLastClosedExerciseByYear(siglumFiltered, yearFilter));
    }

    public List<String> getLastWorkInProgresExercise(List<Siglum> siglumFiltered) {
        // <> 'closed' and MAX(submit_date)
        return new LinkedList<>(workloadEvolutionRepository.findLastNoClosedExercise(siglumFiltered));
    }

    public List<Long> getIdsFromWorkloadList(List<Workload> workloadList) {
        List<Long> wIds = new ArrayList<>();
        for (Workload w : workloadList) {
            wIds.add(w.getId());
        }
        return wIds;
    }

    public double filterWorkloadByRatio(List<Workload> wL, String ratio) {
        Instant now = Instant.now();
        return wL.stream()
                .filter(workload -> Constants.WORKLOAD_STATUS_DIRECT.equalsIgnoreCase(workload.getDirect()))
                .filter(workload -> {
                    boolean isBeforeOrEqual = workload.getEndDate().isBefore(now) || workload.getEndDate().equals(now);
                    return isBeforeOrEqual;
                })
                .mapToDouble(Workload::getKHrs)
                .sum();
    }

}
