package com.airbus.optim.service;

import com.airbus.optim.dto.IndirectRadioDTO;
import com.airbus.optim.dto.OwnRatioDTO;
import com.airbus.optim.dto.TeamOutlookDTO;
import com.airbus.optim.dto.WorkloadEvolutionDTO;
import com.airbus.optim.dto.WorkloadEvolutionStructureDTO;
import com.airbus.optim.dto.WorkloadPerProgramDTO;
import com.airbus.optim.dto.WorkloadPreviewDTO;
import com.airbus.optim.dto.WorkloadWorkforceDTO;
import com.airbus.optim.entity.CostCenter;
import com.airbus.optim.entity.PPSID;
import com.airbus.optim.entity.Siglum;
import com.airbus.optim.entity.Workload;
import com.airbus.optim.repository.CostCenterRepository;
import com.airbus.optim.repository.EmployeeRepository;
import com.airbus.optim.repository.HeadCountRepository;
import com.airbus.optim.repository.JobRequestRepository;
import com.airbus.optim.repository.LeverRepository;
import com.airbus.optim.repository.PPSIDRepository;
import com.airbus.optim.repository.SiglumRepository;
import com.airbus.optim.repository.WorkloadRepository;
import com.airbus.optim.utils.Constants;
import com.airbus.optim.utils.Utils;
import com.airbus.optim.utils.WorkloadUtils;
import jakarta.persistence.EntityNotFoundException;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

@Service
public class WorkloadService {

    @Autowired
    private WorkloadRepository workloadRepository;

    @Autowired
    private LeverRepository leverRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeSpecification employeeSpecification;

    @Autowired
    private JobRequestRepository jobRequestRepository;

    @Autowired
    private SiglumRepository siglumRepository;

    @Autowired
    private CostCenterRepository costCenterRepository;

    @Autowired
    private PPSIDRepository ppsidRepository;

    @Autowired
    private HeadCountRepository headCountRepository;

    @Autowired
    private Utils utils;

    @Autowired
    WorkloadUtils workloadUtils;

    public Workload createWorkload(Workload newWorkload) {
        handleSiglum(newWorkload);
        handleCostCenter(newWorkload);
        handlePpsidCreationOrUpdate(newWorkload);

        return workloadRepository.save(newWorkload);
    }

    public Workload updateWorkload(Long id, Workload workloadDetails) {
        if (id == null) {
            throw new EntityNotFoundException("Workload ID cannot be null.");
        }

        Workload existingWorkload = workloadRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Workload not found with ID: " + id));

        updateWorkloadFields(existingWorkload, workloadDetails);
        handleSiglum(existingWorkload, workloadDetails);
        handleCostCenter(existingWorkload, workloadDetails);
        handlePpsidCreationOrUpdate(existingWorkload, workloadDetails);

        return workloadRepository.save(existingWorkload);
    }

    @Transactional
    public List<Workload> updateWorkloads(List<Workload> workloads) {
        if (workloads == null || workloads.isEmpty()) {
            throw new IllegalArgumentException("La lista no puede estar vacía.");
        }

        return workloads.stream()
                .map(workload -> {
                    if (workload.getId() == null) {
                        throw new EntityNotFoundException("Workload ID no puede ser nulo.");
                    }

                    Workload existingWorkload = workloadRepository.findById(workload.getId())
                            .orElseThrow(() -> new EntityNotFoundException("No se ha encontrando un workload con el id: " + workload.getId()));

                    updateWorkloadFields(existingWorkload, workload);
                    handleSiglum(existingWorkload, workload);
                    handleCostCenter(existingWorkload, workload);
                    handlePpsidCreationOrUpdate(existingWorkload, workload);

                    return workloadRepository.save(existingWorkload);
                })
                .collect(Collectors.toList());
    }

    private void handleSiglum(Workload workload) {
        Siglum siglum = workload.getSiglum();
        if (siglum != null && siglum.getId() != null) {
            workload.setSiglum(
                    siglumRepository.findById(siglum.getId())
                            .orElseThrow(() -> new EntityNotFoundException("No se ha encontrado el siglum con el id: " + siglum.getId()))
            );
        } else {
            workload.setSiglum(null);
        }
    }

    private void handleCostCenter(Workload workload) {
        CostCenter costCenter = workload.getCostCenter();
        if (costCenter != null && costCenter.getId() != null) {
            workload.setCostCenter(
                    costCenterRepository.findById(costCenter.getId())
                            .orElseThrow(() -> new EntityNotFoundException("No se ha encontrado el cost center con el id: " + costCenter.getId()))
            );
        } else {
            workload.setCostCenter(null);
        }
    }

    private void handleCostCenter(Workload existingWorkload, Workload updatedWorkload) {
        CostCenter costCenter = updatedWorkload.getCostCenter();
        if (costCenter != null) {
            existingWorkload.setCostCenter(
                    costCenter.getId() != null
                            ? costCenterRepository.findById(costCenter.getId())
                            .orElseThrow(() -> new EntityNotFoundException("No se ha encontrado el cost center con el id: " + costCenter.getId()))
                            : null
            );
        }
    }

    private void handlePpsidCreationOrUpdate(Workload workload) {
        PPSID ppsid = workload.getPpsid();
        if (ppsid != null) {
            PPSID savedPpsid = ppsid.getId() != null
                    ? ppsidRepository.findById(ppsid.getId())
                    .orElseGet(() -> ppsidRepository.save(ppsid))
                    : ppsidRepository.save(ppsid);
            workload.setPpsid(savedPpsid);
        }
    }

    private void handleSiglum(Workload existingWorkload, Workload updatedWorkload) {
        Siglum siglum = updatedWorkload.getSiglum();
        if (siglum != null) {
            existingWorkload.setSiglum(
                    siglum.getId() != null
                            ? siglumRepository.findById(siglum.getId())
                            .orElseThrow(() -> new EntityNotFoundException("No se ha encontrado el siglum con el id: " + siglum.getId()))
                            : null
            );
        }
    }

    private void handlePpsidCreationOrUpdate(Workload existingWorkload, Workload updatedWorkload) {
        PPSID ppsid = updatedWorkload.getPpsid();
        if (ppsid != null) {
            PPSID savedPpsid = ppsid.getId() != null
                    ? ppsidRepository.findById(ppsid.getId())
                    .orElseGet(() -> ppsidRepository.save(ppsid))
                    : ppsidRepository.save(ppsid);
            existingWorkload.setPpsid(savedPpsid);
        }
    }


    private void updateWorkloadFields(Workload existingWorkload, Workload workloadDetails) {
        if (workloadDetails.getDirect() != null) {
            existingWorkload.setDirect(workloadDetails.getDirect());
        }
        if (workloadDetails.getCollar() != null) {
            existingWorkload.setCollar(workloadDetails.getCollar());
        }
        if (workloadDetails.getOwn() != null) {
            existingWorkload.setOwn(workloadDetails.getOwn());
        }
        if (workloadDetails.getCore() != null) {
            existingWorkload.setCore(workloadDetails.getCore());
        }
        if (workloadDetails.getScenario() != null) {
            existingWorkload.setScenario(workloadDetails.getScenario());
        }
        if (workloadDetails.getGo() != null) {
            existingWorkload.setGo(workloadDetails.getGo());
        }
        if (workloadDetails.getDescription() != null) {
            existingWorkload.setDescription(workloadDetails.getDescription());
        }
        if (workloadDetails.getExercise() != null) {
            existingWorkload.setExercise(workloadDetails.getExercise());
        }
        if (workloadDetails.getStartDate() != null) {
            existingWorkload.setStartDate(workloadDetails.getStartDate());
        }
        if (workloadDetails.getEndDate() != null) {
            existingWorkload.setEndDate(workloadDetails.getEndDate());
        }
        if (workloadDetails.getKHrs() != null) {
            existingWorkload.setKHrs(workloadDetails.getKHrs());
        }
        if (workloadDetails.getFTE() != null) {
            existingWorkload.setFTE(workloadDetails.getFTE());
        }
        if (workloadDetails.getKEur() != null) {
            existingWorkload.setKEur(workloadDetails.getKEur());
        }
        if (workloadDetails.getEac() != null) {
            existingWorkload.setEac(workloadDetails.getEac());
        }
    }

    public IndirectRadioDTO getIndirectRatio(List<Siglum> siglumList, int yearFilter) {
        List<Workload> workloads = workloadRepository.getIndirectRatio(siglumList, yearFilter);
        String exercise = utils.getExerciseName();

        if(workloadUtils.getBotomup(workloads) != 0)
            exercise = Constants.WORKLOAD_STATUS_BOTOM_UP;

        return new IndirectRadioDTO(
                workloadUtils.getIndirectRatioKhrs(workloads, exercise, Constants.WORKLOAD_STATUS_DIRECT),
                workloadUtils.getIndirectRatioKhrs(workloads, exercise, Constants.WORKLOAD_STATUS_INDIRECT));
    }

    public OwnRatioDTO getOwnRatio(List<Siglum> siglumList, int yearFilter) {
        List<Workload> workloads = workloadRepository.getOwnRatio(siglumList, yearFilter);
        String exercise = utils.getExerciseName();

        if(workloadUtils.getBotomup(workloads) != 0)
            exercise = Constants.WORKLOAD_STATUS_BOTOM_UP;

        return new OwnRatioDTO(
                workloadUtils.getOwnRatioKhrs(workloads, exercise, Constants.WORKLOAD_STATUS_OWN),
                workloadUtils.getOwnRatioKhrs(workloads, exercise, Constants.WORKLOAD_STATUS_SUB));
    }

    public List<WorkloadPerProgramDTO> getWorkloadPerProgram(List<Siglum> siglumList, int yearFilter) {
        String exercise = Constants.WORKLOAD_STATUS_BOTOM_UP;
        List<WorkloadPerProgramDTO> workloadPerProgramDTOs =
                workloadRepository.getWorkloadPerProgram(siglumList, yearFilter, exercise);

        if(!workloadPerProgramDTOs.isEmpty()){
            return workloadPerProgramDTOs;
        } else {
            exercise = utils.getExerciseName();
            return workloadRepository.getWorkloadPerProgram(siglumList, yearFilter, exercise);
        }
    }

    public WorkloadEvolutionStructureDTO getWorkloadEvolution(List<Siglum> siglumList, int yearFilter) {
        List<WorkloadEvolutionDTO> workloadEvolutionList = new ArrayList<>();
        WorkloadEvolutionStructureDTO workloadEvolutionStructureDTO = new WorkloadEvolutionStructureDTO(workloadEvolutionList, "");
        String exerciseOP = utils.getExerciseOP(yearFilter);

        workloadEvolutionList = workloadRepository.getWorkloadEvolutionExerciseEdited(siglumList, yearFilter);
        workloadEvolutionList.add(workloadUtils.dbBuildWorkloadEvolutionDTO(siglumList, exerciseOP, yearFilter));
        workloadEvolutionList.add(workloadUtils.dbBuildWorkloadEvolutionDTO(siglumList, Constants.EXERCISE_FORECAST, yearFilter));

        workloadEvolutionStructureDTO.setWorkloadEvolutionList(workloadEvolutionList);
        workloadEvolutionStructureDTO.buildContent();

        return workloadEvolutionStructureDTO;
    }

    public WorkloadWorkforceDTO getWorkloadWorkforce(MultiValueMap<String, String> params, List<Siglum> siglumList, int yearFilter) {
        ResponseEntity<TeamOutlookDTO> teamOutlookDTO = employeeService.getTeamOutlook(params, yearFilter);
        // Montly-distribution : fte de la bu o ultimo ejercicio + wip (ultima bu)
        return new WorkloadWorkforceDTO(
                workloadRepository.getWorkloadWorkforceExerciseFTE(siglumList, yearFilter, utils.getExerciseOP(yearFilter)),
                workloadRepository.getWorkloadWorkforceExerciseFTE(siglumList, yearFilter, Constants.EXERCISE_FORECAST),
                workloadRepository.getWorkloadWorkforceExerciseFTE(siglumList, yearFilter, Constants.WORKLOAD_STATUS_BOTOM_UP),
                workloadRepository.getWorkloadWorkforceExerciseEditionFTE(siglumList, yearFilter, Constants.WORKLOAD_STATUS_BOTOM_UP, Constants.WORKLOAD_STATUS_QMC_APPROVED),
                workloadRepository.getWorkloadWorkforceExerciseEditionFTE(siglumList, yearFilter, Constants.WORKLOAD_STATUS_BOTOM_UP, Constants.WORKLOAD_STATUS_HOT1Q_APPROVED),
                Objects.requireNonNull(teamOutlookDTO.getBody()).getOptimisticView(),
                teamOutlookDTO.getBody().getValidationView(),
                teamOutlookDTO.getBody().getRealisticView(),
                teamOutlookDTO.getBody().getHcCeiling()
        );
    }

    public WorkloadPreviewDTO getWorkloadPreview(List<Siglum> siglumList, int yearFilter) {

        // Se indicará el total de kHrs de la última planificación aprobada del último ejercicio financiero,
        // es decir, las kHrs del FC o de la OP más reciente.
        String exercise = utils.getExerciseName();
        WorkloadPreviewDTO workloadExerciseDTO = workloadRepository.getWorkloadPreviewExercise(siglumList, yearFilter, exercise);

        // Se indicará el total de kHrs del “Work in progress” que se esté editando en ese instante.
        // Si se encuentra fuera de la ventana de cambios, aparecerá el valor del último Bottom Up enviado a validar.
        exercise = Constants.WORKLOAD_STATUS_BOTOM_UP;
        WorkloadPreviewDTO workloadPlanificationDTO = workloadRepository.getWorkloadPreviewExercise(siglumList, yearFilter, exercise);

        return new WorkloadPreviewDTO(
                workloadExerciseDTO.getExercise(),
                workloadExerciseDTO.getFTE(),
                workloadExerciseDTO.getKHrs(),
                workloadPlanificationDTO.getFTE(),
                workloadPlanificationDTO.getKHrs());
    }
}