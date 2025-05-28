package com.airbus.optim.service;

import com.airbus.optim.dto.CanUpdateWorkloadDTO;
import com.airbus.optim.dto.EvolutionDTO;
import com.airbus.optim.dto.ExistsOpenedExerciseDTO;
import com.airbus.optim.dto.SiglumStatusResponseDTO;
import com.airbus.optim.entity.Siglum;
import com.airbus.optim.entity.Workload;
import com.airbus.optim.entity.WorkloadEvolution;
import com.airbus.optim.repository.SiglumRepository;
import com.airbus.optim.repository.WorkloadEvolutionRepository;
import com.airbus.optim.repository.WorkloadRepository;
import com.airbus.optim.service.workloadImpl.WorkloadUtils;
import com.airbus.optim.utils.Constants;
import com.airbus.optim.utils.Utils;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkloadEvolutionService {

    @Autowired
    private WorkloadRepository workloadRepository;

    @Autowired
    private WorkloadEvolutionRepository workloadEvolutionRepository;

    @Autowired
    private SiglumRepository siglumRepository;

    @Autowired
    private SiglumService siglumService;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkloadService workloadService;

    @Autowired
    private WorkloadUtils workloadUtils;

    @Autowired
    private Utils utils;

    public void openExercise(String newExercise) {
        boolean exerciseExists = workloadEvolutionRepository.existsByExercise(newExercise);
        if (exerciseExists) {
            throw new IllegalArgumentException("An exercise with the same name already exists: " + newExercise);
        }

        Optional<WorkloadEvolution> optionalLastClosedExercise = workloadEvolutionRepository.findTopByStatusOrderBySubmitDateDesc(Constants.WORKLOAD_EVOLUTION_STATUS_CLOSED);
        optionalLastClosedExercise.ifPresent(this::cloneWorkloads);
        createNewWorkloadEvolution(newExercise);
    }

    @Transactional
    protected void cloneWorkloads(WorkloadEvolution lastClosedExercise) {
        List<Workload> workloadsToClone = workloadRepository.findByExercise(lastClosedExercise.getExercise());
        workloadsToClone.forEach(this::cloneAndSaveWorkload);
    }

    @Transactional
    protected void createNewWorkloadEvolution(String newExercise) {
        List<Siglum> siglums = siglumRepository.findAll();
        siglums.forEach(siglum -> createAndSaveWorkloadEvolution(siglum, newExercise, Constants.WORKLOAD_EVOLUTION_STATUS_OPENED, false));
    }

    @Transactional
    public void closeExercise() {
        WorkloadEvolution lastOpenedExercise = getLastOpenedExerciseOrThrow();
        String lastOpenedExerciseName = lastOpenedExercise.getExercise();

        List<Workload> workloadsToUpdate = workloadRepository.findByExercise(Constants.WORKLOAD_STATUS_BOTTOM_UP);
        workloadsToUpdate.forEach(workload -> updateWorkloadExercise(workload, lastOpenedExerciseName));

        List<Siglum> siglums = siglumRepository.findAll();
        siglums.forEach(siglum -> createAndSaveWorkloadEvolution(siglum, lastOpenedExerciseName, Constants.WORKLOAD_EVOLUTION_STATUS_CLOSED, false));

        lastOpenedExercise.setStatus(Constants.WORKLOAD_EVOLUTION_STATUS_CLOSED);
        workloadEvolutionRepository.save(lastOpenedExercise);
    }

    private void cloneAndSaveWorkload(Workload workload) {
        Workload clonedWorkload = new Workload();
        clonedWorkload.setExercise(Constants.WORKLOAD_STATUS_BOTTOM_UP);
        clonedWorkload.setSiglum(workload.getSiglum());
        clonedWorkload.setCostCenter(workload.getCostCenter());
        clonedWorkload.setDirect(workload.getDirect());
        clonedWorkload.setCollar(workload.getCollar());
        clonedWorkload.setOwn(workload.getOwn());
        clonedWorkload.setCore(workload.getCore());
        clonedWorkload.setGo(workload.getGo());
        clonedWorkload.setDescription(workload.getDescription());
        clonedWorkload.setStartDate(workload.getStartDate());
        clonedWorkload.setEndDate(workload.getEndDate());
        clonedWorkload.setKHrs(workload.getKHrs());
        clonedWorkload.setKEur(workload.getKEur());
        clonedWorkload.setEac(workload.getEac());
        workloadRepository.save(clonedWorkload);
    }

    private void updateWorkloadExercise(Workload workload, String newExercise) {
        workload.setExercise(newExercise);
        workloadRepository.save(workload);
    }

    private void deletePendingWorkloadEvolution(Siglum siglum, String exercise, String status) {
        Optional<WorkloadEvolution> entity = workloadEvolutionRepository.findBySiglumAndStatusAndExercise(siglum, status, exercise);
        if (entity.isPresent() && entity.get().getStatus().contains("pending"))
            workloadEvolutionRepository.delete(entity.get());
    }

    private void createPendingWorkloadEvolution(Siglum siglum, String exercise) {
        Optional<WorkloadEvolution> lastWorkloadEvolution = workloadEvolutionRepository
                .findTopBySiglumAndExerciseOrderBySubmitDateDesc(siglum, exercise);
        if(lastWorkloadEvolution.isPresent()){
            WorkloadEvolution workloadEvolution = lastWorkloadEvolution.get();
            if (Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_5.equals(workloadEvolution.getStatus())) {
                createAndSaveWorkloadEvolution(siglum, workloadEvolution.getExercise(), Constants.WORKLOAD_EVOLUTION_STATUS_QMC_PENDING, false);
            }
            if (Constants.WORKLOAD_EVOLUTION_STATUS_QMC_APPROVED.equals(workloadEvolution.getStatus())) {
                createAndSaveWorkloadEvolution(siglum, workloadEvolution.getExercise(), Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_PENDING, false);
            }
        }
    }

    private void createAndSaveWorkloadEvolution(Siglum siglum, String exercise, String status, Boolean isRejected) {
        Optional<WorkloadEvolution> entity = workloadEvolutionRepository.findBySiglumAndStatusAndExercise(siglum, status, exercise);
        if (entity.isPresent() && isRejected) { //TODO CONTROL PARA ACTUALIZAR TODOS LOS WORKLOADS
            Optional<WorkloadEvolution> optionalWorkloadEvolution = workloadEvolutionRepository.findBySiglumAndExerciseWithLatestSubmitDate(siglum, exercise);
            if (optionalWorkloadEvolution.isPresent() && optionalWorkloadEvolution.get().getStatus().contains("rejected")) {
                workloadEvolutionRepository.delete(optionalWorkloadEvolution.get());
            }
            //FALTA DEFINICION, ESTE BLOQUE ACTUALIZA KHRS HISTORICOS SI SE MODIFICAN DESPUES DE SER RECHAZADOS
//            List<WorkloadEvolution> workloadEvolutionList = workloadEvolutionRepository.findBySiglumAndExercise(siglum, exercise);
//            for (WorkloadEvolution item:workloadEvolutionList) {
//                if (!Constants.WORKLOAD_EVOLUTION_STATUS_OPENED.equals(item.getStatus())) {
//                    item.setSubmitDate(Instant.now());
//                    List<Siglum> siglumList = List.of(siglum);
//                    if (!status.equalsIgnoreCase(Constants.STATUS_OPENED)) {
//                        EvolutionDTO calculatedKHorus = workloadUtils.dbBuildWorkloadEvolutionDTO(siglumList, Constants.WORKLOAD_STATUS_BOTTOM_UP);
//                        item.setKHrsOwnDirect(calculatedKHorus.getKHrsOwnDirect());
//                        item.setKHrsOwnIndirect(calculatedKHorus.getKHrsOwnIndirect());
//                        item.setKHrsSubDirect(calculatedKHorus.getKHrsSubDirect());
//                        item.setKHrsSubIndirect(calculatedKHorus.getKHrsSubIndirect());
//
//                        item.setFTE(workloadRepository.workloadFTEbyExercise(siglumList, Constants.WORKLOAD_STATUS_BOTTOM_UP).isEmpty() ?
//                                0.0 : workloadRepository.workloadFTEbyExercise(siglumList, Constants.WORKLOAD_STATUS_BOTTOM_UP).get(0).getFte());
//                    }
//                    workloadEvolutionRepository.save(item);
//                }
//            }
        } else if (!entity.isPresent()) {
            WorkloadEvolution workloadEvolution = new WorkloadEvolution();
            workloadEvolution.setExercise(exercise);
            workloadEvolution.setStatus(status);
            workloadEvolution.setSiglum(siglum);
            workloadEvolution.setSubmitDate(Instant.now());

            List<Siglum> siglumList = List.of(siglum);
            if(!status.equalsIgnoreCase(Constants.STATUS_OPENED)){
                EvolutionDTO calculatedKHorus = workloadUtils.dbBuildWorkloadEvolutionDTO(siglumList, Constants.WORKLOAD_STATUS_BOTTOM_UP);
                workloadEvolution.setKHrsOwnDirect(calculatedKHorus.getKHrsOwnDirect());
                workloadEvolution.setKHrsOwnIndirect(calculatedKHorus.getKHrsOwnIndirect());
                workloadEvolution.setKHrsSubDirect(calculatedKHorus.getKHrsSubDirect());
                workloadEvolution.setKHrsSubIndirect(calculatedKHorus.getKHrsSubIndirect());

                workloadEvolution.setFTE(workloadRepository.workloadFTEbyExercise(siglumList, Constants.WORKLOAD_STATUS_BOTTOM_UP).isEmpty() ?
                        0.0 : workloadRepository.workloadFTEbyExercise(siglumList, Constants.WORKLOAD_STATUS_BOTTOM_UP).get(0).getFte());
            }
            workloadEvolutionRepository.save(workloadEvolution);
        }
        }

    public WorkloadEvolution getLastOpenedExerciseOrThrow() {
        return utils.getLastOpenedExerciseOrThrow();
    }

    public SiglumStatusResponseDTO getSiglumsFirstSubmission(String userSelected) {
//        MOSTRAR EN BANDEJA SUBMIT SOLO WORKLOADS PROPIOS O TODOS SI ES SUPERUSER
//        List<Siglum> siglumVisible = utils.isSuperUser(userSelected)
//                ? utils.getVisibleSiglums(null, userSelected)
//                : Collections.singletonList(utils.getUserInSession(userSelected).getSiglum());

        List<Siglum> siglumVisible = utils.getVisibleSiglums(null, userSelected);


        WorkloadEvolution lastOpenedExercise = getLastOpenedExerciseOrThrow();

        List<String> approvedStatuses = List.of(
                Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_HR,
                Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_6,
                Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_5,
                Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_4,
                Constants.WORKLOAD_EVOLUTION_STATUS_QMC_APPROVED,
                Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_APPROVED
        );

        List<String> rejectedStatuses = List.of(
                Constants.WORKLOAD_EVOLUTION_STATUS_REJECTED_BY_SIGLUM_6,
                Constants.WORKLOAD_EVOLUTION_STATUS_REJECTED_BY_SIGLUM_5,
                Constants.WORKLOAD_EVOLUTION_STATUS_QMC_REJECTED,
                Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_REJECTED
        );

        List<String> allRelevantSiglums = workloadEvolutionRepository.findSiglumHRBySiglumAndStatusesAndExercise(
                siglumVisible,
                approvedStatuses,
                Constants.WORKLOAD_EVOLUTION_STATUS_OPENED,
                lastOpenedExercise.getExercise()
        );

        List<String> allRelevantSiglumsRejected = workloadEvolutionRepository.findSiglumHRBySiglumAndStatusesAndExercise(
                siglumVisible,
                rejectedStatuses,
                Constants.WORKLOAD_EVOLUTION_STATUS_OPENED,
                lastOpenedExercise.getExercise()
        );

        List<String> approvedSiglums = allRelevantSiglums.stream()
                .filter(siglumHR -> approvedStatuses.contains(workloadEvolutionRepository.getStatusBySiglumAndExercise(siglumHR, lastOpenedExercise.getExercise())))
                .distinct()
                .collect(Collectors.toList());

        List<String> rejectedSiglums = allRelevantSiglumsRejected.stream()
                .filter(siglumHR -> rejectedStatuses.contains(workloadEvolutionRepository.getStatusBySiglumAndExercise(siglumHR, lastOpenedExercise.getExercise())))
                .distinct()
                .collect(Collectors.toList());

        List<String> pendingSiglums = allRelevantSiglums.stream()
                .filter(siglumHR -> (!rejectedSiglums.contains(siglumHR) && !approvedSiglums.contains(siglumHR)))
                .distinct()
                .collect(Collectors.toList());

        setSiglumStatusByList(siglumVisible, lastOpenedExercise.getExercise(), approvedSiglums);
        setSiglumStatusByList(siglumVisible, lastOpenedExercise.getExercise(), rejectedSiglums);
        setSiglumStatusByList(siglumVisible, lastOpenedExercise.getExercise(), pendingSiglums);

        return new SiglumStatusResponseDTO(pendingSiglums, approvedSiglums, rejectedSiglums);
    }

    @Transactional
    public SiglumStatusResponseDTO createSiglumsFirstSubmission(List<String> approvedList, String userSelected) {
        WorkloadEvolution lastOpenedExercise = getLastOpenedExerciseOrThrow();

        // Definir un flag para verificar si hay algún "REJECTED" en la lista
        boolean isRejected = approvedList.stream().anyMatch(siglumHR -> siglumHR.contains("REJECTED"));

        // Filtrar approvedList para quedarse solo con el SIGLUM
        List<String> filteredApprovedList = approvedList.stream()
                .map(siglumHR -> siglumHR.split(" ")[0]) // Tomar solo la parte SIGLUM
                .collect(Collectors.toList());

        List<Siglum> siglumVisible = utils.getVisibleSiglums(null, userSelected);

        Set<String> visibleSiglumHRs = siglumVisible.stream()
                .map(Siglum::getSiglumHR)
                .collect(Collectors.toSet());

        List<String> invalidSiglums = filteredApprovedList.stream()
                .filter(siglumHR -> !visibleSiglumHRs.contains(siglumHR))
                .collect(Collectors.toList());

        if (!invalidSiglums.isEmpty()) {
            throw new IllegalArgumentException("Los siguientes Siglums no son visibles: " + invalidSiglums);
        }

        for (String siglumHR : filteredApprovedList) {
            Siglum siglum = siglumRepository.findBySiglumHR(siglumHR)
                    .orElseThrow(() -> new IllegalArgumentException("Siglum no encontrado: " + siglumHR));

            List<Workload> workloads = workloadRepository.findByExerciseAndSiglumInAndGoTrue(Constants.WORKLOAD_STATUS_BOTTOM_UP, List.of(siglum));

            for (Workload workload : workloads) {
                validateWorkloadFields(workload);
            }

            String status = calculateNextWorkloadStatus(userSelected, siglum, lastOpenedExercise);
            createAndSaveWorkloadEvolution(siglum, lastOpenedExercise.getExercise(), status, isRejected);
            if (Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_5.equals(status)) {
                createAndSaveWorkloadEvolution(siglum, lastOpenedExercise.getExercise(), Constants.WORKLOAD_EVOLUTION_STATUS_QMC_PENDING, false);
            }
            if (Constants.WORKLOAD_EVOLUTION_STATUS_QMC_APPROVED.equals(status)) {
                createAndSaveWorkloadEvolution(siglum, lastOpenedExercise.getExercise(), Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_PENDING, false);
            }
            createPendingWorkloadEvolution(siglum, lastOpenedExercise.getExercise());
        }

        return new SiglumStatusResponseDTO(null, filteredApprovedList, null);
    }

    @Transactional
    public String calculateNextWorkloadStatus(String email, Siglum siglum, WorkloadEvolution lastOpenedExercise) {
        if (utils.isSuperUser(email)) {
            Optional<WorkloadEvolution> lastWorkloadEvolution = workloadEvolutionRepository
                    .findTopBySiglumAndExerciseOrderBySubmitDateDesc(siglum, lastOpenedExercise.getExercise());

            if (lastWorkloadEvolution.isPresent()) {
                String lastStatus = lastWorkloadEvolution.get().getStatus();

                switch (lastStatus) {
                    case Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_HR:
                        return Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_6;

                    case Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_6:
                        return Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_5;

                    case Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_5:
                        return Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_5;

                    default:
                        return calculateSiglumStatusForWorkload(siglum);
                }
            } else {
                return calculateSiglumStatusForWorkload(siglum);
            }
        } else {
            String userSiglumStatus = utils.getNumberSiglums(email);

            switch (userSiglumStatus) {
                case Constants.USER_SIGLUM_HR:
                    return Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_HR;

                case Constants.USER_SIGLUM_6:
                    return Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_6;

                case Constants.USER_SIGLUM_5:
                    return Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_5;

                default:
                    throw new IllegalArgumentException("No se pudo determinar el estado del Siglum para el usuario.");
            }
        }
    }

    private String calculateSiglumStatusForWorkload(Siglum siglum) {
        if (!Objects.equals(siglum.getSiglumHR(), siglum.getSiglum6())) {
            return Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_HR;
        }
        if (!Objects.equals(siglum.getSiglum6(), siglum.getSiglum5())) {
            return Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_6;
        }
        if (!Objects.equals(siglum.getSiglum5(), siglum.getSiglum4())) {
            return Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_5;
        }
        if (!Objects.equals(siglum.getSiglum4(), siglum.getSiglum3())) {
            return Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_4;
        }
        throw new IllegalArgumentException("No se pudo determinar StatusForWorkload para el usuario.");
    }

    private void validateWorkloadFields(Workload workload) {
        if (workload.getSiglum() == null) {
            throw new IllegalArgumentException("El campo 'Siglum' es obligatorio en el workload con ID: " + workload.getDescription());
        }
        if (workload.getDescription() == null || workload.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("El campo 'Description' es obligatorio en el workload con ID: " + workload.getDescription());
        }
        if (workload.getOwn() == null || workload.getOwn().trim().isEmpty()) {
            throw new IllegalArgumentException("El campo 'OWN/SUB' es obligatorio en el workload con ID: " + workload.getDescription());
        }
        if (workload.getCostCenter() == null) {
            throw new IllegalArgumentException("El campo 'Cost Center' es obligatorio en el workload con ID: " + workload.getDescription());
        }
        if (workload.getDirect() == null || workload.getDirect().trim().isEmpty()) {
            throw new IllegalArgumentException("El campo 'Dir/Ind' es obligatorio en el workload con ID: " + workload.getDescription());
        }
        if (workload.getStartDate() == null) {
            throw new IllegalArgumentException("El campo 'Start Date' es obligatorio en el workload con ID: " + workload.getDescription());
        }
        if (workload.getEndDate() == null) {
            throw new IllegalArgumentException("El campo 'End Date' es obligatorio en el workload con ID: " + workload.getDescription());
        }
        if (workload.getKHrs() == null) {
            throw new IllegalArgumentException("El campo 'kHrs' es obligatorio en el workload con ID: " + workload.getDescription());
        }
        if (workload.getPpsid() == null) {
            throw new IllegalArgumentException("El campo 'PPSID' es obligatorio en el workload con ID: " + workload.getDescription());
        }
    }

    public SiglumStatusResponseDTO getSiglumsStatusByRole(String userSelected) {
        getLastOpenedExerciseOrThrow();
        return calculateSiglumStatus(userSelected);
    }

    @Transactional
    public SiglumStatusResponseDTO setSiglumStatus(List<String> approvedList, List<String> rejectedList, String userSelected) {
        WorkloadEvolution lastOpenedExercise = getLastOpenedExerciseOrThrow();
        List<Siglum> siglumVisible = utils.getVisibleSiglums(null, userSelected);

        Set<String> visibleSiglumHRs = siglumVisible.stream()
                .map(Siglum::getSiglumHR)
                .collect(Collectors.toSet());

        approvedList = (approvedList == null ? Collections.emptyList() : approvedList)
                .stream()
                .map(String::valueOf)
                .map(s -> s.contains(" ") ? s.substring(0, s.indexOf(" ")) : s)
                .collect(Collectors.toList());

        rejectedList = (rejectedList == null ? Collections.emptyList() : rejectedList)
                .stream()
                .map(String::valueOf)
                .map(s -> s.contains(" ") ? s.substring(0, s.indexOf(" ")) : s)
                .collect(Collectors.toList());

        List<String> invalidApproved =
                (approvedList == null ? Collections.emptyList() : approvedList)
                        .stream()
                        .map(Object::toString)
                        .filter(siglumHR -> !visibleSiglumHRs.contains(siglumHR))
                        .collect(Collectors.toList());

        List<String> invalidRejected =
                (rejectedList == null ? Collections.emptyList() : rejectedList)
                        .stream()
                        .map(Object::toString)
                        .filter(siglumHR -> !visibleSiglumHRs.contains(siglumHR))
                        .collect(Collectors.toList());

        if (!invalidApproved.isEmpty() || !invalidRejected.isEmpty()) {
            throw new EntityNotFoundException("Siglums no válidos detectados: " +
                    (invalidApproved.isEmpty() ? "" : "Approved: " + invalidApproved) +
                    (invalidRejected.isEmpty() ? "" : " Rejected: " + invalidRejected));
        }

        if (utils.isSuperUser(userSelected)) {
            processAdminSiglumStatus(approvedList, siglumVisible, lastOpenedExercise, true);
            processAdminSiglumStatus(rejectedList, siglumVisible, lastOpenedExercise, false);
        } else {
            String roles = utils.getUserInSession(userSelected).getRoles();
            processSiglumStatus(approvedList, siglumVisible, roles, lastOpenedExercise, true, userSelected);
            processSiglumStatus(rejectedList, siglumVisible, roles, lastOpenedExercise, false, userSelected);
        }

        SiglumStatusResponseDTO updatedStatus = calculateSiglumStatus(userSelected);
        return updatedStatus;
    }

    private SiglumStatusResponseDTO calculateSiglumStatus(String userSelected) {
        List<Siglum> siglumVisible = utils.getVisibleSiglums(null, userSelected);
        String roles = utils.getUserInSession(userSelected).getRoles().toLowerCase();

        String lastOpenedExercise = getLastOpenedExerciseOrThrow().getExercise();

        List<String> pendingSiglums = new ArrayList<>();
        List<String> approvedSiglums = new ArrayList<>();
        List<String> rejectedSiglums = new ArrayList<>();

        if (!utils.isSuperUser(userSelected)) {
            String siglumType = utils.getNumberSiglums(userSelected);

            if (roles.contains(Constants.USER_ROLE_QMC.toLowerCase())) {
                processStatuses(siglumVisible, lastOpenedExercise,
                        List.of(Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_5, Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_4),
                        List.of(Constants.WORKLOAD_EVOLUTION_STATUS_QMC_APPROVED),
                        List.of(Constants.WORKLOAD_EVOLUTION_STATUS_QMC_REJECTED),
                        pendingSiglums, approvedSiglums, rejectedSiglums);
            }

            if (roles.contains(Constants.USER_ROLE_HO_T1Q.toLowerCase())) {
                processStatuses(siglumVisible, lastOpenedExercise,
                        List.of(Constants.WORKLOAD_EVOLUTION_STATUS_QMC_APPROVED),
                        List.of(Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_APPROVED),
                        List.of(Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_REJECTED),
                        pendingSiglums, approvedSiglums, rejectedSiglums);
            }

            if (roles.contains(Constants.USER_ROLE_HO_SIGLUM.toLowerCase())) {
                processSiglumRole(siglumVisible, lastOpenedExercise, siglumType, pendingSiglums, approvedSiglums, rejectedSiglums);
            }
        } else {
            processSuperUserStatuses(siglumVisible, lastOpenedExercise, pendingSiglums, approvedSiglums, rejectedSiglums);
        }

        filterPendingSiglums(pendingSiglums, approvedSiglums, rejectedSiglums);

        setSiglumStatusByList(siglumVisible, lastOpenedExercise, approvedSiglums);
        setSiglumStatusByList(siglumVisible, lastOpenedExercise, rejectedSiglums);
        setSiglumStatusByList(siglumVisible, lastOpenedExercise, pendingSiglums);

        return new SiglumStatusResponseDTO(pendingSiglums, approvedSiglums, rejectedSiglums);
    }

    private void processStatuses(List<Siglum> siglumVisible, String exercise, List<String> statusesForPending, List<String> statusesForApproved, List<String> statusesForRejected, List<String> pendingSiglums, List<String> approvedSiglums, List<String> rejectedSiglums) {
        List<String> pending = workloadEvolutionRepository.findSiglumHRBySiglumAndStatusAndExerciseIn(siglumVisible, statusesForPending, exercise);
        List<String> approved = workloadEvolutionRepository.findSiglumHRBySiglumAndStatusAndExerciseIn(siglumVisible, statusesForApproved, exercise);
        List<String> rejected = workloadEvolutionRepository.findSiglumHRBySiglumAndStatusAndExerciseIn(siglumVisible, statusesForRejected, exercise);

        rejected.removeAll(approved);

        pendingSiglums.addAll(pending);
        approvedSiglums.addAll(approved);
        rejectedSiglums.addAll(rejected);
    }

    private void processSiglumRole(List<Siglum> siglumVisible, String exercise, String siglumType, List<String> pendingSiglums, List<String> approvedSiglums, List<String> rejectedSiglums) {
        switch (siglumType) {
            case Constants.USER_SIGLUM_HR -> handleSiglumHR(siglumVisible, exercise, approvedSiglums);
            case Constants.USER_SIGLUM_6 -> handleSiglum6(siglumVisible, exercise, pendingSiglums, approvedSiglums, rejectedSiglums);
            case Constants.USER_SIGLUM_5 -> handleSiglum5(siglumVisible, exercise, pendingSiglums, approvedSiglums, rejectedSiglums);
            case Constants.USER_SIGLUM_4 -> handleSiglum4(siglumVisible, exercise, pendingSiglums, approvedSiglums);
        }

        rejectedSiglums.removeAll(approvedSiglums);
        pendingSiglums.removeAll(rejectedSiglums);
        pendingSiglums.removeAll(approvedSiglums);
    }

    private void processSuperUserStatuses(List<Siglum> siglumVisible, String exercise, List<String> pendingSiglums, List<String> approvedSiglums, List<String> rejectedSiglums) {
        List<Object[]> latestStatuses = workloadEvolutionRepository.findLatestStatusBySiglumAndExercise(siglumVisible, exercise);
        Map<String, String> latestStatusMap = new HashMap<>();

        for (Object[] result : latestStatuses) {
            String siglumHR = (String) result[0];
            String status = ((String) result[1]).toUpperCase().replace("_", " ");
            latestStatusMap.put(siglumHR, status);
        }

        for (Map.Entry<String, String> entry : latestStatusMap.entrySet()) {
            String siglumHRWithStatus = entry.getKey() + " (" + entry.getValue() + ")";
            String status = entry.getValue();

            if (!status.toLowerCase().contains(Constants.WORKLOAD_EVOLUTION_STATUS_OPENED.toLowerCase())) {
                if (status.toLowerCase().contains(Constants.JOB_REQUEST_STATUS_HO_T1Q_APPROVED.toLowerCase())) {
                    approvedSiglums.add(siglumHRWithStatus);
                } else if (status.toLowerCase().contains(Constants.STATUS_APPROVED.toLowerCase()) ||
                        status.toLowerCase().contains(Constants.STATUS_SUBMIT.toLowerCase()) ||
                        status.toLowerCase().contains(Constants.STATUS_PENDING.toLowerCase()) ||
                        status.toLowerCase().contains(Constants.STATUS_OPENED.toLowerCase())) {
                    pendingSiglums.add(siglumHRWithStatus);
                } else if (status.toLowerCase().contains(Constants.STATUS_REJECTED.toLowerCase())) {
                    rejectedSiglums.add(siglumHRWithStatus);
                }
            }
        }
    }

    /**
     * Updates the list of siglums by appending the corresponding status if it matches a key in the status map.
     *
     * @param siglumVisible a list of {@code Siglum} objects representing the visible siglums.
     * @param exercise      the current exercise for which the latest statuses are fetched.
     * @param siglums       a list of strings containing the siglums to be evaluated and updated if applicable.
     *
     *                      <p>This method retrieves the latest statuses for the visible siglums based on the provided exercise.
     *                      It then iterates over the {@code siglums} list, and if a siglum matches a key in the status map,
     *                      the siglum is updated by appending its corresponding status.</p>
     */
    private void setSiglumStatusByList(List<Siglum> siglumVisible, String exercise, List<String> siglums) {
        List<Object[]> latestStatuses = workloadEvolutionRepository.findLatestStatusBySiglumAndExercise(siglumVisible, exercise);
        Map<String, String> latestStatusMap = new HashMap<>();

        // Build the map with the latest statuses
        for (Object[] result : latestStatuses) {
            String siglumHR = (String) result[0];
            String status = ((String) result[1]).toUpperCase().replace("_", " ");
            latestStatusMap.put(siglumHR, status);
        }

        // Update the 'siglums' list if they match keys in the status map
        for (int i = 0; i < siglums.size(); i++) {
            String siglum = siglums.get(i);
            if (latestStatusMap.containsKey(siglum)) {
                String siglumHRWithStatus = siglum + " (" + latestStatusMap.get(siglum) + ")";
                siglums.set(i, siglumHRWithStatus);
            }
        }
    }

    private void filterPendingSiglums(List<String> pendingSiglums, List<String> approvedSiglums, List<String> rejectedSiglums) {
        pendingSiglums.removeIf(siglumHR -> approvedSiglums.contains(siglumHR) || rejectedSiglums.contains(siglumHR));
    }

    private void handleSiglumHR(List<Siglum> siglumVisible, String exercise, List<String> approvedSiglums) {
        List<String> statusesForApproved = List.of(
                Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_HR,
                Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_6,
                Constants.WORKLOAD_EVOLUTION_STATUS_REJECTED_BY_SIGLUM_6,
                Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_5,
                Constants.WORKLOAD_EVOLUTION_STATUS_REJECTED_BY_SIGLUM_5,
                Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_4,
                Constants.WORKLOAD_EVOLUTION_STATUS_QMC_REJECTED,
                Constants.WORKLOAD_EVOLUTION_STATUS_QMC_APPROVED,
                Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_REJECTED,
                Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_APPROVED
        );
        approvedSiglums.addAll(workloadEvolutionRepository.findSiglumHRBySiglumAndStatusAndExerciseIn(siglumVisible, statusesForApproved, exercise));
    }

    private void handleSiglum6(List<Siglum> siglumVisible, String exercise, List<String> pendingSiglums, List<String> approvedSiglums, List<String> rejectedSiglums) {
        processStatuses(siglumVisible, exercise,
                List.of(Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_HR),
                List.of(
                        Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_6,
                        Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_5,
                        Constants.WORKLOAD_EVOLUTION_STATUS_REJECTED_BY_SIGLUM_5,
                        Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_4,
                        Constants.WORKLOAD_EVOLUTION_STATUS_QMC_REJECTED,
                        Constants.WORKLOAD_EVOLUTION_STATUS_QMC_APPROVED,
                        Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_REJECTED,
                        Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_APPROVED
                ),
                List.of(Constants.WORKLOAD_EVOLUTION_STATUS_REJECTED_BY_SIGLUM_6),
                pendingSiglums, approvedSiglums, rejectedSiglums);
    }

    private void handleSiglum5(List<Siglum> siglumVisible, String exercise, List<String> pendingSiglums, List<String> approvedSiglums, List<String> rejectedSiglums) {
        processStatuses(siglumVisible, exercise,
                List.of(
                        Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_HR,
                        Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_6,
                        Constants.WORKLOAD_EVOLUTION_STATUS_REJECTED_BY_SIGLUM_6
                ),
                List.of(
                        Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_5,
                        Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_4,
                        Constants.WORKLOAD_EVOLUTION_STATUS_QMC_REJECTED,
                        Constants.WORKLOAD_EVOLUTION_STATUS_QMC_APPROVED,
                        Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_REJECTED,
                        Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_APPROVED
                ),
                List.of(Constants.WORKLOAD_EVOLUTION_STATUS_REJECTED_BY_SIGLUM_5),
                pendingSiglums, approvedSiglums, rejectedSiglums);
    }

    private void handleSiglum4(List<Siglum> siglumVisible, String exercise, List<String> pendingSiglums, List<String> approvedSiglums) {
        processStatuses(siglumVisible, exercise,
                List.of(
                        Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_HR,
                        Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_6,
                        Constants.WORKLOAD_EVOLUTION_STATUS_REJECTED_BY_SIGLUM_6,
                        Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_5,
                        Constants.WORKLOAD_EVOLUTION_STATUS_REJECTED_BY_SIGLUM_5
                ),
                List.of(
                        Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_4,
                        Constants.WORKLOAD_EVOLUTION_STATUS_QMC_REJECTED,
                        Constants.WORKLOAD_EVOLUTION_STATUS_QMC_APPROVED,
                        Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_REJECTED,
                        Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_APPROVED
                ),
                List.of(),
                pendingSiglums, approvedSiglums, new ArrayList<>());
    }

    private void processAdminSiglumStatus(List<String> siglumHRList, List<Siglum> siglumVisible, WorkloadEvolution exercise, boolean isApproved) {
        for (String siglumHR : siglumHRList) {
            Siglum siglum = siglumVisible.stream()
                    .filter(s -> s.getSiglumHR().equals(siglumHR))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Siglum no encontrado: " + siglumHR));

            Optional<WorkloadEvolution> existingRecordOpt = workloadEvolutionRepository
                    .findBySiglumAndExerciseWithLatestSubmitDate(siglum, exercise.getExercise());

            String newStatus;
            String currentStatus = existingRecordOpt.get().getStatus();

            if (isApproved) {
                newStatus = switch (currentStatus) {
                    case Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_HR,
                            Constants.WORKLOAD_EVOLUTION_STATUS_REJECTED_BY_SIGLUM_6 ->
                            Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_6;
                    case Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_6,
                            Constants.WORKLOAD_EVOLUTION_STATUS_REJECTED_BY_SIGLUM_5 ->
                            Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_5;
                    case Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_5,
                            Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_4,
                            Constants.WORKLOAD_EVOLUTION_STATUS_QMC_REJECTED ->
                            Constants.WORKLOAD_EVOLUTION_STATUS_QMC_APPROVED;
                    case Constants.WORKLOAD_EVOLUTION_STATUS_QMC_APPROVED,
                            Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_APPROVED,
                            Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_PENDING,
                            Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_REJECTED ->
                            Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_APPROVED;
                    default -> throw new IllegalStateException("Estado no válido para aprobación: " + currentStatus);
                };
            } else {
                newStatus = switch (currentStatus) {
                    case Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_HR ->
                            Constants.WORKLOAD_EVOLUTION_STATUS_REJECTED_BY_SIGLUM_6;
                    case Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_6 ->
                            Constants.WORKLOAD_EVOLUTION_STATUS_REJECTED_BY_SIGLUM_5;
                    case Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_5,
                            Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_4 ->
                            Constants.WORKLOAD_EVOLUTION_STATUS_QMC_REJECTED;
                    case Constants.WORKLOAD_EVOLUTION_STATUS_QMC_APPROVED,
                            Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_REJECTED,
                            Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_PENDING ->
                            Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_REJECTED;
                    default -> throw new IllegalStateException("Estado no válido para rechazo: " + currentStatus);
                };
            }

        createAndSaveWorkloadEvolution(siglum, exercise.getExercise(), newStatus, false);
        deletePendingWorkloadEvolution(siglum, exercise.getExercise(), Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_PENDING);

        }
    }

    private void processSiglumStatus(List<String> siglumHRList, List<Siglum> siglumVisible, String roles, WorkloadEvolution exercise, boolean isApproved, String userSelected) {
        if (siglumHRList == null) {
            return;
        }

        String status;
        boolean isQmcApproved = false;
        boolean isQmcRejected = false;
        boolean isHoT1qApprovedOrRejected = false;
        boolean isHoSiglumApprobed = false;
        boolean isHoSiglumRejected = false;
        if (roles.toLowerCase().contains(Constants.USER_ROLE_QMC.toLowerCase())) {
            status = isApproved ? Constants.WORKLOAD_EVOLUTION_STATUS_QMC_APPROVED : Constants.WORKLOAD_EVOLUTION_STATUS_QMC_REJECTED;
            if (isApproved) {
             isQmcApproved = true;
            } else {
             isQmcRejected = true;
            }
        } else if (roles.toLowerCase().contains(Constants.USER_ROLE_HO_T1Q.toLowerCase())) {
            status = isApproved ? Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_APPROVED : Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_REJECTED;
            isHoT1qApprovedOrRejected = true;
        } else if (roles.toLowerCase().contains(Constants.USER_ROLE_HO_SIGLUM.toLowerCase())) {
            for (String siglumHR : siglumHRList) {
                Siglum siglum = siglumVisible.stream()
                        .filter(s -> s.getSiglumHR().equals(siglumHR))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Siglum no encontrado: " + siglumHR));

                String userSiglum = utils.getNumberSiglums(userSelected);

                if (isApproved) {
                    isHoSiglumApprobed = true;
                    status = switch (userSiglum) {
                        case Constants.USER_SIGLUM_HR -> Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_HR;
                        case Constants.USER_SIGLUM_6 -> Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_6;
                        case Constants.USER_SIGLUM_5 -> Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_5;
                        case Constants.USER_SIGLUM_4 -> Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_4;
                        default -> throw new IllegalArgumentException("Unsupported siglum for approval: " + userSiglum);
                    };
                } else {
                    isHoSiglumRejected = true;
                    status = switch (userSiglum) {
                        case Constants.USER_SIGLUM_6 -> Constants.WORKLOAD_EVOLUTION_STATUS_REJECTED_BY_SIGLUM_6;
                        case Constants.USER_SIGLUM_5 -> Constants.WORKLOAD_EVOLUTION_STATUS_REJECTED_BY_SIGLUM_5;
                        default -> throw new IllegalArgumentException("Unsupported siglum for rejection: " + userSiglum);
                    };
                }
                createAndSaveWorkloadEvolution(siglum, exercise.getExercise(), status, false);
                if (isHoSiglumApprobed && Constants.WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_5.equals(status)) {
                    createAndSaveWorkloadEvolution(siglum, exercise.getExercise(), Constants.WORKLOAD_EVOLUTION_STATUS_QMC_PENDING, false);
                }
                if (isHoSiglumRejected) {
                    deletePendingWorkloadEvolution(siglum, exercise.getExercise(), Constants.WORKLOAD_EVOLUTION_STATUS_QMC_PENDING);
                }
            }
            return;
        } else {
            throw new IllegalArgumentException("Unsupported user role: " + roles);
        }

        for (String siglumHR : siglumHRList) {
            Siglum siglum = siglumVisible.stream()
                    .filter(s -> s.getSiglumHR().equals(siglumHR))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Siglum no encontrado: " + siglumHR));

            createAndSaveWorkloadEvolution(siglum, exercise.getExercise(), status, false);
            if (isQmcApproved) {
                deletePendingWorkloadEvolution(siglum, exercise.getExercise(), Constants.WORKLOAD_EVOLUTION_STATUS_QMC_PENDING);
                createAndSaveWorkloadEvolution(siglum, exercise.getExercise(), Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_PENDING, false);
            }
            if (isQmcRejected) {
                deletePendingWorkloadEvolution(siglum, exercise.getExercise(), Constants.WORKLOAD_EVOLUTION_STATUS_QMC_PENDING);
            }
            if (isHoT1qApprovedOrRejected) {
                deletePendingWorkloadEvolution(siglum, exercise.getExercise(), Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_PENDING);
            }
        }
    }

    public CanUpdateWorkloadDTO canUpdateWorkload(String userSelected) {
        List<Siglum> editableSiglums = utils.getEditableSiglums(userSelected);
        return new CanUpdateWorkloadDTO(!editableSiglums.isEmpty());
    }

    public ExistsOpenedExerciseDTO existsOpenedExercise() {
        return utils.existsOpenedExercise();
    }
}