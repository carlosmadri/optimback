package com.airbus.optim.service;

import com.airbus.optim.dto.HomeTeamOutlookWorkloadDTO;
import com.airbus.optim.dto.IndirectRadioDTO;
import com.airbus.optim.dto.OwnRatioDTO;
import com.airbus.optim.dto.TeamOutlookDTO;
import com.airbus.optim.dto.EvolutionDTO;
import com.airbus.optim.dto.WorkloadFteDTO;
import com.airbus.optim.dto.WorkloadFteKhrsDTO;
import com.airbus.optim.dto.WorkloadMonthlyDistributionExerciseDTO;
import com.airbus.optim.dto.WorkloadPerProgramDTO;
import com.airbus.optim.dto.WorkloadPreviewDTO;
import com.airbus.optim.dto.WorkloadWorkforceDTO;
import com.airbus.optim.entity.CostCenter;
import com.airbus.optim.entity.PPSID;
import com.airbus.optim.entity.Siglum;
import com.airbus.optim.entity.Workload;
import com.airbus.optim.entity.WorkloadEvolution;
import com.airbus.optim.repository.CostCenterRepository;
import com.airbus.optim.repository.HeadCountRepository;
import com.airbus.optim.repository.PPSIDRepository;
import com.airbus.optim.repository.SiglumRepository;
import com.airbus.optim.repository.WorkloadEvolutionRepository;
import com.airbus.optim.repository.WorkloadRepository;
import com.airbus.optim.service.workloadImpl.WorkloadMontlyDistributionImpl;
import com.airbus.optim.utils.Constants;
import com.airbus.optim.utils.Utils;
import com.airbus.optim.service.workloadImpl.WorkloadUtils;
import jakarta.persistence.EntityNotFoundException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

import jakarta.persistence.criteria.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

@Service
public class WorkloadService {

    @Autowired
    private WorkloadRepository workloadRepository;

    @Autowired
    private WorkloadEvolutionRepository workloadEvolutionRepository;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private SiglumRepository siglumRepository;

    @Autowired
    private CostCenterRepository costCenterRepository;

    @Autowired
    private PPSIDRepository ppsidRepository;

    @Autowired
    private WorkloadSpecification workloadSpecification;

    @Autowired
    private WorkloadEvolutionSpecification workloadEvolutionSpecification;

    @Autowired
    WorkloadMontlyDistributionImpl workloadMontlyDistributionImpl;

    @Autowired
    private HeadCountRepository headCountRepository;

    @Autowired
    WorkloadUtils workloadUtils;

    @Autowired
    private Utils utils;
    
    @Autowired
    private WorkloadEvolutionBarGraphicService workloadEvolutionBarGraphicService;

    private LocalDate convertInstantToLocalDate(Instant instant) {
        LocalDate localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        return localDate;
    }

    public Workload createWorkload(Workload newWorkload) {
        handleSiglum(newWorkload);
        handleCostCenter(newWorkload);
        handlePpsidCreationOrUpdate(newWorkload);
        //default value
        if (newWorkload.getExercise() == null || newWorkload.getExercise().isEmpty()) {
            newWorkload.setExercise(Constants.WORKLOAD_STATUS_BOTTOM_UP);
        }
        return workloadRepository.save(newWorkload);
    }

    @Transactional
    public Workload updateWorkload(Long id, Workload workloadDetails) {
        if (id == null) {
            throw new EntityNotFoundException("Workload ID cannot be null.");
        }

        Workload existingWorkload = workloadRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Workload not found with ID: " + id));

        validateLastWorkloadEvolution(existingWorkload.getSiglum());

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

                    validateLastWorkloadEvolution(existingWorkload.getSiglum());

                    updateWorkloadFields(existingWorkload, workload);
                    handleSiglum(existingWorkload, workload);
                    handleCostCenter(existingWorkload, workload);
                    handlePpsidCreationOrUpdate(existingWorkload, workload);

                    return workloadRepository.save(existingWorkload);
                })
                .collect(Collectors.toList());
    }

    private void validateLastWorkloadEvolution(Siglum siglum) {
        if (siglum == null) {
            throw new IllegalArgumentException("El Siglum del workload no puede ser nulo.");
        }

        WorkloadEvolution lastOpenedExercise = utils.getLastOpenedExerciseOrThrow();
        Optional<WorkloadEvolution> latestWorkloadEvolution = workloadEvolutionRepository.findBySiglumAndExerciseWithLatestSubmitDate(
                siglum,
                lastOpenedExercise.getExercise()
        );

        if (latestWorkloadEvolution.isEmpty()) {
            throw new IllegalArgumentException("No se encontró ningún registro en WorkloadEvolution para el siglum: " + siglum.getSiglumHR());
        }

        String status = latestWorkloadEvolution.get().getStatus();
        if (!status.toLowerCase().contains(Constants.STATUS_REJECTED.toLowerCase()) &&
                !status.toLowerCase().contains((Constants.WORKLOAD_EVOLUTION_STATUS_OPENED.toLowerCase()))) {
            throw new IllegalArgumentException("El último registro en WorkloadEvolution para el siglum "
                    + siglum.getSiglumHR() + " no tiene un estado válido ('rejected' o 'opened').");
        }
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

    public HomeTeamOutlookWorkloadDTO getHomeTeamOutlookWorkloadData(
            MultiValueMap<String, String> params,
            List<Siglum> siglumList,
            String userSelected,
            int yearFilter) {

        Specification<Workload> spec = workloadSpecification.getSpecifications(params);
        List<Workload> workloadList = workloadRepository.findAll(spec);

        List<Workload> lastExerciseList = workloadUtils.getLastExercise(
                Constants.WORKLOAD_EVOLUTION_STATUS_CLOSED, userSelected, workloadList);
        String lastExerciseName = (!lastExerciseList.isEmpty()) ? lastExerciseList.get(0).getExercise() : "";
        double LastExerciseKhrs = workloadRepository.getKhrsByExercise(lastExerciseList, yearFilter);

        List<Siglum> siglumFiltered = utils.getVisibleSiglums("", userSelected);
        List<String> selectedSiglumHRs = Optional.ofNullable(params.get("siglum.siglumHR")).orElse(null);
        List<Siglum> siglumsSelected = Optional.ofNullable(selectedSiglumHRs)
                .map(siglumHRs -> siglumFiltered.stream()
                        .filter(siglum -> siglumHRs.contains(siglum.getSiglumHR()))
                        .collect(Collectors.toList()))
                .orElse(siglumFiltered);

        Float hcFormerRefference = 0.0F;

            Float fte = headCountRepository.sumTotalFTEForCurrentYearExercise(String.valueOf(yearFilter), "old",siglumsSelected);
            hcFormerRefference = Optional.ofNullable(fte).orElse(0.0F);

        return new HomeTeamOutlookWorkloadDTO(
                workloadRepository.getKhrsByExerciseEditionByYear(yearFilter),
                lastExerciseName,
                LastExerciseKhrs,
                (hcFormerRefference != null ? hcFormerRefference : 0.0f)
        );
    }

    public IndirectRadioDTO getIndirectRatio(
            MultiValueMap<String, String> params,
            List<Siglum> siglumList,
            String userSelected,
            int yearFilter) {

        Specification<Workload> spec = workloadSpecification.getSpecifications(params);
        List<Workload> workloadList = workloadRepository.findAll(spec);

        List<Workload> wL = workloadRepository.getIndirectRatioForBottomUp(
                workloadList, siglumList, Constants.WORKLOAD_STATUS_BOTTOM_UP, yearFilter);

        if (wL.isEmpty()) {
            wL = workloadRepository.getIndirectRatioForLastExercise(workloadUtils.getLastExerciseByYear(
                    Constants.WORKLOAD_EVOLUTION_STATUS_CLOSED, userSelected, workloadList, yearFilter), yearFilter);
        }

        double directKhrs = wL.stream()
                .filter(employee -> "Direct".equalsIgnoreCase(employee.getDirect()))
                .mapToDouble(employee -> {
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

                    return proportionalKHrs;
                })
                .sum();

        double indirectKhrs = wL.stream()
                .filter(employee -> "Indirect".equalsIgnoreCase(employee.getDirect()))
                .mapToDouble(employee -> {
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

                    return proportionalKHrs;
                })
                .sum();

        return new IndirectRadioDTO(directKhrs, indirectKhrs);
    }

    public OwnRatioDTO getOwnRatio(
            MultiValueMap<String, String> params,
            List<Siglum> siglumList,
            String userSelected,
            int yearFilter) {

        Specification<Workload> spec = workloadSpecification.getSpecifications(params);
        List<Workload> workloadList = workloadRepository.findAll(spec);

        List<Workload> wL = workloadRepository.getOwnRatioForBottomUp(
                workloadList, siglumList, Constants.WORKLOAD_STATUS_BOTTOM_UP, yearFilter);

        if (wL.isEmpty()) {
            wL = workloadRepository.getOwnRatioForLastExercise(workloadUtils.getLastExerciseByYear(
                    Constants.WORKLOAD_EVOLUTION_STATUS_CLOSED, userSelected, workloadList, yearFilter), yearFilter);
        }

        double ownKhrs = wL.stream()
                .filter(employee -> "OWN".equalsIgnoreCase(employee.getOwn()))
                .mapToDouble(employee -> {
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

                    return proportionalKHrs;
                })
                .sum();

        double subKhrs = wL.stream()
                .filter(employee -> "SUB".equalsIgnoreCase(employee.getOwn()))
                .mapToDouble(employee -> {
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

                    return proportionalKHrs;
                })
                .sum();

        return new OwnRatioDTO(ownKhrs, subKhrs);
    }

    public List<WorkloadPerProgramDTO> getWorkloadPerProgram(
            MultiValueMap<String, String> params,
            List<Siglum> siglumList,
            String userSelected,
            int yearFilter) {

        Specification<Workload> spec = workloadSpecification.getSpecifications(params);
        List<Workload> workloadList = workloadRepository.findAll(spec);

        workloadList.forEach(employee -> {
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
            employee.setKHrs(proportionalKHrs);
        });

        List<WorkloadPerProgramDTO> workloadPerProgramDTOs =
                workloadRepository.getWorkloadPerProgram(
                        workloadList, siglumList, Constants.WORKLOAD_STATUS_BOTTOM_UP, yearFilter);

        Map<String, WorkloadPerProgramDTO> programMap = new HashMap<>();
        for (WorkloadPerProgramDTO dto : workloadPerProgramDTOs) {
            programMap.put(dto.getProgramName(), new WorkloadPerProgramDTO(dto.getProgramName(), 0, 0));
        }

        for (Workload employee : workloadList) {
            PPSID ppsid = employee.getPpsid();
            if (ppsid != null && "Nominal".equalsIgnoreCase(ppsid.getScenario()) && "BU".equalsIgnoreCase(employee.getExercise())) {
                String programName = ppsid.getProgramLine();
                if (programMap.containsKey(programName)) {
                    WorkloadPerProgramDTO dto = programMap.get(programName);
                    dto.setProgramKHrsSum(dto.getProgramKHrsSum() + employee.getKHrs());
                    dto.setProgramsCount(dto.getProgramsCount() + 1);
                }
            }
        }

        List<WorkloadPerProgramDTO> finalWorkloadPerProgramDTOs = new ArrayList<>(programMap.values());

        if (finalWorkloadPerProgramDTOs.isEmpty()) {
            List<Workload> wL = workloadUtils.getLastExerciseByYear(
                    Constants.WORKLOAD_EVOLUTION_STATUS_CLOSED, userSelected, workloadList, yearFilter);

            finalWorkloadPerProgramDTOs = workloadRepository.getPerProgramLastExercise(
                    workloadUtils.getIdsFromWorkloadList(wL), yearFilter);
        }

        return finalWorkloadPerProgramDTOs;
    }

    public WorkloadMonthlyDistributionExerciseDTO workloadMontlyDistribution(
            MultiValueMap<String, String> params,
            List<Siglum> siglumList,
            int yearFilter) {
        Specification<Workload> spec = workloadSpecification.getSpecifications(params);
        List<Workload> workloadList = workloadRepository.findAll(spec);

        String workloadWIP = workloadRepository.getWorkloadWIP(siglumList) != null ? workloadRepository.getWorkloadWIP(siglumList) : "";
        EvolutionDTO w = new EvolutionDTO(workloadWIP);
        w.mapper();

        return new WorkloadMonthlyDistributionExerciseDTO(
                workloadUtils.fillMontlyDistribution(
                        workloadMontlyDistributionImpl.getMontlyDistributionByExercise(
                                workloadList, siglumList, yearFilter, utils.getExerciseOP(yearFilter))),
                workloadUtils.fillMontlyDistribution(
                        workloadMontlyDistributionImpl.getMontlyDistributionByExercise(
                                workloadList, siglumList, yearFilter, Constants.EXERCISE_FORECAST)),
                workloadUtils.fillMontlyDistribution(
                        workloadMontlyDistributionImpl.getMontlyDistributionByExercise(
                                workloadList, siglumList, yearFilter, Constants.WORKLOAD_STATUS_BOTTOM_UP)),
                w.getExerciseName()
        );
    }

    
    public WorkloadWorkforceDTO getWorkloadWorkforce(MultiValueMap<String, String> params, List<Siglum> siglumList, int yearFilter, String validationStatus, String userSelected) {
        boolean fciiFirst = false;
        boolean foundNullId = false; 

        Specification<Workload> spec = workloadSpecification.getSpecifications(params);
        List<Workload> workloadList = workloadRepository.findAll(spec);
        
        List<Siglum> siglumFiltered = utils.getVisibleSiglums(null, userSelected);
        List<WorkloadEvolution> workloadEvolutionList = workloadEvolutionRepository.findAll(workloadEvolutionSpecification.getSpecifications(params));
        List<String> selectedSiglumHRs = Optional.ofNullable(params.get("siglum.siglumHR")).orElse(null);
        List<Siglum> siglumsSelected = Optional.ofNullable(selectedSiglumHRs)
            .map(siglumHRs -> siglumFiltered.stream()
                .filter(siglum -> siglumHRs.contains(siglum.getSiglumHR()))
                .collect(Collectors.toList()))
            .orElse(siglumFiltered);

        List<EvolutionDTO> barDTOList = new LinkedList<>();

        workloadEvolutionBarGraphicService.processWorkloads(barDTOList, workloadEvolutionRepository.findLatestClosedExercisesByType(), siglumFiltered, workloadList,true);
        workloadEvolutionBarGraphicService.orderByLowId(barDTOList);
        workloadEvolutionBarGraphicService.processLatestExerciseWorkloads(barDTOList, validationStatus, workloadEvolutionList, siglumFiltered, false);
        workloadEvolutionBarGraphicService.processWorkloads(barDTOList, List.of(Constants.WORKLOAD_STATUS_BOTTOM_UP), siglumFiltered, workloadList,false);

        if (barDTOList != null) {
            boolean hasNullId = barDTOList.stream().anyMatch(e -> e.getId() == null);
            if (hasNullId) {
                fciiFirst = true;
            } else {
                if (barDTOList.get(0).getExercise().contains("FCII")) {
                	fciiFirst = true;
                } 
  
            }
            
            barDTOList.sort(Comparator.comparing(EvolutionDTO::getId, Comparator.nullsFirst(Comparator.naturalOrder())));
        }

        double fciiValue = 0.0;
        double opValue = 0.0;
        double firstSubmissionValue = 0.0;
        double qmcValue = 0.0;
        double hott1q = 0.0;
        double wipValue = 0.0;

        for (EvolutionDTO evolution : barDTOList) {
            double kHrsOwnDirect = Optional.ofNullable(evolution.getKHrsOwnDirect()).orElse(0.0);
            double kHrsOwnIndirect = Optional.ofNullable(evolution.getKHrsOwnIndirect()).orElse(0.0);
            double kHrsSubDirect = Optional.ofNullable(evolution.getKHrsSubDirect()).orElse(0.0);
            double kHrsSubIndirect = Optional.ofNullable(evolution.getKHrsSubIndirect()).orElse(0.0);
            double totalKhrs = kHrsOwnDirect + kHrsOwnIndirect + kHrsSubDirect + kHrsSubIndirect;

            if (evolution.getExercise().contains("FCII")) {
                fciiValue += totalKhrs;
            } else if (evolution.getExercise().contains("OP")) {
                opValue += totalKhrs;
            } else if ("First Submission".equals(evolution.getExercise())) {
                firstSubmissionValue = BigDecimal.valueOf(totalKhrs).setScale(1, RoundingMode.HALF_UP).doubleValue();
            } else if ("QMC".equals(evolution.getExercise())) {
                qmcValue = BigDecimal.valueOf(totalKhrs).setScale(1, RoundingMode.HALF_UP).doubleValue();
            } else if ("HOT1Q".equals(evolution.getExercise())) {
                hott1q = BigDecimal.valueOf(totalKhrs).setScale(1, RoundingMode.HALF_UP).doubleValue();
            } else if (evolution.getExercise().contains("BU")) {
                wipValue = BigDecimal.valueOf(totalKhrs).setScale(1, RoundingMode.HALF_UP).doubleValue();
            }
        }

        fciiValue = BigDecimal.valueOf(fciiValue).setScale(1, RoundingMode.HALF_UP).doubleValue();
        opValue = BigDecimal.valueOf(opValue).setScale(1, RoundingMode.HALF_UP).doubleValue();

        if (fciiFirst) {
            double temp = fciiValue;
            fciiValue = opValue;
            opValue = temp;
        }

        ResponseEntity<TeamOutlookDTO> teamOutlookDTO = employeeService.getTeamOutlook(params, siglumList, userSelected, yearFilter);


        WorkloadWorkforceDTO workloadWorkforce = new WorkloadWorkforceDTO(
                fciiValue, opValue, firstSubmissionValue, wipValue,
                qmcValue, hott1q, Objects.requireNonNull(teamOutlookDTO.getBody()).getOptimisticView(),
                teamOutlookDTO.getBody().getValidationView(), teamOutlookDTO.getBody().getRealisticView(),
                teamOutlookDTO.getBody().getHcCeiling(), "", fciiFirst);

        workloadWorkforce.buildWIPwhenRejected();

        return workloadWorkforce;
    }

    public WorkloadPreviewDTO getWorkloadPreview(
            MultiValueMap<String, String> params,
            List<Siglum> siglumList,
            String userSelected,
            int yearFilter) {

        Specification<Workload> spec = workloadSpecification.getSpecifications(params);
        List<Workload> workloadList = workloadRepository.findAll(spec);

        List<Workload> wLastExercise = workloadUtils.getLastExercise(
                Constants.WORKLOAD_EVOLUTION_STATUS_CLOSED, userSelected, workloadList);

        String lastExercise = (!wLastExercise.isEmpty()) ? wLastExercise.get(0).getExercise() : "";

        WorkloadFteKhrsDTO workloadFteKhrsExercise = workloadUtils.fteFromWorkload(
                wLastExercise, workloadRepository.workloadFTEbyLastExercise(wLastExercise, yearFilter), yearFilter);

        List<Workload> wLastBU = workloadUtils.getLastExercise(
                Constants.WORKLOAD_STATUS_BOTTOM_UP, userSelected, workloadList);

        List<WorkloadFteDTO> wList = workloadRepository.workloadFTEbyLastExercise(wLastBU, yearFilter);
        WorkloadFteKhrsDTO workloadFteKhrsPlanification = workloadUtils.fteFromWorkload(wLastBU, wList, yearFilter);

        ResponseEntity<TeamOutlookDTO> teamOutlookDTO =
                employeeService.getTeamOutlook(params, siglumList, userSelected, yearFilter);

        double endOfYear = 0.0;
        if (!wLastBU.isEmpty()) {
            endOfYear = workloadUtils.filterWorkloadByRatio(wLastBU, Constants.WORKLOAD_STATUS_DIRECT);
        } else {
            endOfYear = workloadUtils.filterWorkloadByRatio(wLastExercise, Constants.WORKLOAD_STATUS_DIRECT);
        }

        return new WorkloadPreviewDTO(
                lastExercise,
                workloadFteKhrsExercise.getKHrs(),
                workloadFteKhrsExercise.getFte(),
                workloadFteKhrsPlanification.getKHrs(),
                workloadFteKhrsPlanification.getFte(),
                endOfYear,
                Objects.requireNonNull(teamOutlookDTO.getBody()).getRealisticViewAverage(),
                teamOutlookDTO.getBody().getHcCeiling());
    }

    public Page<Workload> filterWorkloads(MultiValueMap<String, String> params, Pageable pageable, String userSelected) {
        List<Siglum> visibleSiglums = utils.getVisibleSiglums(null, userSelected);
        Siglum mySiglum = utils.getUserInSession(userSelected).getSiglum();

        if (visibleSiglums.isEmpty()) {
            return Page.empty(pageable);
        }

        boolean isSuperUser = utils.isSuperUser(userSelected);
        List<Siglum> filteredSiglums = visibleSiglums;

        Specification<Workload> spec;
        String exercise = null;

        try {
            utils.getLastOpenedExerciseOrThrow();
            exercise = Constants.WORKLOAD_STATUS_BOTTOM_UP;

            String finalExercise = exercise;
            spec = Specification.where((root, query, criteriaBuilder) -> {
                Path<Siglum> siglumPath = root.get("siglum");
                Path<String> exercisePath = root.get("exercise");

                return criteriaBuilder.and(
                        siglumPath.in(filteredSiglums),
                        criteriaBuilder.equal(exercisePath, finalExercise)
                );
            });

        } catch (Exception e) {
            exercise = workloadEvolutionRepository.findLatestClosedExercisesByType()
                    .stream()
                    .findFirst()
                    .orElse("");

            if (exercise == null) {
                return Page.empty(pageable);
            }

            String finalExercise1 = exercise;
            spec = Specification.where((root, query, criteriaBuilder) -> {
                Path<Siglum> siglumPath = root.get("siglum");
                Path<String> exercisePath = root.get("exercise");

                return criteriaBuilder.and(
                        siglumPath.in(filteredSiglums),
                        criteriaBuilder.equal(exercisePath, finalExercise1)
                );
            });
        }

        Specification<Workload> dynamicSpec = workloadSpecification.getSpecifications(params);
        spec = spec.and(dynamicSpec);

        Page<Workload> workloadsPage = workloadRepository.findAll(spec, pageable);

        Optional<WorkloadEvolution> lastStatusMySiglum = workloadEvolutionRepository.findBySiglumAndExerciseWithLatestSubmitDate(mySiglum, exercise);

        List<Workload> workloads = workloadsPage.getContent().stream()
                .peek(workload -> {

                    if (workload.getKHrs() != null && workload.getCostCenter() != null && workload.getCostCenter().getEfficiency() != null) {
                        double fte = (workload.getKHrs() * 1000 / workload.getCostCenter().getEfficiency());
                        workload.setFTE((Math.round(fte * 10.0) / 10.0));
                    }

                    if (workload.getKHrs() != null && workload.getCostCenter() != null) {
                        if (workload.getOwn().equalsIgnoreCase("OWN")) {
                            double kEurEquivalent = workload.getKHrs() * workload.getCostCenter().getRateOwn();
                            workload.setKEur(Math.round(kEurEquivalent * 10.0) / 10.0);
                        } else {
                            double kEurEquivalent = workload.getKHrs() * workload.getCostCenter().getRateSub();
                            workload.setKEur(Math.round(kEurEquivalent * 10.0) / 10.0);
                        }
                    }

                    if (isSuperUser) {
                        workload.setReadOnly(false);
                    } else {
                        boolean isMySiglum = workload.getSiglum() != null && workload.getSiglum().equals(mySiglum);
                        boolean isStatusInvalid = lastStatusMySiglum.isPresent() &&
                                !lastStatusMySiglum.get().getStatus().toLowerCase().contains(Constants.STATUS_REJECTED.toLowerCase()) &&
                                !lastStatusMySiglum.get().getStatus().toLowerCase().contains(Constants.STATUS_OPENED.toLowerCase());
                        workload.setReadOnly(!isMySiglum || isStatusInvalid);
                    }
                })
                .sorted((w1, w2) -> {
                    if (!isSuperUser) {
                        return Boolean.compare(w1.getReadOnly(), w2.getReadOnly());
                    }
                    return 0;
                })
                .toList();

        return new PageImpl<>(workloads, pageable, workloadsPage.getTotalElements());
    }

}