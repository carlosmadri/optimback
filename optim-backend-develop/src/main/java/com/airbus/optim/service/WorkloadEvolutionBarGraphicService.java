package com.airbus.optim.service;

import com.airbus.optim.dto.EvolutionDTO;
import com.airbus.optim.dto.WorkloadEvolutionStructureDTO;
import com.airbus.optim.entity.Siglum;
import com.airbus.optim.entity.User;
import com.airbus.optim.entity.Workload;
import com.airbus.optim.entity.WorkloadEvolution;
import com.airbus.optim.repository.WorkloadEvolutionRepository;
import com.airbus.optim.repository.WorkloadRepository;
import com.airbus.optim.utils.Constants;
import com.airbus.optim.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkloadEvolutionBarGraphicService {

    @Autowired
    private WorkloadRepository workloadRepository;

    @Autowired
    private WorkloadEvolutionRepository workloadEvolutionRepository;

    @Autowired
    private WorkloadSpecification workloadSpecification;

    @Autowired
    private WorkloadEvolutionSpecification workloadEvolutionSpecification;

    @Autowired
    private Utils utils;

    public WorkloadEvolutionStructureDTO getWorkloadEvolution(
            MultiValueMap<String, String> params,
            String userSelected,
            int yearFilter,
            String validationStatus) {
        Specification<WorkloadEvolution> specWorkloadEvolution = workloadEvolutionSpecification.getSpecifications(params);
        List<WorkloadEvolution> workloadEvolutionList = workloadEvolutionRepository.findAll(specWorkloadEvolution);
        Specification<Workload> specWorkload = workloadSpecification.getSpecifications(params);
        List<Workload> workloadList = workloadRepository.findAll(specWorkload);
        List<Siglum> siglumFiltered = utils.getVisibleSiglums(null, userSelected);
        List<String> selectedSiglumHRs = Optional.ofNullable(params.get("siglum.siglumHR"))
                .orElse(null);
        List<Siglum> siglumsSelected = Optional.ofNullable(selectedSiglumHRs)
                .map(siglumHRs -> siglumFiltered.stream()
                        .filter(siglum -> siglumHRs.contains(siglum.getSiglumHR()))
                        .collect(Collectors.toList()))
                .orElse(siglumFiltered);
        boolean isMultiSiglum = calculateMultipleSiglums(siglumFiltered, workloadList);

        List<EvolutionDTO> barDTOList = new LinkedList<>();
        processWorkloads(barDTOList, workloadEvolutionRepository.findLatestClosedExercisesByType(), siglumFiltered, workloadList, true);
        //OP and FC should be displayed in order from oldest to newest.
        orderByLowId(barDTOList);
        processLatestExerciseWorkloads(barDTOList, validationStatus, workloadEvolutionList, siglumFiltered, isMultiSiglum);
        processWorkloads(barDTOList, List.of(Constants.WORKLOAD_STATUS_BOTTOM_UP), siglumFiltered, workloadList,false);

        String lastStatus = calculateStatus(barDTOList, calculateMoreThanOneSiglum(workloadEvolutionList, workloadList, siglumsSelected), siglumsSelected);
        String wip = calculateWIP(barDTOList, calculateMoreThanOneSiglum(workloadEvolutionList, workloadList, siglumFiltered), siglumFiltered);
        checkObjectsToReturn(barDTOList, lastStatus);
        if(!isMultiSiglum) setFirstSubmissionValues(barDTOList, userSelected);
        WorkloadEvolutionStructureDTO workloadEvolutionStructureDTO = new WorkloadEvolutionStructureDTO();
        workloadEvolutionStructureDTO.setWorkloadEvolutionList(barDTOList);
        workloadEvolutionStructureDTO.setLastStatus(lastStatus);
        workloadEvolutionStructureDTO.setWipValue(wip);
        workloadEvolutionStructureDTO.setMultipleSiglums(calculateMultipleSiglums(siglumFiltered, workloadList));
        return workloadEvolutionStructureDTO;
    }


    private void setFirstSubmissionValues(List<EvolutionDTO> barDTOList, String userSelected) {
        List<EvolutionDTO> filteredItems = barDTOList.stream()
                .filter(item -> Constants.WORKLOAD_STATUS_BOTTOM_UP_SUBMISSION.equals(item.getExercise()))
                .collect(Collectors.toList());
        if (filteredItems.isEmpty()) {
            return; // No hay elementos que procesar
        }
        String latestExercise = utils.getLastOpenedExerciseOrThrow().getExercise();
        User user = utils.getUserInSession(userSelected);
        workloadEvolutionRepository
                .findFirstBySiglumAndExerciseWithOlderSubmitDate(user.getSiglum(), latestExercise)
                .ifPresent(workloadEvolution -> {
                    // Asignar los valores obtenidos a cada elemento filtrado
                    filteredItems.forEach(item -> {
                        item.setKHrsOwnDirect(workloadEvolution.getKHrsOwnDirect());
                        item.setKHrsSubDirect(workloadEvolution.getKHrsSubDirect());
                        item.setKHrsOwnIndirect(workloadEvolution.getKHrsOwnIndirect());
                        item.setKHrsSubIndirect(workloadEvolution.getKHrsSubIndirect());
                    });
                });
    }

    public void processWorkloads(
            List<EvolutionDTO> barDTOList,
            List<String> exercises,
            List<Siglum> siglumFiltered,
            List<Workload> workloads,
            boolean addDefaultIfEmpty) {

        List<Workload> workloadsFiltered = new ArrayList<>();
        if (!(exercises.size() == 1 && exercises.get(0).equalsIgnoreCase(Constants.WORKLOAD_STATUS_BOTTOM_UP))) {
            workloadsFiltered = workloadRepository.findWorkloadsByExerciseAndSiglums(exercises, siglumFiltered, workloads);
            if (addDefaultIfEmpty ||  workloadsFiltered.size() < 2) {
                addDefaultEvolutionDTOs(barDTOList, exercises);
            }
        } else {
            try {
                String latestExercise = utils.getLastOpenedExerciseOrThrow().getExercise();
                if (siglumFiltered.size() == 1) {
                    Optional<WorkloadEvolution> latest = workloadEvolutionRepository
                            .findTopBySiglumAndExerciseOrderBySubmitDateDesc(siglumFiltered.get(0), latestExercise);
                    if (latest.isPresent()) {
                        String latestStatus = latest.get().getStatus();
                        if (!latestStatus.equalsIgnoreCase(Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_APPROVED)) {
                            workloadsFiltered = workloadRepository.findWorkloadsByExerciseAndSiglums(exercises, siglumFiltered, workloads);
                        }
                    }
                } else {
                    workloadsFiltered = workloadRepository.findWorkloadsByExerciseAndSiglums(exercises, siglumFiltered, workloads);
                }
            } catch (Exception e) {
                // Do nothing
            }
        }

        mergeWorkloadEvolutionDTOs(barDTOList, createWorkloadEvolutionDTOs(workloadsFiltered));
    }

    private LocalDate convertInstantToLocalDate(Instant instant) {
        LocalDate localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        return localDate;
    }

    public void processLatestExerciseWorkloads(
            List<EvolutionDTO> barDTOList,
            String validationStatus,
            List<WorkloadEvolution> workloadEvolutionList,
            List<Siglum> siglumFiltered,
            boolean isMultiSiglum) {
        String latestExerciseOpened = workloadEvolutionRepository.findLatestOpenedExerciseWithoutClosedState();
        if (latestExerciseOpened == null || latestExerciseOpened.isEmpty()) {
            return;
        }
        List<WorkloadEvolution> workloadEvolutionLatestState = workloadEvolutionRepository.findAllWorkloadsInLatestStateBySiglum(
                latestExerciseOpened, workloadEvolutionList, siglumFiltered);
        if (workloadEvolutionLatestState != null && !workloadEvolutionLatestState.isEmpty()) {
            workloadEvolutionLatestState = filterWorkloadEvolutionByStatus(workloadEvolutionLatestState, validationStatus, siglumFiltered);
            addCurrentStatusWorkloadEvolution(barDTOList, workloadEvolutionLatestState, isMultiSiglum);
        }
    }

    private List<WorkloadEvolution> filterWorkloadEvolutionByStatus(
            List<WorkloadEvolution> workloadEvolutions,
            String validationStatus, List<Siglum> siglumFiltered) {
        if(siglumFiltered.size() > 1) {
            String statusToCheck = validationStatus != null
                    ? validationStatus.toLowerCase()
                    : Constants.STATUS_ALL.toLowerCase();
            return workloadEvolutions.stream()
                    .filter(workloadEvolution -> {
                        if (Constants.STATUS_REJECTED.equalsIgnoreCase(statusToCheck)) {
                            return workloadEvolution.getStatus().toLowerCase().contains(Constants.STATUS_REJECTED.toLowerCase());
                        } else if (Constants.STATUS_APPROVED.equalsIgnoreCase(statusToCheck)) {
                            return !workloadEvolution.getStatus().toLowerCase().contains(Constants.STATUS_REJECTED.toLowerCase());
                        } else if (Constants.STATUS_ALL.equalsIgnoreCase(statusToCheck)) {
                            return true;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        } else {
            return workloadEvolutions;
        }
    }

    private String calculateWipValue(List<WorkloadEvolution> workloadEvolutionLatestState, List<Siglum> siglumFiltered, List<EvolutionDTO> barDTOList) {
        String wipValue = "";
        if (siglumFiltered.size() == 1 && !workloadEvolutionLatestState.isEmpty()) {
            if (barDTOList.get(0).getExercise().toLowerCase().contains(Constants.WORKLOAD_STATUS_BOTTOM_UP.toLowerCase())) {
                return Constants.WORKLOAD_STATUS_BOTTOM_UP.toUpperCase();
            }
            if (barDTOList.get(0).getExercise().toLowerCase().contains(Constants.WORKLOAD_STATUS_QMC.toLowerCase()) &&
                    workloadEvolutionLatestState.get(0).getStatus().toLowerCase().contains(Constants.WORKLOAD_EVOLUTION_STATUS_QMC_APPROVED.toLowerCase())) {
                return Constants.WORKLOAD_STATUS_QMC.toUpperCase();
            }
            if (barDTOList.get(0).getExercise().toLowerCase().contains(Constants.WORKLOAD_STATUS_QMC.toLowerCase()) &&
                    workloadEvolutionLatestState.get(0).getStatus().toLowerCase().contains(Constants.WORKLOAD_EVOLUTION_STATUS_QMC_REJECTED.toLowerCase())) {
                return Constants.WORKLOAD_STATUS_QMC;
            }
            if (barDTOList.get(0).getExercise().toLowerCase().contains(Constants.WORKLOAD_STATUS_HOT1Q.toLowerCase())&&
                    workloadEvolutionLatestState.get(0).getStatus().toLowerCase().contains(Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_APPROVED.toLowerCase())) {
                return "";
            }
            if (barDTOList.get(0).getExercise().toLowerCase().contains(Constants.WORKLOAD_STATUS_HOT1Q.toLowerCase())&&
                    workloadEvolutionLatestState.get(0).getStatus().toLowerCase().contains(Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_REJECTED.toLowerCase())) {
                return Constants.WORKLOAD_STATUS_HOT1Q.toUpperCase();
            }
        }

        return wipValue;
    }

    private String calculateLastStatusValue(List<WorkloadEvolution> workloadEvolutionLatestState, List<Siglum> siglumFiltered, List<EvolutionDTO> barDTOList) {
        String lastStatus = "";
        if (!workloadEvolutionLatestState.isEmpty() && siglumFiltered.size() == 1) {
            if (barDTOList.get(0).getExercise().toLowerCase().contains(Constants.WORKLOAD_STATUS_BOTTOM_UP.toLowerCase())) {
                return Constants.WORKLOAD_EVOLUTION_STATUS_OPENED.toUpperCase();
            }
            if (barDTOList.get(0).getExercise().toLowerCase().contains(Constants.WORKLOAD_STATUS_QMC.toLowerCase()) &&
                    workloadEvolutionLatestState.get(0).getStatus().toLowerCase().contains(Constants.WORKLOAD_EVOLUTION_STATUS_QMC_APPROVED.toLowerCase())) {
                return Constants.WORKLOAD_EVOLUTION_STATUS_BU_SUBMIT.toUpperCase();
            }
            if (barDTOList.get(0).getExercise().toLowerCase().contains(Constants.WORKLOAD_STATUS_QMC.toLowerCase()) &&
                    workloadEvolutionLatestState.get(0).getStatus().toLowerCase().contains(Constants.WORKLOAD_EVOLUTION_STATUS_QMC_REJECTED.toLowerCase())) {
                return Constants.STATUS_REJECTED;
            }
            if (barDTOList.get(0).getExercise().toLowerCase().contains(Constants.WORKLOAD_STATUS_HOT1Q.toLowerCase())&&
                    workloadEvolutionLatestState.get(0).getStatus().toLowerCase().contains(Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_APPROVED.toLowerCase())) {
                return Constants.STATUS_APPROVED.toUpperCase();
            }
            if (barDTOList.get(0).getExercise().toLowerCase().contains(Constants.WORKLOAD_STATUS_HOT1Q.toLowerCase())&&
                    workloadEvolutionLatestState.get(0).getStatus().toLowerCase().contains(Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_REJECTED.toLowerCase())) {
                return Constants.STATUS_REJECTED.toUpperCase();
            }
        }
        return lastStatus;
    }
    // AQUI SE CALCULA EL QMC
    private void addCurrentStatusWorkloadEvolution(
            List<EvolutionDTO> barDTOList,
            List<WorkloadEvolution> workloadEvolutionLatestState,
            boolean isMultiSiglum) {
        if (workloadEvolutionLatestState == null || workloadEvolutionLatestState.isEmpty()) {
            return;
        }
        Map<String, EvolutionDTO> groupedByStatus = new HashMap<>();
        for (WorkloadEvolution workload : workloadEvolutionLatestState) {
            String status = workload.getStatus();
            if (Constants.WORKLOAD_EVOLUTION_STATUS_OPENED.equalsIgnoreCase(status)
                    || (isMultiSiglum && status.contains(Constants.WORKLOAD_EVOLUTION_STATUS_PENDING))) {
                continue;
            }
            String exerciseName = calculateExerciseName(workload.getStatus());
            groupedByStatus.putIfAbsent(exerciseName, new EvolutionDTO(
                    null,
                    exerciseName,
                    null,
                    null,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            ));
            EvolutionDTO dto = groupedByStatus.get(exerciseName);
            dto.setKHrsOwnDirect(dto.getKHrsOwnDirect() + (workload.getKHrsOwnDirect() != null ? workload.getKHrsOwnDirect() : 0.0));
            dto.setKHrsOwnIndirect(dto.getKHrsOwnIndirect() + (workload.getKHrsOwnIndirect() != null ? workload.getKHrsOwnIndirect() : 0.0));
            dto.setKHrsSubDirect(dto.getKHrsSubDirect() + (workload.getKHrsSubDirect() != null ? workload.getKHrsSubDirect() : 0.0));
            dto.setKHrsSubIndirect(dto.getKHrsSubIndirect() + (workload.getKHrsSubIndirect() != null ? workload.getKHrsSubIndirect() : 0.0));
        }
        groupedByStatus = sortListGroupedByStatus(groupedByStatus);
        barDTOList.addAll(groupedByStatus.values());
    }


    private Map<String, EvolutionDTO> sortListGroupedByStatus(Map<String, EvolutionDTO> groupedByStatus) {
        List<String> orderKeys = Arrays.asList(
                Constants.WORKLOAD_STATUS_BOTTOM_UP_SUBMISSION,
                Constants.WORKLOAD_STATUS_QMC,
                Constants.WORKLOAD_STATUS_HOT1Q
        );
        List<Map.Entry<String, EvolutionDTO>> sortedEntries = groupedByStatus.entrySet().stream()
                .sorted((entry1, entry2) -> {
                    int index1 = orderKeys.indexOf(entry1.getKey());
                    int index2 = orderKeys.indexOf(entry2.getKey());
                    if (index1 == -1 && index2 == -1) {
                        return 0;
                    } else if (index1 == -1) {
                        return 1;
                    } else if (index2 == -1) {
                        return -1;
                    } else {
                        return Integer.compare(index1, index2);
                    }
                })
                .toList();
        Map<String, EvolutionDTO> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, EvolutionDTO> entry : sortedEntries) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

    private String calculateExerciseName(String exercise) {
        if (exercise == null || exercise.isEmpty()) {
            return "";
        }
        if (exercise.toLowerCase().contains(Constants.EXERCISE_OPERATION_PLANNING.toLowerCase()) ||
                exercise.toLowerCase().contains(Constants.EXERCISE_FORECAST.toLowerCase()) ||
                exercise.toLowerCase().contains(Constants.WORKLOAD_STATUS_BOTTOM_UP.toLowerCase())) {
            return exercise;
        }
        //TODO aqui se agrupa en First Submission
        if (containsAnyIgnoreCase(exercise,
                Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_HR,
                Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_6,
                Constants.WORKLOAD_EVOLUTION_STATUS_REJECTED_BY_SIGLUM_6,
                Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_5,
                Constants.WORKLOAD_EVOLUTION_STATUS_REJECTED_BY_SIGLUM_5,
                Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_4)) {
            return Constants.WORKLOAD_STATUS_BOTTOM_UP_SUBMISSION;
        }
        if (exercise.toLowerCase().contains(Constants.WORKLOAD_STATUS_QMC.toLowerCase())) {
            return Constants.WORKLOAD_STATUS_QMC;
        }
        if (exercise.toLowerCase().contains(Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_APPROVED.toLowerCase()) ||
                exercise.toLowerCase().contains(Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_REJECTED.toLowerCase()) ||
                exercise.toLowerCase().contains(Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_PENDING.toLowerCase())) {
            return Constants.WORKLOAD_STATUS_HOT1Q;
        }
        return "";
    }

    private boolean containsAnyIgnoreCase(String target, String... values) {
        if (target == null || values == null) {
            return false;
        }
        for (String value : values) {
            if (value != null && target.toLowerCase().contains(value.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private void addDefaultEvolutionDTOs(List<EvolutionDTO> barDTOList, List<String> latestExercises) {
        for (String exercise : latestExercises) {
            barDTOList.add(new EvolutionDTO(
                    null,
                    calculateExerciseName(exercise),
                    null,
                    null,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            ));
        }
    }

    private List<EvolutionDTO> createWorkloadEvolutionDTOs(List<Workload> workloads) {
        if (workloads.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, EvolutionDTO> groupedData = new LinkedHashMap<>();
        for (Workload workload : workloads) {
            String exercise = workload.getExercise();
            String exerciseName = calculateExerciseName(exercise);
            groupedData.putIfAbsent(exerciseName, new EvolutionDTO(
                    null,
                    exerciseName,
                    null,
                    null,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            ));
            EvolutionDTO dto = groupedData.get(exerciseName);
            double hours = workload.getKHrs() != null ? workload.getKHrs() : 0.0;
            if ("direct".equalsIgnoreCase(workload.getDirect())) {
                if ("own".equalsIgnoreCase(workload.getOwn())) {
                    dto.setKHrsOwnDirect(dto.getKHrsOwnDirect() + hours);
                } else if ("sub".equalsIgnoreCase(workload.getOwn())) {
                    dto.setKHrsSubDirect(dto.getKHrsSubDirect() + hours);
                }
                dto.setId(workload.getId());
            } else if ("indirect".equalsIgnoreCase(workload.getDirect())) {
                if ("own".equalsIgnoreCase(workload.getOwn())) {
                    dto.setKHrsOwnIndirect(dto.getKHrsOwnIndirect() + hours);
                } else if ("sub".equalsIgnoreCase(workload.getOwn())) {
                    dto.setKHrsSubIndirect(dto.getKHrsSubIndirect() + hours);
                }
                dto.setId(workload.getId());
            }
        }
        return new LinkedList<>(groupedData.values());
    }

    private void mergeWorkloadEvolutionDTOs(List<EvolutionDTO> existingList, List<EvolutionDTO> newList) {
        Map<String, EvolutionDTO> existingMap = existingList.stream()
                .collect(Collectors.toMap(EvolutionDTO::getExercise, dto -> dto, (dto1, dto2) -> dto1));
        for (EvolutionDTO newDto : newList) {
            EvolutionDTO existingDto = existingMap.get(newDto.getExercise());
            if (existingDto != null) {
                existingDto.setId(newDto.getId());
                existingDto.setKHrsOwnDirect(existingDto.getKHrsOwnDirect() + newDto.getKHrsOwnDirect());
                existingDto.setKHrsOwnIndirect(existingDto.getKHrsOwnIndirect() + newDto.getKHrsOwnIndirect());
                existingDto.setKHrsSubDirect(existingDto.getKHrsSubDirect() + newDto.getKHrsSubDirect());
                existingDto.setKHrsSubIndirect(existingDto.getKHrsSubIndirect() + newDto.getKHrsSubIndirect());
            } else {
                existingList.add(newDto);
            }
        }
    }

    private boolean calculateMoreThanOneSiglum(List<WorkloadEvolution> workloadEvolutionList,
                                               List<Workload> workloadList,
                                               List<Siglum> siglumFiltered) {
        if (workloadEvolutionList == null || workloadList == null || siglumFiltered == null) {
            throw new IllegalArgumentException("Las listas no deben ser nulas");
        }

        Set<Long> uniqueSiglumIdsInWorkloadEvolution = workloadEvolutionList.stream()
                .filter(Objects::nonNull)
                .map(WorkloadEvolution::getSiglum)
                .filter(Objects::nonNull)
                .map(Siglum::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        boolean hasDifferentSiglumIdsInWorkloadEvolution = uniqueSiglumIdsInWorkloadEvolution.size() > 1;

        Set<Long> uniqueWorkloadIds = workloadList.stream()
                .filter(Objects::nonNull)
                .map(Workload::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        boolean hasDifferentWorkloads = uniqueWorkloadIds.size() > 1;

        Set<Long> uniqueSiglumIdsInSiglumFiltered = siglumFiltered.stream()
                .filter(Objects::nonNull)
                .map(Siglum::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        boolean hasDifferentSiglumFiltered = uniqueSiglumIdsInSiglumFiltered.size() > 1;

        return hasDifferentSiglumIdsInWorkloadEvolution && hasDifferentWorkloads && hasDifferentSiglumFiltered;
    }
    
    private String calculateStatus(List<EvolutionDTO> barDTOList, boolean moreThanOneSiglum, List<Siglum> siglumFiltered) {
        String lastOpenedStatus;
        try {
            lastOpenedStatus = utils.getLastOpenedExerciseOrThrow().getExercise();
        } catch (Exception e) {
            return "";
        }

        if (moreThanOneSiglum) {
            return "";
        } else {
            if (barDTOList.size() == 2 &&
                    lastOpenedStatus.startsWith(Constants.EXERCISE_OPERATION_PLANNING) ||
                    lastOpenedStatus.startsWith(Constants.EXERCISE_FORECAST) &&
                            !utils.existsOpenedExercise().isExistsOpenedExercise()) {
                return Constants.STATUS_APPROVED;
            }

            if (barDTOList.size() == 3 &&
                    (lastOpenedStatus.equals(Constants.WORKLOAD_STATUS_BOTTOM_UP_SUBMISSION) ||
                            lastOpenedStatus.startsWith(Constants.EXERCISE_OPERATION_PLANNING) ||
                            lastOpenedStatus.startsWith(Constants.EXERCISE_FORECAST))) {

                Optional<WorkloadEvolution> latest = workloadEvolutionRepository
                        .findTopBySiglumAndExerciseOrderBySubmitDateDesc(siglumFiltered.get(0), lastOpenedStatus);

                if (latest.isPresent()) {
                    String status = latest.get().getStatus();
                    if (status.equals(Constants.WORKLOAD_EVOLUTION_STATUS_REJECTED_BY_SIGLUM_6) ||
                            status.equals(Constants.WORKLOAD_EVOLUTION_STATUS_REJECTED_BY_SIGLUM_5)) {
                        return Constants.STATUS_REJECTED;
                    }
                }
            }
            // ---- START SIGLUM 5 & 6
            boolean containsFourOrFiveSpecificExercisesA = barDTOList.stream()
                    .map(EvolutionDTO::getExercise)
                    .allMatch(exercise -> exercise.equals(Constants.WORKLOAD_STATUS_BOTTOM_UP_SUBMISSION) ||
                            exercise.startsWith(Constants.EXERCISE_OPERATION_PLANNING) ||
                            exercise.startsWith(Constants.EXERCISE_FORECAST) ||
                            exercise.equals(Constants.WORKLOAD_STATUS_BOTTOM_UP) ||
                            exercise.equals(Constants.WORKLOAD_STATUS_QMC) ||
                            exercise.equals(Constants.WORKLOAD_STATUS_HOT1Q));

            boolean hasExactlyFourSpecificExercisesA = barDTOList.size() == 4 && barDTOList.stream()
                    .map(EvolutionDTO::getExercise)
                    .distinct()
                    .filter(exercise -> exercise.equals(Constants.WORKLOAD_STATUS_BOTTOM_UP_SUBMISSION) ||
                            exercise.startsWith(Constants.EXERCISE_OPERATION_PLANNING) ||
                            exercise.startsWith(Constants.EXERCISE_FORECAST) ||
                            exercise.equals(Constants.WORKLOAD_STATUS_BOTTOM_UP))
                    .count() == 4;
            boolean hasExactlyFiveSpecificExercisesA = barDTOList.size() == 5 && barDTOList.stream()
                    .map(EvolutionDTO::getExercise)
                    .distinct()
                    .filter(exercise -> exercise.equals(Constants.WORKLOAD_STATUS_BOTTOM_UP_SUBMISSION) ||
                            exercise.startsWith(Constants.EXERCISE_OPERATION_PLANNING) ||
                            exercise.startsWith(Constants.EXERCISE_FORECAST) ||
                            exercise.equals(Constants.WORKLOAD_STATUS_BOTTOM_UP) ||
                            exercise.equals(Constants.WORKLOAD_STATUS_QMC))
                    .count() == 5;
            boolean hasExactlySixSpecificExercisesA = barDTOList.size() == 6 && barDTOList.stream()
                    .map(EvolutionDTO::getExercise)
                    .distinct()
                    .filter(exercise -> exercise.equals(Constants.WORKLOAD_STATUS_BOTTOM_UP_SUBMISSION) ||
                            exercise.startsWith(Constants.EXERCISE_OPERATION_PLANNING) ||
                            exercise.startsWith(Constants.EXERCISE_FORECAST) ||
                            exercise.equals(Constants.WORKLOAD_STATUS_BOTTOM_UP) ||
                            exercise.equals(Constants.WORKLOAD_STATUS_QMC) ||
                            exercise.equals(Constants.WORKLOAD_STATUS_HOT1Q))
                    .count() == 6;

            if (containsFourOrFiveSpecificExercisesA && (hasExactlyFourSpecificExercisesA || hasExactlyFiveSpecificExercisesA || hasExactlySixSpecificExercisesA)) {
                Optional<WorkloadEvolution> latest = workloadEvolutionRepository
                        .findTopBySiglumAndExerciseOrderBySubmitDateDesc(siglumFiltered.get(0), lastOpenedStatus);
                if (latest.isPresent()) {
                    String status = latest.get().getStatus();
                    if (status.equals(Constants.WORKLOAD_EVOLUTION_STATUS_REJECTED_BY_SIGLUM_6) ||
                            status.equals(Constants.WORKLOAD_EVOLUTION_STATUS_REJECTED_BY_SIGLUM_5) ||
                            status.equals(Constants.WORKLOAD_EVOLUTION_STATUS_QMC_REJECTED) ||
                            status.equals(Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_REJECTED)) {
                        return Constants.STATUS_REJECTED;
                    }
                    if (status.equals(Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_6) ||
                            status.equals(Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_5) ||
                            status.equals(Constants.WORKLOAD_EVOLUTION_STATUS_QMC_PENDING) ||
                            status.equals(Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_PENDING)) {
                        return Constants.STATUS_SUBMIT;
                    }
                    if (status.equals(Constants.WORKLOAD_EVOLUTION_STATUS_QMC_APPROVED) ||
                            status.equals(Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_APPROVED)) {
                        return Constants.STATUS_APPROVED;
                    }
                    if (status.equals(Constants.WORKLOAD_EVOLUTION_STATUS_BU_SUBMIT)) {
                        return Constants.STATUS_SUBMIT;
                    }
                    return Constants.STATUS_OPENED;
                }
            }
            // ---- END SIGLUM 5 & 6

            boolean containsFourSpecificExercises = barDTOList.stream()
                    .map(EvolutionDTO::getExercise)
                    .allMatch(exercise -> exercise.equals(Constants.WORKLOAD_STATUS_BOTTOM_UP_SUBMISSION) ||
                            exercise.startsWith(Constants.EXERCISE_OPERATION_PLANNING) ||
                            exercise.startsWith(Constants.EXERCISE_FORECAST) ||
                            exercise.equals(Constants.WORKLOAD_STATUS_QMC));

            boolean hasExactlyFourSpecificExercises = barDTOList.size() == 4 && barDTOList.stream()
                    .map(EvolutionDTO::getExercise)
                    .distinct()
                    .filter(exercise -> exercise.equals(Constants.WORKLOAD_STATUS_BOTTOM_UP_SUBMISSION) ||
                            exercise.startsWith(Constants.EXERCISE_OPERATION_PLANNING) ||
                            exercise.startsWith(Constants.EXERCISE_FORECAST) ||
                            exercise.equals(Constants.WORKLOAD_STATUS_QMC))
                    .count() == 4;

            if (containsFourSpecificExercises && hasExactlyFourSpecificExercises) {
                Optional<WorkloadEvolution> latest = workloadEvolutionRepository
                        .findTopBySiglumAndExerciseOrderBySubmitDateDesc(siglumFiltered.get(0), lastOpenedStatus);
                if (latest.isPresent()) {
                    String status = latest.get().getStatus();
                    if (status.equals(Constants.WORKLOAD_EVOLUTION_STATUS_QMC_REJECTED)) {
                        return Constants.STATUS_REJECTED;
                    }
                }
            }

            boolean containsFiveSpecificExercises = barDTOList.stream()
                    .map(EvolutionDTO::getExercise)
                    .allMatch(exercise -> exercise.equals(Constants.WORKLOAD_STATUS_HOT1Q) ||
                            exercise.startsWith(Constants.EXERCISE_OPERATION_PLANNING) ||
                            exercise.startsWith(Constants.EXERCISE_FORECAST) ||
                            exercise.equals(Constants.WORKLOAD_STATUS_QMC) ||
                            exercise.equals(Constants.WORKLOAD_STATUS_BOTTOM_UP_SUBMISSION));

            boolean hasExactlyFiveSpecificExercises = barDTOList.size() == 5 && barDTOList.stream()
                    .map(EvolutionDTO::getExercise)
                    .distinct()
                    .filter(exercise -> exercise.equals(Constants.WORKLOAD_STATUS_HOT1Q) ||
                            exercise.startsWith(Constants.EXERCISE_OPERATION_PLANNING) ||
                            exercise.startsWith(Constants.EXERCISE_FORECAST) ||
                            exercise.equals(Constants.WORKLOAD_STATUS_QMC) ||
                            exercise.equals(Constants.WORKLOAD_STATUS_BOTTOM_UP_SUBMISSION))
                    .count() == 5;

            if (containsFiveSpecificExercises && hasExactlyFiveSpecificExercises) {
                Optional<WorkloadEvolution> latest = workloadEvolutionRepository
                        .findTopBySiglumAndExerciseOrderBySubmitDateDesc(siglumFiltered.get(0), lastOpenedStatus);
                if (latest.isPresent()) {
                    String status = latest.get().getStatus();
                    if (status.equals(Constants.WORKLOAD_EVOLUTION_STATUS_QMC_APPROVED)) {
                        return Constants.STATUS_OPENED;
                    }
                    if (status.equals(Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_APPROVED)) {
                        return Constants.STATUS_APPROVED;
                    }
                    if (status.equals(Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_REJECTED)) {
                        return Constants.STATUS_REJECTED;
                    }
                }
                return Constants.STATUS_OPENED;
            }
        }
        return "";
    }

    private boolean calculateMultipleSiglums(List<Siglum> siglumFiltered, List<Workload> workloadList) {
        boolean singleVisibleSiglum = siglumFiltered.size() == 1;
        boolean singleSiglumInWorkloads = workloadList.stream()
                .map(Workload::getSiglum)
                .filter(Objects::nonNull)
                .distinct()
                .count() <= 1;

        return !(singleVisibleSiglum || singleSiglumInWorkloads);
    }

    private String calculateWIP(List<EvolutionDTO> barDTOList, boolean moreThanOneSiglum, List<Siglum> siglumFiltered) {
        return "";
    }

    /**
     * This method sorts a list of EvolutionDTO objects by their 'id' attribute in ascending order.
     * It is inferred that the lowest id is the oldest record
     *
     * @param barDTOList The list of EvolutionDTO objects to be sorted.
     */
    public void orderByLowId(List<EvolutionDTO> barDTOList) {
        if (barDTOList != null && !barDTOList.isEmpty()) {
            barDTOList.sort(Comparator.comparing(EvolutionDTO::getId));
        }
    }

    /**
     * Checks and modifies a list of EvolutionDTOs based on specific conditions:
     * - If the list contains an element with the attribute exercise="HOT1Q",
     *   it removes all elements with the attribute exercise="BU".
     *
     * @param barDTOList the list of EvolutionDTOs to process
     * @param lastStatus the last status
     */
    private void checkObjectsToReturn(List<EvolutionDTO> barDTOList, String lastStatus) {
        if (barDTOList != null && !barDTOList.isEmpty()) {
            boolean hasHot1Q = barDTOList.stream()
                    .anyMatch(dto -> Constants.WORKLOAD_STATUS_HOT1Q.equals(dto.getExercise()));
            boolean hasFirstSubm = barDTOList.stream()
                    .anyMatch(dto -> Constants.WORKLOAD_STATUS_BOTTOM_UP_SUBMISSION.equals(dto.getExercise()));
            if ((hasHot1Q || hasFirstSubm) && (!Constants.STATUS_REJECTED.equals(lastStatus) && !Constants.STATUS_APPROVED.equals(lastStatus))) {
                barDTOList.removeIf(dto -> Constants.WORKLOAD_STATUS_BOTTOM_UP.equals(dto.getExercise()));
            }
        }
    }

}