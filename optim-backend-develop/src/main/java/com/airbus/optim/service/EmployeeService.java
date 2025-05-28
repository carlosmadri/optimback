package com.airbus.optim.service;

import com.airbus.optim.dto.*;
import com.airbus.optim.dto.ReportEndOfYear.ActiveWorkforceReportCapacityDTO;
import com.airbus.optim.dto.ReportEndOfYear.ActiveWorkforceReportDTO;
import com.airbus.optim.dto.ReportEndOfYear.ReportEndOfYearDTO;
import com.airbus.optim.entity.*;
import com.airbus.optim.exception.LeverConflictException;
import com.airbus.optim.repository.*;
import com.airbus.optim.repository.projections.LeverTypeFteSum;
import com.airbus.optim.service.EmployeeImpl.EmployeeActiveWorkforceCapacityReport;
import com.airbus.optim.service.EmployeeImpl.EmployeeWorkforceReport;
import com.airbus.optim.service.workloadImpl.WorkloadUtils;
import com.airbus.optim.utils.Constants;
import com.airbus.optim.utils.Utils;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.util.List;
import org.springframework.util.MultiValueMap;

@Service
@Slf4j
public class EmployeeService {

    @Autowired
    private LeverRepository leverRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private JobRequestRepository jobRequestRepository;

    @Autowired
    private JobRequestService jobRequestService;

    @Autowired
    private SiglumRepository siglumRepository;

    @Autowired
    private CostCenterRepository costCenterRepository;

    @Autowired
    private HeadCountRepository headCountRepository;

    @Autowired
    private EmployeeSpecification employeeSpecification;

    @Autowired
    private JobRequestSpecification jobRequestSpecification;

    @Autowired
    @Lazy
    private WorkloadService workloadService;

    @Autowired
    EmployeeActiveWorkforceCapacityReport employeeReportImpl;

    @Autowired
    EmployeeWorkforceReport employeeWorkforceReport;

    @Autowired
    private WorkloadSpecification workloadSpecification;

    @Autowired
    private WorkloadEvolutionRepository workloadEvolutionRepository;

    @Autowired
    private WorkloadRepository workloadRepository;

    @Autowired
    WorkloadUtils workloadUtils;

    @Autowired
    private Utils utils;

    public List<Map<String, Object>> getSumOfLeversGroupedByType(MultiValueMap<String, String> params) {
        Specification<Employee> employeeSpec = employeeSpecification.getSpecifications(params);
        List<Employee> employees = employeeRepository.findAll(employeeSpec);

        LocalDate currentDate = LocalDate.now();
        Instant currentDateInstant = currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfYear = LocalDate.of(currentDate.getYear() + 1, 1, 1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<LeverTypeFteSum> leaverSums = leverRepository.sumLeaversGroupedByType(currentDateInstant, endOfYear, employees);
        List<LeverTypeFteSum> recoverySums = leverRepository.sumRecoveriesGroupedByType(currentDateInstant, endOfYear, employees);

        Map<String, Map<String, Double>> results = new HashMap<>();

        leaverSums.forEach(leaver -> {
            String leverType = leaver.getLeverType() != null ? utils.capitalize(leaver.getLeverType()) : "Unknown";
            results.computeIfAbsent(leverType, k -> new HashMap<>()).put("leaver", leaver.getFteSum() != null ? leaver.getFteSum() : 0.0);
        });

        recoverySums.forEach(recovery -> {
            String leverType = recovery.getLeverType() != null ? utils.capitalize(recovery.getLeverType()) : "Unknown";
            results.computeIfAbsent(leverType, k -> new HashMap<>()).put("recovery", recovery.getFteSum() != null ? recovery.getFteSum() : 0.0);
        });

        return results.entrySet().stream()
                .filter(entry -> !entry.getKey().equalsIgnoreCase("Redeployment") && !entry.getKey().equalsIgnoreCase("Perimeter Change"))
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("leverType", entry.getKey());
                    map.put("leaver", entry.getValue().getOrDefault("leaver", 0.0));
                    map.put("recovery", entry.getValue().getOrDefault("recovery", 0.0));
                    return map;
                })
                .collect(Collectors.toList());
    }

    public Page<Employee> filterEmployees(MultiValueMap<String, String> params, Pageable pageable) {
        Specification<Employee> spec = employeeSpecification.getSpecifications(params);

        Pageable pageableWithoutSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());

        Page<Employee> employeePage = employeeRepository.findAll(spec, pageableWithoutSort);

        List<Sort.Order> orders = pageable.getSort().toList();

        List<Employee> sortedEmployees = employeePage.getContent().stream()
                .sorted((e1, e2) -> {
                    for (Sort.Order order : orders) {
                        String property = order.getProperty();
                        boolean ascending = order.isAscending();

                        Object value1 = getPropertyValue(e1, property);
                        Object value2 = getPropertyValue(e2, property);

                        if (value1 instanceof Comparable && value2 instanceof Comparable) {
                            int comparisonResult = ((Comparable) value1).compareTo(value2);
                            if (comparisonResult != 0) {
                                return ascending ? comparisonResult : -comparisonResult;
                            }
                        }
                    }
                    return 0;
                })
                .collect(Collectors.toList());

        return new PageImpl<>(sortedEmployees, pageable, employeePage.getTotalElements());
    }

    private Object getPropertyValue(Employee employee, String propertyPath) {
        try {
            String[] properties = propertyPath.split("\\.");
            Object value = employee;

            for (String property : properties) {
                if (value == null) return null;

                Field field = value.getClass().getDeclaredField(property);
                field.setAccessible(true);
                value = field.get(value);
            }

            return value;
        } catch (Exception e) {
            return null;
        }
    }

    public Employee saveEmployee(Employee newEmployee) {
        if(newEmployee.getLevers().size() >= 2) hasDuplicateLevers(newEmployee);

        handleCostCenter(newEmployee);
        handleSiglums(newEmployee);
        handleJobRequest(newEmployee);
        handleLevers(newEmployee);

        if (newEmployee.getImpersonal()) setImpersonalEmployee(newEmployee);

        newEmployee.setId(employeeRepository.findNextAvailableId());
        return employeeRepository.save(newEmployee);
    }

    public Employee updateEmployee(Long id, Employee updatedEmployee) {
        if (id == null) {
            throw new EntityNotFoundException("Employee not found: " + id);
        }

        Employee existingEmployee = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found: " + id));

        updatedEmployee.setId(id);

        updateEmployeeDetails(existingEmployee, updatedEmployee);
        hasDuplicateLevers(existingEmployee, updatedEmployee);
        handleCostCenter(existingEmployee, updatedEmployee);
        handleSiglums(existingEmployee, updatedEmployee);
        handleJobRequest(existingEmployee, updatedEmployee);
        handleLevers(existingEmployee, updatedEmployee);

        if (updatedEmployee.getImpersonal()) {
            setImpersonalEmployee(existingEmployee);
        }

        return employeeRepository.save(existingEmployee);
    }

    private void hasDuplicateLevers(Employee existingEmployee, Employee updatedEmployee) {
        boolean hasDuplicate = existingEmployee.getLevers().stream()
                .anyMatch(existingLever -> utils.checkOverlapOfLevers(existingLever, updatedEmployee.getId()));

        if (hasDuplicate) {
            throw new LeverConflictException("Levers duplicated in time");
        }
    }

    private void hasDuplicateLevers(Employee employee) {
        List<Lever> levers = employee.getLevers();
        boolean hasDuplicate = levers.stream()
                .anyMatch(lever -> levers.stream()
                        .filter(otherLever -> !otherLever.equals(lever))
                        .anyMatch(otherLever -> utils.checkOverlapOfLevers(lever, otherLever.getId()))
                );

        if (hasDuplicate) {
            throw new LeverConflictException("Levers duplicated in time");
        }
    }


    private void setImpersonalEmployee(Employee newEmployee) {
        newEmployee.setFirstName("Impersonal");
        newEmployee.setImpersonal(true);
    }

    private void handleCostCenter(Employee newEmployee) {
        CostCenter costCenter = newEmployee.getCostCenter();
        if (costCenter != null && costCenter.getId() != null) {
            newEmployee.setCostCenter(
                    costCenterRepository.findById(costCenter.getId())
                            .orElseThrow(() -> new EntityNotFoundException("CostCenter not found: " + costCenter.getId()))
            );
        } else {
            newEmployee.setCostCenter(null);
        }
    }

    private void handleCostCenter(Employee existingEmployee, Employee updatedEmployee) {
        existingEmployee.setCostCenter(
                updatedEmployee.getCostCenter() != null && updatedEmployee.getCostCenter().getId() != null
                        ? costCenterRepository.findById(updatedEmployee.getCostCenter().getId())
                        .orElseThrow(() -> new EntityNotFoundException("CostCenter not found: " + updatedEmployee.getCostCenter().getId()))
                        : null
        );
    }

    private void handleSiglums(Employee newEmployee) {
        newEmployee.setSiglum(
                newEmployee.getSiglum() != null && newEmployee.getSiglum().getId() != null
                        ? siglumRepository.findById(newEmployee.getSiglum().getId())
                        .orElseThrow(() -> new EntityNotFoundException("Siglum not found: " + newEmployee.getSiglum().getId()))
                        : null
        );
    }

    private void handleSiglums(Employee existingEmployee, Employee updatedEmployee) {
        existingEmployee.setSiglum(
                updatedEmployee.getSiglum() != null && updatedEmployee.getSiglum().getId() != null
                        ? siglumRepository.findById(updatedEmployee.getSiglum().getId())
                        .orElseThrow(() -> new EntityNotFoundException("Siglum not found: " + updatedEmployee.getSiglum().getId()))
                        : null
        );
    }

    private void handleJobRequest(Employee newEmployee) {
        JobRequest jobRequest = null;
        if (newEmployee.getJobRequest() != null) {
            jobRequest = (newEmployee.getJobRequest().getId() != null)
                    ? jobRequestService.updateJobRequest(newEmployee.getJobRequest().getId(), newEmployee.getJobRequest())
                    : jobRequestService.createJobRequest(newEmployee.getJobRequest());
        }
        newEmployee.setJobRequest(jobRequest);
    }

    private void handleJobRequest(Employee existingEmployee, Employee updatedEmployee) {
        JobRequest jobRequest = null;
        if (updatedEmployee.getJobRequest() != null) {
            jobRequest = (updatedEmployee.getJobRequest().getId() != null)
                    ? jobRequestService.updateJobRequest(updatedEmployee.getJobRequest().getId(), updatedEmployee.getJobRequest())
                    : jobRequestService.createJobRequest(updatedEmployee.getJobRequest());
        }
        existingEmployee.setJobRequest(jobRequest);
    }


    private void handleLevers(Employee newEmployee) {
        Set<Lever> updatedLevers = Optional.ofNullable(newEmployee.getLevers())
                .map(levers -> levers.stream()
                        .map(lever -> lever.getId() != null
                                ? leverRepository.findById(lever.getId())
                                .orElseThrow(() -> new EntityNotFoundException("Lever not found: " + lever.getId()))
                                : createAndAssignLeverToEmployee(lever, newEmployee))
                        .collect(Collectors.toSet()))
                .orElse(null);

        newEmployee.setLevers(updatedLevers != null ? new ArrayList<>(updatedLevers) : null);
    }

    private Lever createAndAssignLeverToEmployee(Lever lever, Employee employee) {
        lever.setEmployee(employee);
        return leverRepository.save(lever);
    }


    private void handleLevers(Employee existingEmployee, Employee updatedEmployee) {
        Set<Lever> updatedLevers = Optional.ofNullable(updatedEmployee.getLevers())
                .filter(levers -> !levers.isEmpty())
                .map(levers -> levers.stream()
                        .map(lever -> {
                            if (lever.getId() != null) {
                                return leverRepository.findById(lever.getId())
                                        .orElseThrow(() -> new EntityNotFoundException("Lever not found: " + lever.getId()));
                            } else {
                                lever.setEmployee(existingEmployee);
                                return leverRepository.save(lever);
                            }
                        })
                        .collect(Collectors.toSet()))
                .orElse(null);

        existingEmployee.setLevers(updatedLevers != null ? new ArrayList<>(updatedLevers) : null);
    }

    private void updateEmployeeDetails(Employee existingEmployee, Employee updatedEmployee) {
        existingEmployee.setEmployeeId(updatedEmployee.getEmployeeId());
        existingEmployee.setDirect(updatedEmployee.getDirect());
        existingEmployee.setJob(updatedEmployee.getJob());
        existingEmployee.setCollar(updatedEmployee.getCollar());
        existingEmployee.setLastName(updatedEmployee.getLastName());
        existingEmployee.setFirstName(updatedEmployee.getFirstName());
        existingEmployee.setActiveWorkforce(updatedEmployee.getActiveWorkforce());
        existingEmployee.setAvailabilityReason(updatedEmployee.getAvailabilityReason());
        existingEmployee.setContractType(updatedEmployee.getContractType());
        existingEmployee.setFTE(updatedEmployee.getFTE());
    }

    public IndirectRadioDTO getIndirectRadio(
            MultiValueMap<String, String> params, List<Siglum> siglumVisibilityList) {

        Specification<Employee> spec = employeeSpecification.getSpecifications(params);
        List<Employee> employees = employeeRepository.findAll(spec);

        List<Employee> employeesFiltered =
                employeeRepository.getEmployeesFilteredBySiglumVisibility(employees, siglumVisibilityList);

        double directFteSum = employeesFiltered.stream()
                .filter(employee -> ("AWF".equalsIgnoreCase(employee.getActiveWorkforce()) ||
                        "TEMP".equalsIgnoreCase(employee.getActiveWorkforce())) &&
                        "Direct".equalsIgnoreCase(employee.getDirect()) &&
                        !employee.getImpersonal())
                .mapToDouble(Employee::getFTE)
                .sum();

        double indirectFteSum = employeesFiltered.stream()
                .filter(employee -> ("AWF".equalsIgnoreCase(employee.getActiveWorkforce()) ||
                        "TEMP".equalsIgnoreCase(employee.getActiveWorkforce())) &&
                        "Indirect".equalsIgnoreCase(employee.getDirect())&&
                        !employee.getImpersonal())
                .mapToDouble(Employee::getFTE)
                .sum();

        return new IndirectRadioDTO(directFteSum, indirectFteSum);
    }

    public List<NawsGroupedByReasonDTO> getNawsGroupedByReason(
            MultiValueMap<String, String> params, List<Siglum> siglumVisibilityList) {

        Specification<Employee> spec = employeeSpecification.getSpecifications(params);
        List<Employee> employees = employeeRepository.findAll(spec);

        List<Employee> employeesFiltered =
                employeeRepository.getEmployeesFilteredBySiglumVisibility(employees, siglumVisibilityList);

        return employeesFiltered.stream()
                .filter(employee -> "NAWF".equals(employee.getActiveWorkforce()))
                .collect(Collectors.groupingBy(
                        employee -> Optional.ofNullable(employee.getAvailabilityReason()).orElse("Unknown"),
                        Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new NawsGroupedByReasonDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public BorrowedLeasedDTO getBorrowedLeasedDTO(
            MultiValueMap<String, String> params, List<Siglum> siglumVisibilityList, int yearFilter) {

        Specification<Employee> spec = employeeSpecification.getSpecifications(params);
        List<Employee> employees = employeeRepository.findAll(spec);

        List<Employee> employeesFiltered =
                employeeRepository.getEmployeesFilteredBySiglumVisibility(employees, siglumVisibilityList);


        List<Double> borrowedMonthly = new ArrayList<>(Collections.nCopies(12, 0.0));
        List<Double> leasedMonthly = new ArrayList<>(Collections.nCopies(12, 0.0));

        for (int i = 0; i < 12; i++) {
            final Month month = Month.of(i + 1);

            List<Lever> leversForMonth = employeesFiltered.stream()
                    .flatMap(employee -> employee.getLevers().stream())
                    .filter(lever -> lever.getStartDate() != null && lever.getEndDate() != null)
                    .filter(lever -> {
                        LocalDate startDate = lever.getStartDate().atZone(ZoneId.systemDefault()).toLocalDate();
                        LocalDate endDate = lever.getEndDate().atZone(ZoneId.systemDefault()).toLocalDate();
                        return (startDate.getYear() <= yearFilter && endDate.getYear() >= yearFilter &&
                                (startDate.getMonthValue() <= month.getValue() || startDate.getYear() < yearFilter) &&
                                (endDate.getMonthValue() >= month.getValue() || endDate.getYear() > yearFilter));
                    })
                    .collect(Collectors.toList());

            borrowedMonthly.set(i, leversForMonth.stream()
                    .filter(lever -> "Borrowed".equals(lever.getLeverType()))
                    .mapToDouble(lever -> lever.getEmployee() != null && lever.getEmployee().getFTE() != null ? lever.getEmployee().getFTE() : 0.0)
                    .sum());

            leasedMonthly.set(i, leversForMonth.stream()
                    .filter(lever -> "Leased".equals(lever.getLeverType()))
                    .mapToDouble(lever -> lever.getEmployee() != null && lever.getEmployee().getFTE() != null ? lever.getEmployee().getFTE() : 0.0)
                    .sum());
        }

        List<Float> borrowedMonthlyDistribution = borrowedMonthly.stream()
                .map(Double::floatValue)
                .collect(Collectors.toList());

        List<Float> leasedMonthlyDistribution = leasedMonthly.stream()
                .map(Double::floatValue)
                .collect(Collectors.toList());

        double borrowedMonthlyAverage = borrowedMonthly.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double leasedMonthlyAverage = leasedMonthly.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        borrowedMonthlyAverage = Math.round(borrowedMonthlyAverage * 10.0) / 10.0;
        leasedMonthlyAverage = Math.round(leasedMonthlyAverage * 10.0) / 10.0;

        Float averageBorrowed = (float) borrowedMonthlyAverage;
        Float averageLeased = (float) leasedMonthlyAverage;

        Float netDifference = averageBorrowed - averageLeased;

        return new BorrowedLeasedDTO(averageBorrowed, averageLeased, netDifference, borrowedMonthlyDistribution, leasedMonthlyDistribution);
    }

    public ResponseEntity<TeamOutlookDTO> getTeamOutlook(
            MultiValueMap<String, String> params,
            List<Siglum> siglumList,
            String userSelected,
            int year) {

        Specification<Employee> spec = employeeSpecification.getSpecifications(params);
        List<Employee> employees = employeeRepository.findAll(spec);

        TeamOutlookDTO teamOutlookDTO = new TeamOutlookDTO();

        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate endOfYear = LocalDate.of(year, 12, 31);
        Instant startOfYearInstant = startOfYear.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfYearInstant = endOfYear.atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<Employee> borrowedList = leverRepository.findEmployeesWithBorrowedLever(startOfYearInstant, endOfYearInstant, employees);
        List<Employee> leasedList = leverRepository.findEmployeesWithLeasedLever(startOfYearInstant, endOfYearInstant, employees);

        Float fteAwf = employeeRepository.sumFTEsByActiveWorkforce("AWF", employees);
        Float fteTemp = employeeRepository.sumFTEsByActiveWorkforce("TEMP", employees);
        Float fteActuals = (fteAwf != null ? fteAwf : 0.0f) + (fteTemp != null ? fteTemp : 0.0f);
        teamOutlookDTO.setFteActives(fteActuals);

        Float fteNonActives = employeeRepository.sumFTEsByActiveWorkforce("NAWF", employees);
        teamOutlookDTO.setFteNonActives(fteNonActives != null ? fteNonActives : 0.0f);

        List<Employee> filteredEmployees = employees.stream()
                .filter(emp -> !borrowedList.contains(emp) && !leasedList.contains(emp))
                .collect(Collectors.toList());

        Float leavers = leverRepository.sumFTEsLeaversAfterStartDate(startOfYearInstant, endOfYearInstant, filteredEmployees);
        teamOutlookDTO.setLeavers(leavers != null ? leavers : 0.0f);

        Float recoveries = leverRepository.sumFTEsRecoveriesBeforeEndOfYear(startOfYearInstant, endOfYearInstant, filteredEmployees);
        teamOutlookDTO.setRecoveries(recoveries != null ? recoveries : 0.0f);

        Float redeployment = leverRepository.sumFTEsForRedeployment(startOfYearInstant, filteredEmployees);
        teamOutlookDTO.setRedeployment(redeployment != null ? redeployment : 0.0f);

        Float perimeterChanges = leverRepository.sumFTEsForPerimeterChanges(startOfYearInstant, filteredEmployees);
        teamOutlookDTO.setPerimeterChanges(perimeterChanges != null ? perimeterChanges : 0.0f);

        Specification<JobRequest> jobRequestSpec = jobRequestSpecification.getSpecifications(params);
        List<JobRequest> filteredJobRequests = jobRequestRepository.findAll(jobRequestSpec);

        Float filled = jobRequestRepository.countFilledJobRequestsAfterStartDate(filteredJobRequests, startOfYearInstant, endOfYearInstant);
        teamOutlookDTO.setFilled(filled != null ? filled : 0.0f);

        Float opened = jobRequestRepository.countOpenedJobRequestsAfterStartDate(filteredJobRequests, startOfYearInstant, endOfYearInstant);
        teamOutlookDTO.setOpened(opened != null ? opened : 0.0f);

        Float validationProcess = jobRequestRepository.countValidationProcessJobRequests(filteredJobRequests, startOfYearInstant, endOfYearInstant);
        teamOutlookDTO.setValidationProcess(validationProcess != null ? validationProcess : 0.0f);

        Float onHold = jobRequestRepository.countOnHoldJobRequestsAfterStartDate(filteredJobRequests, startOfYearInstant, endOfYearInstant);
        teamOutlookDTO.setOnHold(onHold != null ? onHold : 0.0f);

        Float realisticView = fteActuals
                + (leavers != null ? leavers : 0.0f)
                + (recoveries != null ? recoveries : 0.0f)
                + (redeployment != null ? redeployment : 0.0f)
                + (perimeterChanges != null ? perimeterChanges : 0.0f)
                + (filled != null ? filled : 0.0f)
                + (opened != null ? opened : 0.0f);
        teamOutlookDTO.setRealisticView(realisticView);

        Float validationView = realisticView
                + (validationProcess != null ? validationProcess : 0.0f);
        teamOutlookDTO.setValidationView(validationView);

        Float optimisticView = validationView
                + (onHold != null ? onHold : 0.0f);
        teamOutlookDTO.setOptimisticView(optimisticView);

        List<Siglum> siglumFiltered = utils.getVisibleSiglums(null, userSelected);
        List<String> selectedSiglumHRs = Optional.ofNullable(params.get("siglum.siglumHR"))
                .orElse(null);
        List<Siglum> siglumsSelected = Optional.ofNullable(selectedSiglumHRs)
                .map(siglumHRs -> siglumFiltered.stream()
                        .filter(siglum -> siglumHRs.contains(siglum.getSiglumHR()))
                        .collect(Collectors.toList()))
                .orElse(siglumFiltered);

        Float hcCeiling = 0.0F;

            Float fte = headCountRepository.sumTotalFTEForCurrentYearExercise(String.valueOf(year), "current", siglumsSelected);
            hcCeiling = Optional.ofNullable(fte).orElse(0.0F);
            
        teamOutlookDTO.setHcCeiling(hcCeiling != null ? hcCeiling : 0.0f);

        Float internalMobility = leverRepository.sumFTEsForInternalMobility(startOfYearInstant, endOfYearInstant, filteredEmployees);
        teamOutlookDTO.setInternalMobility(internalMobility != null ? internalMobility : 0.0f);

        MonthlyDistributionDTO monthlyDistribution = getMonthlyDistribution(params, siglumList, userSelected, year);
        List<Double> realisticViewMonths = monthlyDistribution.getRealisticView().stream().limit(12).collect(Collectors.toList());
        List<Double> validationViewMonths = monthlyDistribution.getValidationView().stream().limit(12).collect(Collectors.toList());
        List<Double> optimisticViewMonths = monthlyDistribution.getOptimisticView().stream().limit(12).collect(Collectors.toList());

        Float realisticViewMonthlyAverage = (float) realisticViewMonths.stream()
                .mapToDouble(value -> value)
                .average()
                .orElse(0.0);

        Float validationViewMonthlyAverage = (float) validationViewMonths.stream()
                .mapToDouble(value -> value)
                .average()
                .orElse(0.0);

        Float optimisticViewMonthlyAverage = (float) optimisticViewMonths.stream()
                .mapToDouble(value -> value)
                .average()
                .orElse(0.0);

        teamOutlookDTO.setRealisticViewAverage(realisticViewMonthlyAverage);
        teamOutlookDTO.setValidationViewAverage(validationViewMonthlyAverage);
        teamOutlookDTO.setOptimisticViewAverage(optimisticViewMonthlyAverage);

        return ResponseEntity.ok(teamOutlookDTO);
    }

    public MonthlyDistributionDTO getMonthlyDistribution(
            MultiValueMap<String, String> params,
            List<Siglum> siglumList,
            String userSelected,
            int year) {

  		Specification<Employee> spec = employeeSpecification.getSpecifications(params);
  		List<Employee> employees = employeeRepository.findAll(spec);

  		Specification<JobRequest> jobRequestSpec = jobRequestSpecification.getSpecifications(params);
  		List<JobRequest> filteredJobRequests = jobRequestRepository.findAll(jobRequestSpec);

  		Float fteAwf = employeeRepository.sumFTEsByActiveWorkforce("AWF", employees);
  		Float fteTemp = employeeRepository.sumFTEsByActiveWorkforce("TEMP", employees);
  		Float fteActuals = (fteAwf != null ? fteAwf : 0.0f) + (fteTemp != null ? fteTemp : 0.0f);

  		List<Double> awfMonthly = new ArrayList<>(Collections.nCopies(13, 0.0));
  		List<Double> nawfMonthly = new ArrayList<>(Collections.nCopies(13, 0.0));
  		List<Double> tempMonthly = new ArrayList<>(Collections.nCopies(13, 0.0));
  		List<Double> fteActualsMonthly = new ArrayList<>(Collections.nCopies(13, 0.0));

  		List<Double> realisticViewMonthly = new ArrayList<>(Collections.nCopies(13, 0.0));
  		List<Double> validationViewMonthly = new ArrayList<>(Collections.nCopies(13, 0.0));
  		List<Double> optimisticViewMonthly = new ArrayList<>(Collections.nCopies(13, 0.0));

  		List<Double> leaversMonthly = new ArrayList<>(Collections.nCopies(13, 0.0));
  		List<Double> recoveriesMonthly = new ArrayList<>(Collections.nCopies(13, 0.0));
  		List<Double> redeploymentMonthly = new ArrayList<>(Collections.nCopies(13, 0.0));
  		List<Double> perimeterChangesMonthly = new ArrayList<>(Collections.nCopies(13, 0.0));

  		List<Double> op = new ArrayList<>(Collections.nCopies(13, 0.0));
  		List<Double> fcii = new ArrayList<>(Collections.nCopies(13, 0.0));
  		List<Double> bottomUp = new ArrayList<>(Collections.nCopies(13, 0.0));

  		List<Double> jobRequestWithFilledOrOpenedStatusMonthly = new ArrayList<>(Collections.nCopies(13, 0.0));
  		List<Double> jobRequestWithValidationOrProgressStatusMonthly = new ArrayList<>(Collections.nCopies(13, 0.0));
  		List<Double> jobRequestWithOnHoldStatusMonthly = new ArrayList<>(Collections.nCopies(13, 0.0));

  		LocalDate startOfYear = LocalDate.of(year, 1, 1);
	    LocalDate endOfYear = LocalDate.of(year, 12, 31);
		 
	    Instant startOfYearInstant = startOfYear.atStartOfDay(ZoneId.systemDefault()).toInstant();
	    Instant endOfYearInstant = endOfYear.atStartOfDay(ZoneId.systemDefault()).toInstant();

	    List<Employee> borrowedList = leverRepository.findEmployeesWithBorrowedLever(startOfYearInstant, endOfYearInstant, employees);
	    List<Employee> leasedList = leverRepository.findEmployeesWithLeasedLever(startOfYearInstant, endOfYearInstant, employees);
  		List<Employee> filteredEmployees = employees.stream()
  		.filter(emp -> !borrowedList.contains(emp) && !leasedList.contains(emp))
  		.collect(Collectors.toList());
  		
		Float recoveries = leverRepository.sumFTEsRecoveriesBeforeEndOfYear(startOfYearInstant, endOfYearInstant, filteredEmployees);
		Float perimeterChanges = leverRepository.sumFTEsForPerimeterChanges(startOfYearInstant, filteredEmployees);
	       
  		for (int i = 0; i < 12; i++) {
  		  final Month month = Month.of(i + 1);
  		  LocalDate startOfMonth = LocalDate.of(year, month, 1);
  		  LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());

  		  Instant startOfMonthInstant = startOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant();
  		  Instant endOfMonthInstant = endOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant();

  		  List<Lever> leversForMonth = employees.stream()
  		          .flatMap(employee -> employee.getLevers().stream())
  		          .filter(lever -> lever.getStartDate() != null)
  		          .filter(lever -> {
  		              Instant startDate = lever.getStartDate();
  		              Instant endDate = lever.getEndDate();

  		              if (endDate == null) {
  		                  return startDate.isBefore(endOfMonthInstant) || startDate.equals(endOfMonthInstant);
  		              } else {
  		                  return (startDate.isBefore(endOfMonthInstant) || startDate.equals(endOfMonthInstant)) &&
  		                          (endDate.isAfter(startOfMonthInstant) || endDate.equals(startOfMonthInstant));
  		              }
  		          })
  		          .collect(Collectors.toList());

  		  awfMonthly.set(i, leversForMonth.stream()
  		          .filter(lever -> "AWF".equals(lever.getEmployee().getActiveWorkforce()))
  		          .mapToDouble(lever -> lever.getEmployee().getFTE() != null ? lever.getEmployee().getFTE() : 0.0)
  		          .sum());

  		  nawfMonthly.set(i, leversForMonth.stream()
  		          .filter(lever -> "NAWF".equals(lever.getEmployee().getActiveWorkforce()))
  		          .mapToDouble(lever -> lever.getEmployee().getFTE() != null ? lever.getEmployee().getFTE() : 0.0)
  		          .sum());

  		  tempMonthly.set(i, leversForMonth.stream()
  		          .filter(lever -> "TEMP".equals(lever.getEmployee().getActiveWorkforce()))
  		          .mapToDouble(lever -> lever.getEmployee().getFTE() != null ? lever.getEmployee().getFTE() : 0.0)
  		          .sum());

  		  fteActualsMonthly.set(i,
  		          (awfMonthly.get(i) != null ? awfMonthly.get(i) : 0.0) +
  		          (tempMonthly.get(i) != null ? tempMonthly.get(i) : 0.0));

  		  Float leavers = leverRepository.sumFTEsLeaversAfterStartDate(startOfYearInstant, endOfYearInstant, filteredEmployees);
	      
  		  leaversMonthly.set(i, Double.valueOf(leavers != null ? leavers : 0.0f));
  	
  		  recoveriesMonthly.set(i, (double) (recoveries != null ? recoveries : 0.0f));

  		  Float  redeployment = leverRepository.sumFTEsForRedeployment( startOfYearInstant, filteredEmployees);
		 
  		  
  		  redeploymentMonthly.set(i, (double) (redeployment != null ? redeployment : 0.0f));

  		  perimeterChangesMonthly.set(i, (double) (perimeterChanges != null ? perimeterChanges : 0.0f));

  		  Float jobRequestWithFilledOrOpenedStatus = Float.valueOf(jobRequestRepository.countJobRequestsWithFilledOrOpenedStatus(startOfMonthInstant, endOfMonthInstant, filteredJobRequests));
  		  jobRequestWithFilledOrOpenedStatusMonthly.set(i, (double) (jobRequestWithFilledOrOpenedStatus != null ? jobRequestWithFilledOrOpenedStatus : 0.0f));

  		  Float jobRequestWithValidationOrProgressStatus = Float.valueOf(jobRequestRepository.countJobRequestsWithValidationOrProgressStatus(startOfMonthInstant, endOfMonthInstant, filteredJobRequests));
  		  jobRequestWithValidationOrProgressStatusMonthly.set(i, (double) (jobRequestWithValidationOrProgressStatus != null ? jobRequestWithValidationOrProgressStatus : 0.0f));

  		  Float jobRequestWithOnHoldStatus = Float.valueOf(jobRequestRepository.countJobRequestsWithOnHoldStatus(startOfMonthInstant, endOfMonthInstant, filteredJobRequests));
  		  jobRequestWithOnHoldStatusMonthly.set(i, (double) (jobRequestWithOnHoldStatus != null ? jobRequestWithOnHoldStatus : 0.0f));

  		  double realisticView = (fteActuals != null ? fteActuals : 0.0)
  		          + (leaversMonthly.get(i) != null ? leaversMonthly.get(i) : 0.0)
  		          + (recoveriesMonthly.get(i) != null ? recoveriesMonthly.get(i) : 0.0)
  		          + (redeploymentMonthly.get(i) != null ? redeploymentMonthly.get(i) : 0.0)
  		          + (perimeterChangesMonthly.get(i) != null ? perimeterChangesMonthly.get(i) : 0.0)
  		          + (jobRequestWithFilledOrOpenedStatusMonthly.get(i) != null ? jobRequestWithFilledOrOpenedStatusMonthly.get(i) : 0.0);
  		  realisticViewMonthly.set(i, realisticView);

  		  double validationView = (realisticView
  		          + (jobRequestWithValidationOrProgressStatusMonthly.get(i) != null ? jobRequestWithValidationOrProgressStatusMonthly.get(i) : 0.0));
  		  validationViewMonthly.set(i, validationView);

  		  double optimisticView = (validationView
  		          + (jobRequestWithOnHoldStatusMonthly.get(i) != null ? jobRequestWithOnHoldStatusMonthly.get(i) : 0.0));
  		  optimisticViewMonthly.set(i, optimisticView);
  		}

  		LocalDate startOfNextJanuary = LocalDate.of(year + 1, Month.JANUARY, 1);
  		LocalDate endOfNextJanuary = startOfNextJanuary.withDayOfMonth(startOfNextJanuary.lengthOfMonth());

  		Instant startOfNextJanuaryInstant = startOfNextJanuary.atStartOfDay(ZoneId.systemDefault()).toInstant();
  		Instant endOfNextJanuaryInstant = endOfNextJanuary.atStartOfDay(ZoneId.systemDefault()).toInstant();

  		awfMonthly.set(12, employees.stream()
  		      .flatMap(employee -> employee.getLevers().stream())
  		      .filter(lever -> "AWF".equals(lever.getEmployee().getActiveWorkforce()) && lever.getStartDate() != null)
  		      .filter(lever -> lever.getStartDate().isBefore(endOfNextJanuaryInstant) || lever.getStartDate().equals(endOfNextJanuaryInstant))
  		      .mapToDouble(lever -> lever.getEmployee().getFTE() != null ? lever.getEmployee().getFTE() : 0.0)
  		      .sum());

  		nawfMonthly.set(12, employees.stream()
  		      .flatMap(employee -> employee.getLevers().stream())
  		      .filter(lever -> "NAWF".equals(lever.getEmployee().getActiveWorkforce()) && lever.getStartDate() != null)
  		      .filter(lever -> lever.getStartDate().isBefore(endOfNextJanuaryInstant) || lever.getStartDate().equals(endOfNextJanuaryInstant))
  		      .mapToDouble(lever -> lever.getEmployee().getFTE() != null ? lever.getEmployee().getFTE() : 0.0)
  		      .sum());

  		tempMonthly.set(12, employees.stream()
  		      .flatMap(employee -> employee.getLevers().stream())
  		      .filter(lever -> "TEMP".equals(lever.getEmployee().getActiveWorkforce()) && lever.getStartDate() != null)
  		      .filter(lever -> lever.getStartDate().isBefore(endOfNextJanuaryInstant) || lever.getStartDate().equals(endOfNextJanuaryInstant))
  		      .mapToDouble(lever -> lever.getEmployee().getFTE() != null ? lever.getEmployee().getFTE() : 0.0)
  		      .sum());

  		fteActualsMonthly.set(12, awfMonthly.get(12) + tempMonthly.get(12));

  		Float leavers = leverRepository.sumFTEsLeaversAfterStartDate(startOfYearInstant, endOfYearInstant, filteredEmployees);
		
  		leaversMonthly.set(12, (double) (leavers != null ? leavers : 0.0f));

   		recoveriesMonthly.set(12, (double) (recoveries != null ? recoveries : 0.0f));

  		Float  redeployment = leverRepository.sumFTEsForRedeployment(startOfYearInstant, filteredEmployees);
		
  		redeploymentMonthly.set(12, (double) (redeployment != null ? redeployment : 0.0f));

  		Float internalMobility = leverRepository.sumFTEsForInternalMobility(startOfYearInstant, endOfYearInstant, filteredEmployees);
			
   		perimeterChangesMonthly.set(12, (double) (perimeterChanges != null ? perimeterChanges : 0.0f));

  		Float filled = jobRequestRepository.countFilledJobRequestsAfterStartDate(filteredJobRequests, startOfYearInstant, endOfYearInstant);

   		Float opened = jobRequestRepository.countOpenedJobRequestsAfterStartDate(filteredJobRequests, startOfYearInstant, endOfYearInstant);

   		Float validationProcess = jobRequestRepository.countValidationProcessJobRequests(filteredJobRequests, startOfYearInstant, endOfYearInstant);

  		Float onHold = jobRequestRepository.countOnHoldJobRequestsAfterStartDate(filteredJobRequests, startOfYearInstant, endOfYearInstant);

  		Float jobRequestWithFilledOrOpenedStatus = Float.valueOf(jobRequestRepository.countJobRequestsWithFilledOrOpenedStatus(startOfNextJanuaryInstant, endOfNextJanuaryInstant, filteredJobRequests));
  		jobRequestWithFilledOrOpenedStatusMonthly.set(12, (double) (jobRequestWithFilledOrOpenedStatus != null ? jobRequestWithFilledOrOpenedStatus : 0.0f));

  		Float jobRequestWithValidationOrProgressStatus = Float.valueOf(jobRequestRepository.countJobRequestsWithValidationOrProgressStatus(startOfNextJanuaryInstant, endOfNextJanuaryInstant, filteredJobRequests));
  		jobRequestWithValidationOrProgressStatusMonthly.set(12, (double) (jobRequestWithValidationOrProgressStatus != null ? jobRequestWithValidationOrProgressStatus : 0.0f));

  		Float jobRequestWithOnHoldStatus = Float.valueOf(jobRequestRepository.countJobRequestsWithOnHoldStatus(startOfNextJanuaryInstant, endOfNextJanuaryInstant, filteredJobRequests));
  		jobRequestWithOnHoldStatusMonthly.set(12, (double) (jobRequestWithOnHoldStatus != null ? jobRequestWithOnHoldStatus : 0.0f));

  		double realisticViewNextYear = fteActuals                        ;
  		realisticViewNextYear +=  (leavers != null) ? leavers : 0.0;               ;
  		realisticViewNextYear +=  (redeployment != null) ? redeployment : 0.0;      ;
  		realisticViewNextYear +=  (double) (recoveries != null ? recoveries : 0.0f); 
  		realisticViewNextYear +=  (double) (perimeterChanges != null ? perimeterChanges : 0.0f); 
  		realisticViewNextYear +=  filled != null ? filled : 0.0f; 
  		realisticViewNextYear +=  opened != null ? opened : 0.0f; 

  		realisticViewMonthly.set(12, realisticViewNextYear);

  		double validationViewNextYear =  validationProcess != null ? validationProcess : 0.0f;
  		validationViewNextYear += realisticViewNextYear;
  		validationViewMonthly.set(12, validationViewNextYear);
  		
  		double optimisticViewNextYear = validationViewNextYear;
  		optimisticViewNextYear +=  onHold != null ? onHold : 0.0f; 
  		optimisticViewMonthly.set(12, optimisticViewNextYear);

  		String currentYear = String.valueOf(startOfYearInstant.atZone(ZoneId.systemDefault()).getYear());

  		Float totalFTEForCurrentYear = headCountRepository.sumTotalFTEForCurrentYear(currentYear);

   		Float perimeterChangesGeneric = leverRepository.sumFTEsForPerimeterChanges(startOfYearInstant, employees);
    	
   		Float redeploymentGeneric = leverRepository.sumFTEsForRedeployment(startOfYearInstant, employees);

        int currentYear2 = LocalDate.now().getYear();
        List<WorkloadEvolution> wExerciseClosed = workloadEvolutionRepository.findByStatusAndSiglumByYear(Constants.WORKLOAD_EVOLUTION_STATUS_CLOSED, year);
        List<WorkloadEvolution> wExerciseClosedCurrentYear = workloadEvolutionRepository.findByStatusAndSiglumByYear(Constants.WORKLOAD_EVOLUTION_STATUS_CLOSED, currentYear2);

        List<Siglum> siglumFiltered = utils.getVisibleSiglums(null, userSelected);
        List<String> selectedSiglumHRs = Optional.ofNullable(params.get("siglum.siglumHR"))
                .orElse(null);
        List<Siglum> siglumsSelected = Optional.ofNullable(selectedSiglumHRs)
                .map(siglumHRs -> siglumFiltered.stream()
                        .filter(siglum -> siglumHRs.contains(siglum.getSiglumHR()))
                        .collect(Collectors.toList()))
                .orElse(siglumFiltered);

        Float hcCeiling = 0.0F;
        if (!wExerciseClosed.isEmpty()) {
            Float fte = headCountRepository.sumTotalFTEForCurrentYearExercise(String.valueOf(year), "current", siglumsSelected);
            hcCeiling = Optional.ofNullable(fte).orElse(0.0F);
        }


  		WorkloadMonthlyDistributionExerciseDTO wmd = workloadService.workloadMontlyDistribution(params, siglumList, year);

  		return new MonthlyDistributionDTO(
  		      realisticViewMonthly,
  		      validationViewMonthly,
  		      optimisticViewMonthly,
  		      wmd.getOp(),
  		      wmd.getFcii(),
  		      wmd.getWip(),
  		      wmd.getWipValue(),
  		      hcCeiling);
  		}
    
    public byte[] getReportFromEmployeeeWorkforce(
            MultiValueMap<String, String> params,
            String userSelected,
            int yearFilter) {

    	 Workbook workbook = new XSSFWorkbook();

         try {
             Specification<Employee> employeeSpec = employeeSpecification.getSpecifications(params);
             List<Employee> employees = employeeRepository.findAll(employeeSpec);

             List<Siglum> siglumVisibilityList = utils.getSiglumVisibilityList(userSelected);
             List<Employee> employeeReportList = employeeRepository.getEmployeesFilteredBySiglumVisibility(employees, siglumVisibilityList);

             LocalDate currentDate = LocalDate.now();
             StringBuilder monthlyHeader = new StringBuilder();
             for (int i = 1; i <= 12; i++) {
                 if (currentDate.getMonthValue() == i) {
                     monthlyHeader.append("Actual,");
                 } else {
                     monthlyHeader.append("01/").append(i).append("/").append(yearFilter).append(",");
                 }
             }

             List<String> filterParams = List.of(
                     "SiglumHR", "Siglum6", "Siglum5", "Siglum4", "WorkerId",
                     "ActiveWorkforce", "WC/BC", "LastName", "FirstName", "Job", "AvailabilityReason",
                     "Direct", "ContractType", "CostCenter", "Country", "Site", "KAPIS Code");

             StringBuilder endOfYearHeader = new StringBuilder();
             List<String> fields = params.get("fields");

             for (String key : fields) {
                 if (employeeWorkforceReport.existsIn(key, filterParams))
                     endOfYearHeader.append(key).append(",");
             }

             // Asegurarse de que los campos "Actual" y "EndOfYear" estén siempre presentes en el encabezado
             endOfYearHeader.append("Actual,EndOfYear,");

             Sheet sheet = workbook.createSheet("Employee Workforce Report");

             CellStyle headerStyleBase = workbook.createCellStyle();
             headerStyleBase.setFillForegroundColor(IndexedColors.SKY_BLUE.getIndex());
             headerStyleBase.setFillPattern(FillPatternType.SOLID_FOREGROUND);
             headerStyleBase.setBorderBottom(BorderStyle.THIN);
             headerStyleBase.setBorderTop(BorderStyle.THIN);
             headerStyleBase.setBorderLeft(BorderStyle.THIN);
             headerStyleBase.setBorderRight(BorderStyle.THIN);
             Font headerFont = workbook.createFont();
             headerFont.setBold(true);
             headerStyleBase.setFont(headerFont);

             CellStyle headerStyleLeft = workbook.createCellStyle();
             headerStyleLeft.cloneStyleFrom(headerStyleBase);
             headerStyleLeft.setBorderRight(BorderStyle.NONE);

             CellStyle headerStyleMiddle = workbook.createCellStyle();
             headerStyleMiddle.cloneStyleFrom(headerStyleBase);
             headerStyleMiddle.setBorderLeft(BorderStyle.NONE);
             headerStyleMiddle.setBorderRight(BorderStyle.NONE);

             CellStyle headerStyleRight = workbook.createCellStyle();
             headerStyleRight.cloneStyleFrom(headerStyleBase);
             headerStyleRight.setBorderLeft(BorderStyle.NONE);

             Row headerRow = sheet.createRow(0);
             String[] headers = endOfYearHeader.toString().split(",");

             for (int i = 0; i < headers.length; i++) {
                 String headerText = headers[i] + "    ";
                 Cell cell = headerRow.createCell(i);
                 cell.setCellValue(headerText);
                 if (i == 0) {
                     cell.setCellStyle(headerStyleLeft);
                 } else if (i == headers.length - 1) {
                     cell.setCellStyle(headerStyleRight);
                 } else {
                     cell.setCellStyle(headerStyleMiddle);
                 }
             }

             int rowIndex = 1;
             for (Employee e : employeeReportList) {
                 Row row = sheet.createRow(rowIndex);
                 int cellIndex = 0;
                 for (String key : fields) {
                     if (employeeWorkforceReport.existsIn(key, filterParams)) {
                         switch (key) {
                             case "SiglumHR":
                                 row.createCell(cellIndex).setCellValue(e.getSiglum().getSiglumHR());
                                 break;
                             case "Siglum6":
                                 row.createCell(cellIndex).setCellValue(e.getSiglum().getSiglum6());
                                 break;
                             case "Siglum5":
                                 row.createCell(cellIndex).setCellValue(e.getSiglum().getSiglum5());
                                 break;
                             case "Siglum4":
                                 row.createCell(cellIndex).setCellValue(e.getSiglum().getSiglum4());
                                 break;
                             case "WorkerId":
                                 row.createCell(cellIndex).setCellValue(e.getEmployeeId() != null ? e.getEmployeeId().toString() : "");
                                 break;
                             case "ActiveWorkforce":
                                 row.createCell(cellIndex).setCellValue(e.getActiveWorkforce());
                                 break;
                             case "WC/BC":
                                 row.createCell(cellIndex).setCellValue(e.getCollar());
                                 break;
                             case "LastName":
                                 row.createCell(cellIndex).setCellValue(e.getLastName());
                                 break;
                             case "FirstName":
                                 row.createCell(cellIndex).setCellValue(e.getFirstName());
                                 break;
                             case "Job":
                                 row.createCell(cellIndex).setCellValue(e.getJob());
                                 break;
                             case "AvailabilityReason":
                                 row.createCell(cellIndex).setCellValue(e.getAvailabilityReason() != null ? e.getAvailabilityReason() : "");
                                 break;
                             case "Direct":
                                 row.createCell(cellIndex).setCellValue(e.getDirect());
                                 break;
                             case "ContractType":
                                 row.createCell(cellIndex).setCellValue(e.getContractType());
                                 break;
                             case "CostCenter":
                                 CostCenter costCenter = e.getCostCenter();
                                 row.createCell(cellIndex).setCellValue(costCenter != null ? costCenter.getCostCenterCode() : "");
                                 break;
                             case "Country":
                                 if (e.getCostCenter() != null && e.getCostCenter().getLocation() != null) {
                                     row.createCell(cellIndex).setCellValue(e.getCostCenter().getLocation().getCountry());
                                 } else {
                                     row.createCell(cellIndex).setCellValue("");
                                 }
                                 break;
                             case "Site":
                                 if (e.getCostCenter() != null && e.getCostCenter().getLocation() != null) {
                                     row.createCell(cellIndex).setCellValue(e.getCostCenter().getLocation().getSite());
                                 } else {
                                     row.createCell(cellIndex).setCellValue("");
                                 }
                                 break;
                             case "KAPIS Code":
                                 if (e.getCostCenter() != null && e.getCostCenter().getLocation() != null) {
                                     row.createCell(cellIndex).setCellValue(e.getCostCenter().getLocation().getKapisCode());
                                 } else {
                                     row.createCell(cellIndex).setCellValue("");
                                 }
                                 break;
                         }
                         cellIndex++;
                     }
                 }
                 // Calcular y añadir campos "Actual" y "EndOfYear"
                 double actual = 0.0;
                 double endOfYear = 0.0;

                 if ("Parental leave absence".equals(e.getAvailabilityReason())) {
                     actual = 1.0;
                     endOfYear = 1.0;
                 }

                 row.createCell(cellIndex++).setCellValue(actual);
                 row.createCell(cellIndex).setCellValue(endOfYear);
                 rowIndex++;
             }

             int lastDataRowIndex = rowIndex - 1;
             int numCols = headers.length;
             for (int r = 1; r <= lastDataRowIndex; r++) {
                 Row dataRow = sheet.getRow(r);
                 if (dataRow == null) continue;
                 for (int c = 0; c < numCols; c++) {
                     Cell cell = dataRow.getCell(c);
                     if (cell == null) continue;
                     CellStyle currentStyle = cell.getCellStyle();
                     CellStyle newStyle = workbook.createCellStyle();
                     if (currentStyle != null) {
                         newStyle.cloneStyleFrom(currentStyle);
                     }
                     if (c == 0) {
                         newStyle.setBorderLeft(BorderStyle.THIN);
                     }
                     if (c == numCols - 1) {
                         newStyle.setBorderRight(BorderStyle.THIN);
                     }
                     if (r == lastDataRowIndex) {
                         newStyle.setBorderBottom(BorderStyle.THIN);
                     }
                     cell.setCellStyle(newStyle);
                 }
             }

             for (int cont = 0; cont < headers.length; cont++) {
                 sheet.autoSizeColumn(cont);
             }

             try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                 workbook.write(bos);
                 return bos.toByteArray();
             }
         } catch (IOException e) {
             e.printStackTrace();
             return null;
         } finally {
             try {
                 workbook.close();
             } catch (IOException e) {
                 e.printStackTrace();
             }
         }
     }
      
    public ReportEndOfYearDTO getReportSiglumEndOfYear(
            List<ActiveWorkforceReportDTO> activeWorkforceReportList,
            List<ActiveWorkforceReportDTO> tempWorkforceReportList,
            List<Employee> employees,
            TeamOutlookDTO teamOutlookDTO) {

        List<ActiveWorkforceReportCapacityDTO> actualsList =
                employeeReportImpl.getActualsActiveWorkforceCapacity(
                        activeWorkforceReportList, tempWorkforceReportList);

        List<ActiveWorkforceReportCapacityDTO> realisticList =
                employeeReportImpl.getRealisticActiveWorkforceCapacity(actualsList, teamOutlookDTO);

        List<ActiveWorkforceReportCapacityDTO> validationList =
                employeeReportImpl.getValidationActiveWorkforceCapacity(realisticList, teamOutlookDTO);

        List<ActiveWorkforceReportCapacityDTO> optimisticList =
                employeeReportImpl.getOptimisticActiveWorkforceCapacity(validationList, teamOutlookDTO);

        return new ReportEndOfYearDTO(actualsList, realisticList, validationList, optimisticList);
    }


    public byte[] getReportSiglum4EndOfYear(
            MultiValueMap<String, String> params,
            String userSelected,
            int yearFilter) {

        try {
            // Obtener especificaciones y datos necesarios
            Specification<Employee> employeeSpec = employeeSpecification.getSpecifications(params);
            List<Employee> employees = employeeRepository.findAll(employeeSpec);

            List<Siglum> siglumVisibilityList = utils.getSiglumVisibilityList(userSelected);

            TeamOutlookDTO teamOutlookDTO =
                    getTeamOutlook(params, utils.getSiglumVisibilityList(userSelected), userSelected, yearFilter).getBody();

            List<ActiveWorkforceReportDTO> activeWorkforceAndSiglumList =
                    employeeRepository.sumFTEsByActiveWorkforceAndSiglum4("AWF", employees, siglumVisibilityList);
            List<ActiveWorkforceReportDTO> tempWorkforceAndSiglumList =
                    employeeRepository.sumFTEsByActiveWorkforceAndSiglum4("TEMP", employees, siglumVisibilityList);

            ReportEndOfYearDTO reportSiglumEndOfYearDTO = getReportSiglumEndOfYear(
                    activeWorkforceAndSiglumList, tempWorkforceAndSiglumList, employees, teamOutlookDTO);

            // Crear el libro de Excel
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("End of Year Report");

            // Crear estilos
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setBorderBottom(BorderStyle.THIN);
            cellStyle.setBorderTop(BorderStyle.THIN);
            cellStyle.setBorderLeft(BorderStyle.THIN);
            cellStyle.setBorderRight(BorderStyle.THIN);

            // Crear estilo de celda para los títulos
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleStyle.setFont(titleFont);
            // Crear título
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Managers included on their own siglum");
            titleCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 12)); // Hasta la columna M

         // Crear encabezados (fila 2)
            Row headerRow = sheet.createRow(1);
            String[] headers = {"Actuals", "Realistic View", "Validation Required View", "Optimistic View"};
            int headerIndex = 0;
 
            
         // Crear estilo con fondo azul más claro
            CellStyle titleStyleWithColor = workbook.createCellStyle();
            titleStyleWithColor.cloneStyleFrom(titleStyle); // Clonar estilo base
            Font titleFontWithColor = workbook.createFont();
            titleFontWithColor.setBold(true);
            titleStyleWithColor.setFont(titleFontWithColor);
            titleStyleWithColor.setFillForegroundColor(IndexedColors.SKY_BLUE.getIndex()); // Azul más claro
            titleStyleWithColor.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(headerIndex);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(titleStyleWithColor); // Aplicar estilo centrado con fondo azul

                int mergeSize = (i == 0) ? 3 : 2; // "Actuals" abarca 4 columnas, las demás 3
                sheet.addMergedRegion(new CellRangeAddress(1, 1, headerIndex, headerIndex + mergeSize));

                // Aplicar borde normal al extremo derecho (columnas K, L, M) y mantener el título
                if (i == headers.length - 1) {
                    for (int col = headerIndex; col <= headerIndex + mergeSize; col++) {
                        Cell mergedCell = headerRow.createCell(col);
                        mergedCell.setCellValue("Optimistic View"); // Aseguramos que el título se conserve
                        CellStyle normalBorderStyleWithColor = workbook.createCellStyle();
                        normalBorderStyleWithColor.cloneStyleFrom(titleStyleWithColor);
                        normalBorderStyleWithColor.setBorderRight(BorderStyle.THIN); // Borde derecho normal
                        mergedCell.setCellStyle(normalBorderStyleWithColor);
                    }
                }

                headerIndex += mergeSize + 1;
            }

            
         // Crear sub-encabezados (fila 3)
            Row subHeaderRow = sheet.createRow(2);
            String[] subHeaders = {"Siglum 4", "AWF", "TEMP", "Capacity", "AWF", "TEMP", "Capacity",
                                   "AWF", "TEMP", "Capacity", "AWF", "TEMP", "Capacity"};

         // Crear estilo con fondo azul más claro "SKY_BLUE"
            CellStyle subHeaderStyleWithColor = workbook.createCellStyle();
            subHeaderStyleWithColor.cloneStyleFrom(headerStyle); // Clonar estilo base

            // Aplicar el color predefinido más claro SKY_BLUE
            subHeaderStyleWithColor.setFillForegroundColor(IndexedColors.SKY_BLUE.getIndex());
            subHeaderStyleWithColor.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (int i = 0; i < subHeaders.length; i++) {
                Cell cell = subHeaderRow.createCell(i);
                cell.setCellValue(subHeaders[i]);

                // Aplicar bordes únicamente a las celdas "Capacity" y extremos de la tabla
                if (subHeaders[i].equals("Capacity") || i == subHeaders.length - 1) {
                    CellStyle customStyle = workbook.createCellStyle();
                    customStyle.cloneStyleFrom(subHeaderStyleWithColor); // Clonar estilo con fondo azul
                    cell.setCellStyle(customStyle);
                } else if (i == 0) {
                    // Quitar el borde derecho de "Siglum 4"
                    CellStyle siglumStyle = workbook.createCellStyle();
                    siglumStyle.cloneStyleFrom(subHeaderStyleWithColor);
                    siglumStyle.setBorderRight(BorderStyle.NONE);
                    cell.setCellStyle(siglumStyle);
                } else {
                    // Las demás celdas de esta fila no tendrán bordes adicionales
                    CellStyle noBorderStyle = workbook.createCellStyle();
                    noBorderStyle.cloneStyleFrom(subHeaderStyleWithColor);
                    noBorderStyle.setBorderLeft(BorderStyle.NONE);
                    noBorderStyle.setBorderRight(BorderStyle.NONE);
                    cell.setCellStyle(noBorderStyle);
                }
            }


            // Pintar el recuadro alrededor del grupo de celdas de A1 a M1
            for (int i = 0; i <= 12; i++) {
                Cell cell = titleRow.createCell(i);
                cell.setCellStyle(headerStyle);
            }
            titleRow.getCell(0).setCellValue("Managers included on their own siglum");
            
         // Llenar filas de datos
            for (int i = 0; i < reportSiglumEndOfYearDTO.getActuals().size(); i++) {
                Row row = sheet.createRow(i + 3);

                for (int j = 0; j <= 12; j++) {
                    Cell cell = row.createCell(j);

                    if (j == 0) {
                        // Primera columna (columna A): mantener borde izquierdo y mostrar datos
                        cell.setCellValue(reportSiglumEndOfYearDTO.getActuals().get(i).getReport());
                        CellStyle leftBorderStyle = workbook.createCellStyle();
                        leftBorderStyle.cloneStyleFrom(cellStyle);
                        leftBorderStyle.setBorderLeft(BorderStyle.THIN); // Borde izquierdo
                        leftBorderStyle.setBorderTop(BorderStyle.NONE);
                        leftBorderStyle.setBorderBottom(BorderStyle.NONE);
                        leftBorderStyle.setBorderRight(BorderStyle.NONE);
                        cell.setCellStyle(leftBorderStyle);
                    } else if (j == 12) {
                        // Última columna (columna M): mantener bordes izquierdo y derecho, y mostrar datos
                        Double capacityValue = reportSiglumEndOfYearDTO.getOptimistic().get(i).getCapacity();
                        if (capacityValue != null) {
                            cell.setCellValue(capacityValue); // Asignar valor numérico
                        } else {
                            cell.setCellValue(0.0); // Valor por defecto si es nulo
                        }
                        CellStyle columnMStyle = workbook.createCellStyle();
                        columnMStyle.cloneStyleFrom(cellStyle);
                        columnMStyle.setBorderLeft(BorderStyle.THIN);  // Borde izquierdo
                        columnMStyle.setBorderRight(BorderStyle.THIN); // Borde derecho
                        columnMStyle.setBorderTop(BorderStyle.NONE);
                        columnMStyle.setBorderBottom(BorderStyle.NONE);
                        cell.setCellStyle(columnMStyle);
                    } else if (j == 3 || j == 6 || j == 9) {
                        // "Capacity": mantener bordes izquierdo y derecho
                        Double capacityValue = (j == 3) ? reportSiglumEndOfYearDTO.getActuals().get(i).getCapacity()
                                          : (j == 6) ? reportSiglumEndOfYearDTO.getRealistic().get(i).getCapacity()
                                          : reportSiglumEndOfYearDTO.getValidation().get(i).getCapacity();
                        if (capacityValue != null) {
                            cell.setCellValue(capacityValue); // Asignar valor numérico
                        } else {
                            cell.setCellValue(0.0); // Valor por defecto si es nulo
                        }
                        CellStyle capacityStyle = workbook.createCellStyle();
                        capacityStyle.cloneStyleFrom(cellStyle);
                        capacityStyle.setBorderLeft(BorderStyle.THIN);  // Borde izquierdo
                        capacityStyle.setBorderRight(BorderStyle.THIN); // Borde derecho
                        capacityStyle.setBorderTop(BorderStyle.NONE);
                        capacityStyle.setBorderBottom(BorderStyle.NONE);
                        cell.setCellStyle(capacityStyle);
                    } else {
                        // Otras celdas intermedias: mostrar datos pero sin bordes
                        Object cellValue = switch (j) {
                            case 1 -> reportSiglumEndOfYearDTO.getActuals().get(i).getAwfFTE();
                            case 2 -> reportSiglumEndOfYearDTO.getActuals().get(i).getTempFTE();
                            case 4 -> reportSiglumEndOfYearDTO.getRealistic().get(i).getAwfFTE();
                            case 5 -> reportSiglumEndOfYearDTO.getRealistic().get(i).getTempFTE();
                            case 7 -> reportSiglumEndOfYearDTO.getValidation().get(i).getAwfFTE();
                            case 8 -> reportSiglumEndOfYearDTO.getValidation().get(i).getTempFTE();
                            case 10 -> reportSiglumEndOfYearDTO.getOptimistic().get(i).getAwfFTE();
                            case 11 -> reportSiglumEndOfYearDTO.getOptimistic().get(i).getTempFTE();
                            default -> 0.0; // Dejar como valor por defecto
                        };
                        cell.setCellValue(Double.valueOf(cellValue.toString())); // Asegurar que sea numérico
                        CellStyle noBorderStyle = workbook.createCellStyle();
                        noBorderStyle.cloneStyleFrom(cellStyle);
                        noBorderStyle.setBorderLeft(BorderStyle.NONE);
                        noBorderStyle.setBorderRight(BorderStyle.NONE);
                        noBorderStyle.setBorderTop(BorderStyle.NONE);
                        noBorderStyle.setBorderBottom(BorderStyle.NONE);
                        cell.setCellStyle(noBorderStyle);
                    }
                }
            }
      
         // Agregar fila de totales con fondo azul claro "LIGHT_BLUE" y bordes completos
            int totalRowIndex = reportSiglumEndOfYearDTO.getActuals().size() + 3;
            Row totalRow = sheet.createRow(totalRowIndex);

         // Usar uno de los colores predefinidos más claros en IndexedColors
            CellStyle totalRowStyleWithColor = workbook.createCellStyle();
            totalRowStyleWithColor.cloneStyleFrom(cellStyle);
            totalRowStyleWithColor.setFillForegroundColor(IndexedColors.SKY_BLUE.getIndex()); // Azul más claro disponible
            totalRowStyleWithColor.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            // Establecer bordes superior e inferior para todas las celdas de la fila de totales
            totalRowStyleWithColor.setBorderTop(BorderStyle.THIN);  // Borde superior
            totalRowStyleWithColor.setBorderBottom(BorderStyle.THIN); // Borde inferior

            for (int i = 0; i <= 12; i++) {
                Cell totalCell = totalRow.createCell(i);

                if (i == 0) {
                    // Primera celda: mantener borde izquierdo, superior e inferior
                    totalCell.setCellValue("Total");
                    CellStyle leftBorderStyle = workbook.createCellStyle();
                    leftBorderStyle.cloneStyleFrom(totalRowStyleWithColor);
                    leftBorderStyle.setBorderLeft(BorderStyle.THIN); // Borde izquierdo
                    leftBorderStyle.setBorderTop(BorderStyle.THIN); // Borde superior
                    leftBorderStyle.setBorderBottom(BorderStyle.THIN); // Borde inferior
                    totalCell.setCellStyle(leftBorderStyle);
                } else if (i == 12) {
                    // Última celda: mantener borde derecho, superior e inferior
                    totalCell.setCellFormula("SUM(M4:M" + totalRowIndex + ")");
                    CellStyle rightBorderStyle = workbook.createCellStyle();
                    rightBorderStyle.cloneStyleFrom(totalRowStyleWithColor);
                    rightBorderStyle.setBorderRight(BorderStyle.THIN); // Borde derecho
                    rightBorderStyle.setBorderTop(BorderStyle.THIN); // Borde superior
                    rightBorderStyle.setBorderBottom(BorderStyle.THIN); // Borde inferior
                    totalCell.setCellStyle(rightBorderStyle);
                } else if (i == 3 || i == 6 || i == 9) {
                    // Campos "Capacity": bordes izquierdo, derecho, superior e inferior
                    String columnLetter = CellReference.convertNumToColString(i);
                    totalCell.setCellFormula("SUM(" + columnLetter + "4:" + columnLetter + totalRowIndex + ")");
                    CellStyle capacityStyle = workbook.createCellStyle();
                    capacityStyle.cloneStyleFrom(totalRowStyleWithColor);
                    capacityStyle.setBorderLeft(BorderStyle.THIN);  // Borde izquierdo
                    capacityStyle.setBorderRight(BorderStyle.THIN); // Borde derecho
                    capacityStyle.setBorderTop(BorderStyle.THIN);  // Borde superior
                    capacityStyle.setBorderBottom(BorderStyle.THIN); // Borde inferior
                    totalCell.setCellStyle(capacityStyle);
                } else {
                    // Celdas intermedias: bordes superior e inferior solamente
                    String columnLetter = CellReference.convertNumToColString(i);
                    totalCell.setCellFormula("SUM(" + columnLetter + "4:" + columnLetter + totalRowIndex + ")");
                    CellStyle topBottomBorderStyle = workbook.createCellStyle();
                    topBottomBorderStyle.cloneStyleFrom(totalRowStyleWithColor);
                    topBottomBorderStyle.setBorderTop(BorderStyle.THIN);  // Borde superior
                    topBottomBorderStyle.setBorderBottom(BorderStyle.THIN); // Borde inferior
                    totalCell.setCellStyle(topBottomBorderStyle);
                }
            }
      
            // Escribir la salida en un array de bytes
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                workbook.write(bos);
                return bos.toByteArray();
            } finally {
                workbook.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    } 
        
    public byte[] getReportSiglum5EndOfYear(
            MultiValueMap<String, String> params,
            String userSelected,
            int yearFilter) {

        Workbook workbook = new XSSFWorkbook();
        try {
            // Obtener especificaciones y datos necesarios
            Specification<Employee> employeeSpec = employeeSpecification.getSpecifications(params);
            List<Employee> employees = employeeRepository.findAll(employeeSpec);
            List<Siglum> siglumVisibilityList = utils.getSiglumVisibilityList(userSelected);
            TeamOutlookDTO teamOutlookDTO = getTeamOutlook(params, siglumVisibilityList, userSelected, yearFilter).getBody();

            List<ActiveWorkforceReportDTO> activeWorkforceAndSiglumList =
                    employeeRepository.sumFTEsByActiveWorkforceAndSiglum5("AWF", employees, siglumVisibilityList);
            List<ActiveWorkforceReportDTO> tempWorkforceAndSiglumList =
                    employeeRepository.sumFTEsByActiveWorkforceAndSiglum5("TEMP", employees, siglumVisibilityList);

            ReportEndOfYearDTO reportSiglumEndOfYearDTO = getReportSiglumEndOfYear(
                    activeWorkforceAndSiglumList, tempWorkforceAndSiglumList, employees, teamOutlookDTO);

            // Crear la hoja de Excel
            Sheet sheet = workbook.createSheet("End of Year Report");

            // --- Definir estilos comunes ---
            // Estilo de celda base (lo usaremos para clones y para modificar según el caso)
            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setBorderBottom(BorderStyle.THIN);
            cellStyle.setBorderTop(BorderStyle.THIN);
            cellStyle.setBorderLeft(BorderStyle.THIN);
            cellStyle.setBorderRight(BorderStyle.THIN);

            // Estilo para encabezados (negrita y con bordes)
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Estilo para título (centrado y en negrita)
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleStyle.setFont(titleFont);

            // --- Fila 0: Título ---
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Managers included on their own siglum");
            titleCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 15));
            for (int i = 0; i <= 15; i++) {
                Cell cell = titleRow.createCell(i);
                cell.setCellStyle(headerStyle);
            }
            titleRow.getCell(0).setCellValue("Managers included on their own siglum");

            // --- Fila 1: Encabezados de grupo ---
            Row headerRow = sheet.createRow(1);
            String[] groupHeaders = {"Actuals", "Realistic View", "Validation Required View", "Optimistic View"};
            int headerIndex = 0;
            CellStyle groupHeaderStyle = workbook.createCellStyle();
            groupHeaderStyle.cloneStyleFrom(titleStyle);
            Font groupHeaderFont = workbook.createFont();
            groupHeaderFont.setBold(true);
            groupHeaderStyle.setFont(groupHeaderFont);
            groupHeaderStyle.setFillForegroundColor(IndexedColors.SKY_BLUE.getIndex());
            groupHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            for (int i = 0; i < groupHeaders.length; i++) {
                Cell cell = headerRow.createCell(headerIndex);
                cell.setCellValue(groupHeaders[i]);
                cell.setCellStyle(groupHeaderStyle);
                int mergeSize = 3; // Cada grupo ocupa 4 columnas (índices: 0-3, 4-7, etc.)
                sheet.addMergedRegion(new CellRangeAddress(1, 1, headerIndex, headerIndex + mergeSize));
                // Para el último grupo, aseguramos el borde derecho
                if (i == groupHeaders.length - 1) {
                    for (int col = headerIndex; col <= headerIndex + mergeSize; col++) {
                        Cell mergedCell = headerRow.getCell(col);
                        if (mergedCell == null) {
                            mergedCell = headerRow.createCell(col);
                            mergedCell.setCellValue(groupHeaders[i]);
                        }
                        CellStyle tempStyle = workbook.createCellStyle();
                        tempStyle.cloneStyleFrom(groupHeaderStyle);
                        tempStyle.setBorderRight(BorderStyle.THIN);
                        mergedCell.setCellStyle(tempStyle);
                    }
                }
                headerIndex += mergeSize + 1;
            }

            // --- Fila 2: Subencabezados ---
            Row subHeaderRow = sheet.createRow(2);
            String[] subHeaders = {
                "Siglum5", "AWF", "Temp", "Capacity",
                "Siglum5", "AWF", "Temp", "Capacity",
                "Siglum5", "AWF", "Temp", "Capacity",
                "Siglum5", "AWF", "Temp", "Capacity"
            };
            // Se usará un estilo con fondo azul similar al de encabezados
            CellStyle subHeaderStyle = workbook.createCellStyle();
            subHeaderStyle.cloneStyleFrom(headerStyle);
            subHeaderStyle.setFillForegroundColor(IndexedColors.SKY_BLUE.getIndex());
            subHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            for (int i = 0; i < subHeaders.length; i++) {
                Cell cell = subHeaderRow.createCell(i);
                cell.setCellValue(subHeaders[i]);
                cell.setCellStyle(subHeaderStyle);
            }
            // --- Estilos para celdas de datos ---
            // Estilo sin bordes (para celdas que NO sean "Capacity")
            CellStyle dataNoBorderStyle = workbook.createCellStyle();
            dataNoBorderStyle.setBorderTop(BorderStyle.NONE);
            dataNoBorderStyle.setBorderBottom(BorderStyle.NONE);
            dataNoBorderStyle.setBorderLeft(BorderStyle.NONE);
            dataNoBorderStyle.setBorderRight(BorderStyle.NONE);
            // Estilo para celdas "Capacity": aplicar únicamente borde izquierdo y derecho
            CellStyle dataCapacityStyle = workbook.createCellStyle();
            dataCapacityStyle.setBorderTop(BorderStyle.NONE);
            dataCapacityStyle.setBorderBottom(BorderStyle.NONE);
            dataCapacityStyle.setBorderLeft(BorderStyle.THIN);
            dataCapacityStyle.setBorderRight(BorderStyle.THIN);

            // --- Filas de datos (desde fila 3) ---
            int dataRowCount = reportSiglumEndOfYearDTO.getActuals().size();
            for (int i = 0; i < dataRowCount; i++) {
                Row row = sheet.createRow(i + 3);
                for (int j = 0; j < 16; j++) {
                    Cell cell = row.createCell(j);
                    int group = j / 4;   // 0: Actuals, 1: Realistic, 2: Validation, 3: Optimistic
                    int posInGroup = j % 4; // 0 → Report, 1 → AWF, 2 → Temp, 3 → Capacity
                    if (posInGroup == 0) {
                        String value = "";
                        if (group == 0)
                            value = reportSiglumEndOfYearDTO.getActuals().get(i).getReport();
                        else if (group == 1)
                            value = reportSiglumEndOfYearDTO.getRealistic().get(i).getReport();
                        else if (group == 2)
                            value = reportSiglumEndOfYearDTO.getValidation().get(i).getReport();
                        else if (group == 3)
                            value = reportSiglumEndOfYearDTO.getOptimistic().get(i).getReport();
                        cell.setCellValue(value);
                        cell.setCellStyle(dataNoBorderStyle);
                    } else if (posInGroup == 1) {
                        Double awf = null;
                        if (group == 0)
                            awf = reportSiglumEndOfYearDTO.getActuals().get(i).getAwfFTE();
                        else if (group == 1)
                            awf = reportSiglumEndOfYearDTO.getRealistic().get(i).getAwfFTE();
                        else if (group == 2)
                            awf = reportSiglumEndOfYearDTO.getValidation().get(i).getAwfFTE();
                        else if (group == 3)
                            awf = reportSiglumEndOfYearDTO.getOptimistic().get(i).getAwfFTE();
                        if (awf == null)
                            awf = 0.0;
                        cell.setCellValue(awf);
                        cell.setCellStyle(dataNoBorderStyle);
                    } else if (posInGroup == 2){
                        Double temp = null;
                        if (group == 0)
                            temp = reportSiglumEndOfYearDTO.getActuals().get(i).getTempFTE();
                        else if (group == 1)
                            temp = reportSiglumEndOfYearDTO.getRealistic().get(i).getTempFTE();
                        else if (group == 2)
                            temp = reportSiglumEndOfYearDTO.getValidation().get(i).getTempFTE();
                        else if (group == 3)
                            temp = reportSiglumEndOfYearDTO.getOptimistic().get(i).getTempFTE();
                        if (temp == null)
                            temp = 0.0;
                        cell.setCellValue(temp);
                        cell.setCellStyle(dataNoBorderStyle);
                    } else if (posInGroup == 3) {
                        Double capacity = null;
                        if (group == 0)
                            capacity = reportSiglumEndOfYearDTO.getActuals().get(i).getCapacity();
                        else if (group == 1)
                            capacity = reportSiglumEndOfYearDTO.getRealistic().get(i).getCapacity();
                        else if (group == 2)
                            capacity = reportSiglumEndOfYearDTO.getValidation().get(i).getCapacity();
                        else if (group == 3)
                            capacity = reportSiglumEndOfYearDTO.getOptimistic().get(i).getCapacity();
                        if (capacity == null)
                            capacity = 0.0;
                        cell.setCellValue(capacity);
                        cell.setCellStyle(dataCapacityStyle);
                    }
                }
            }

            // --- Fila de totales ---
            int totalRowIndex = dataRowCount + 3;  // La fila de totales se sitúa justo debajo de los datos
            Row totalRow = sheet.createRow(totalRowIndex);
            // Definir estilo base para totales (fondo azul, bordes superior e inferior)
            CellStyle totalRowBaseStyle = workbook.createCellStyle();
            totalRowBaseStyle.cloneStyleFrom(cellStyle);
            totalRowBaseStyle.setFillForegroundColor(IndexedColors.SKY_BLUE.getIndex());
            totalRowBaseStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalRowBaseStyle.setBorderTop(BorderStyle.THIN);
            totalRowBaseStyle.setBorderBottom(BorderStyle.THIN);

            // Estilos para totales:
            // celdas que NO son Capacity: sin bordes laterales
            CellStyle totalNoBorderStyle = workbook.createCellStyle();
            totalNoBorderStyle.cloneStyleFrom(totalRowBaseStyle);
            totalNoBorderStyle.setBorderLeft(BorderStyle.NONE);
            totalNoBorderStyle.setBorderRight(BorderStyle.NONE);
            // celdas Capacity: con borde izquierdo y derecho
            CellStyle totalCapacityStyle = workbook.createCellStyle();
            totalCapacityStyle.cloneStyleFrom(totalRowBaseStyle);
            totalCapacityStyle.setBorderLeft(BorderStyle.THIN);
            totalCapacityStyle.setBorderRight(BorderStyle.THIN);

            for (int j = 0; j < 16; j++) {
                Cell totalCell = totalRow.createCell(j);
                int posInGroup = j % 4;
                if (posInGroup == 0) {
                    if (j == 0) {
                        totalCell.setCellValue("Total");
                        CellStyle tempStyle = workbook.createCellStyle();
                        tempStyle.cloneStyleFrom(totalRowBaseStyle);
                        tempStyle.setBorderLeft(BorderStyle.THIN);
                        totalCell.setCellStyle(tempStyle);
                    } else {
                        totalCell.setCellValue("");
                        totalCell.setCellStyle(totalNoBorderStyle);
                    }
                } else {
                    String colLetter = CellReference.convertNumToColString(j);
                    // Los datos se encuentran en las filas Excel desde la 4 hasta (dataRowCount+3)
                    String formula = "SUM(" + colLetter + "4:" + colLetter + (dataRowCount + 3) + ")";
                    totalCell.setCellFormula(formula);
                    totalCell.setCellStyle(posInGroup == 3 ? totalCapacityStyle : totalNoBorderStyle);
                }
            }

            // --- Escritura de la salida en un array de bytes ---
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                workbook.write(bos);
                return bos.toByteArray();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                workbook.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
  
    public byte[] getReportSiteEndOfYear(
            MultiValueMap<String, String> params,
            String userSelected,
            int yearFilter) {

        Workbook workbook = new XSSFWorkbook();
        try {
            // Obtener especificaciones y datos necesarios
            Specification<Employee> employeeSpec = employeeSpecification.getSpecifications(params);
            List<Employee> employees = employeeRepository.findAll(employeeSpec);
            List<Siglum> siglumVisibilityList = utils.getSiglumVisibilityList(userSelected);
            TeamOutlookDTO teamOutlookDTO =
                    getTeamOutlook(params, siglumVisibilityList, userSelected, yearFilter).getBody();

            // Se obtienen los datos para AWF y TEMP. La query en el repositorio ya agrupa correctamente por site.
            List<ActiveWorkforceReportDTO> activeWorkforceAndSiteList =
                    employeeRepository.sumFTEsByActiveWorkforceAndSite("AWF", employees, siglumVisibilityList);
            List<ActiveWorkforceReportDTO> tempWorkforceAndSiteList =
                    employeeRepository.sumFTEsByActiveWorkforceAndSite("TEMP", employees, siglumVisibilityList);

            ReportEndOfYearDTO reportEndOfYearDTO = getReportSiglumEndOfYear(
                    activeWorkforceAndSiteList, tempWorkforceAndSiteList, employees, teamOutlookDTO);

            // Crear la hoja de Excel
            Sheet sheet = workbook.createSheet("End of Year Report");

            // --- Definir estilos comunes ---
            // Estilo base para clonar
            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setBorderBottom(BorderStyle.THIN);
            cellStyle.setBorderTop(BorderStyle.THIN);
            cellStyle.setBorderLeft(BorderStyle.THIN);
            cellStyle.setBorderRight(BorderStyle.THIN);

            // Estilo para encabezados (negrita y con bordes)
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Estilo para el título (centrado y en negrita)
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleStyle.setFont(titleFont);

            // --- Fila 0: Título ---
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Managers included on their own site");
            titleCell.setCellStyle(headerStyle);
            // Ahora tenemos 13 columnas en total (índices 0 a 12)
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 12));
            for (int i = 0; i <= 12; i++) {
                Cell cell = titleRow.createCell(i);
                cell.setCellStyle(headerStyle);
            }
            titleRow.getCell(0).setCellValue("Managers included on their own site");

            // --- Fila 1: Encabezados de grupo ---
            Row headerRow = sheet.createRow(1);
            // Los grupos: Actuals, Realistic, Validation y Optimistic
            // En Actuals mostramos 4 columnas; en los demás mostramos 3.
            String[] groupHeaders = {"Actuals", "Realistic View", "Validation Required View", "Optimistic View"};
            int headerIndex = 0;
            CellStyle groupHeaderStyle = workbook.createCellStyle();
            groupHeaderStyle.cloneStyleFrom(titleStyle);
            Font groupHeaderFont = workbook.createFont();
            groupHeaderFont.setBold(true);
            groupHeaderStyle.setFont(groupHeaderFont);
            groupHeaderStyle.setFillForegroundColor(IndexedColors.SKY_BLUE.getIndex());
            groupHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            for (int i = 0; i < groupHeaders.length; i++) {
                Cell cell = headerRow.createCell(headerIndex);
                cell.setCellValue(groupHeaders[i]);
                cell.setCellStyle(groupHeaderStyle);
                // Definir el número de columnas que ocupa cada grupo:
                int mergeSize = (i == 0) ? 3 : 2; // Grupo 0: 4 columnas (0 a 3), los demás: 3 columnas cada uno
                sheet.addMergedRegion(new CellRangeAddress(1, 1, headerIndex, headerIndex + mergeSize));
                // Para el último grupo se asegura el borde derecho
                if (i == groupHeaders.length - 1) {
                    for (int col = headerIndex; col <= headerIndex + mergeSize; col++) {
                        Cell mergedCell = headerRow.getCell(col);
                        if (mergedCell == null) {
                            mergedCell = headerRow.createCell(col);
                            mergedCell.setCellValue(groupHeaders[i]);
                        }
                        CellStyle tempStyle = workbook.createCellStyle();
                        tempStyle.cloneStyleFrom(groupHeaderStyle);
                        tempStyle.setBorderRight(BorderStyle.THIN);
                        mergedCell.setCellStyle(tempStyle);
                    }
                }
                headerIndex += mergeSize + 1;
            }

            // --- Fila 2: Subencabezados ---
            Row subHeaderRow = sheet.createRow(2);
            // Solo el primer grupo muestra "Site"; en los demás no.
            String[] subHeaders = {
                "Site", "AWF", "Temp", "Capacity",
                "AWF", "Temp", "Capacity",
                "AWF", "Temp", "Capacity",
                "AWF", "Temp", "Capacity"
            };
            CellStyle subHeaderStyle = workbook.createCellStyle();
            subHeaderStyle.cloneStyleFrom(headerStyle);
            subHeaderStyle.setFillForegroundColor(IndexedColors.SKY_BLUE.getIndex());
            subHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            for (int i = 0; i < subHeaders.length; i++) {
                Cell cell = subHeaderRow.createCell(i);
                cell.setCellValue(subHeaders[i]);
                cell.setCellStyle(subHeaderStyle);
            }
            // --- Estilos para celdas de datos ---
            // Celdas sin bordes para las columnas que NO sean "Capacity"
            CellStyle dataNoBorderStyle = workbook.createCellStyle();
            dataNoBorderStyle.setBorderTop(BorderStyle.NONE);
            dataNoBorderStyle.setBorderBottom(BorderStyle.NONE);
            dataNoBorderStyle.setBorderLeft(BorderStyle.NONE);
            dataNoBorderStyle.setBorderRight(BorderStyle.NONE);
            // Celdas "Capacity": aplicar solo el borde izquierdo y derecho
            CellStyle dataCapacityStyle = workbook.createCellStyle();
            dataCapacityStyle.setBorderTop(BorderStyle.NONE);
            dataCapacityStyle.setBorderBottom(BorderStyle.NONE);
            dataCapacityStyle.setBorderLeft(BorderStyle.THIN);
            dataCapacityStyle.setBorderRight(BorderStyle.THIN);

            // --- Filas de datos (a partir de la fila 3) ---
            int dataRowCount = reportEndOfYearDTO.getActuals().size();
            for (int i = 0; i < dataRowCount; i++) {
                Row row = sheet.createRow(i + 3);
                // Grupo 0: Actuals (columnas 0 a 3)
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(reportEndOfYearDTO.getActuals().get(i).getReport());
                cell0.setCellStyle(dataNoBorderStyle);
                Cell cell1 = row.createCell(1);
                double awfActual = reportEndOfYearDTO.getActuals().get(i).getAwfFTE();
                cell1.setCellValue(awfActual);
                cell1.setCellStyle(dataNoBorderStyle);
                Cell cell2 = row.createCell(2);
                double tempActual = reportEndOfYearDTO.getActuals().get(i).getTempFTE();
                cell2.setCellValue(tempActual);
                cell2.setCellStyle(dataNoBorderStyle);
                Cell cell3 = row.createCell(3);
                double capacityActual = reportEndOfYearDTO.getActuals().get(i).getCapacity();
                cell3.setCellValue(capacityActual);
                cell3.setCellStyle(dataCapacityStyle);

                // Grupo 1: Realistic (columnas 4 a 6)
                int baseCol = 4;
                Cell cell4 = row.createCell(baseCol);
                double awfRealistic = reportEndOfYearDTO.getRealistic().get(i).getAwfFTE();
                cell4.setCellValue(awfRealistic);
                cell4.setCellStyle(dataNoBorderStyle);
                Cell cell5 = row.createCell(baseCol + 1);
                double tempRealistic = reportEndOfYearDTO.getRealistic().get(i).getTempFTE();
                cell5.setCellValue(tempRealistic);
                cell5.setCellStyle(dataNoBorderStyle);
                Cell cell6 = row.createCell(baseCol + 2);
                double capacityRealistic = reportEndOfYearDTO.getRealistic().get(i).getCapacity();
                cell6.setCellValue(capacityRealistic);
                cell6.setCellStyle(dataCapacityStyle);

                // Grupo 2: Validation (columnas 7 a 9)
                baseCol = 7;
                Cell cell7 = row.createCell(baseCol);
                double awfValidation = reportEndOfYearDTO.getValidation().get(i).getAwfFTE();
                cell7.setCellValue(awfValidation);
                cell7.setCellStyle(dataNoBorderStyle);
                Cell cell8 = row.createCell(baseCol + 1);
                double tempValidation = reportEndOfYearDTO.getValidation().get(i).getTempFTE();
                cell8.setCellValue(tempValidation);
                cell8.setCellStyle(dataNoBorderStyle);
                Cell cell9 = row.createCell(baseCol + 2);
                double capacityValidation = reportEndOfYearDTO.getValidation().get(i).getCapacity();
                cell9.setCellValue(capacityValidation);
                cell9.setCellStyle(dataCapacityStyle);

                // Grupo 3: Optimistic (columnas 10 a 12)
                baseCol = 10;
                Cell cell10 = row.createCell(baseCol);
                double awfOptimistic = reportEndOfYearDTO.getOptimistic().get(i).getAwfFTE();
                cell10.setCellValue(awfOptimistic);
                cell10.setCellStyle(dataNoBorderStyle);
                Cell cell11 = row.createCell(baseCol + 1);
                double tempOptimistic = reportEndOfYearDTO.getOptimistic().get(i).getTempFTE();
                cell11.setCellValue(tempOptimistic);
                cell11.setCellStyle(dataNoBorderStyle);
                Cell cell12 = row.createCell(baseCol + 2);
                double capacityOptimistic = reportEndOfYearDTO.getOptimistic().get(i).getCapacity();
                cell12.setCellValue(capacityOptimistic);
                cell12.setCellStyle(dataCapacityStyle);
            }

            // --- Fila de totales ---
            int totalRowIndex = dataRowCount + 3;
            Row totalRow = sheet.createRow(totalRowIndex);
            // Estilo base para totales (fondo azul con bordes superior e inferior)
            CellStyle totalRowBaseStyle = workbook.createCellStyle();
            totalRowBaseStyle.cloneStyleFrom(cellStyle);
            totalRowBaseStyle.setFillForegroundColor(IndexedColors.SKY_BLUE.getIndex());
            totalRowBaseStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalRowBaseStyle.setBorderTop(BorderStyle.THIN);
            totalRowBaseStyle.setBorderBottom(BorderStyle.THIN);

            // Estilos para totales: columnas que NO son Capacity sin bordes laterales
            CellStyle totalNoBorderStyle = workbook.createCellStyle();
            totalNoBorderStyle.cloneStyleFrom(totalRowBaseStyle);
            totalNoBorderStyle.setBorderLeft(BorderStyle.NONE);
            totalNoBorderStyle.setBorderRight(BorderStyle.NONE);
            // Para Capacity, aplicar borde izquierdo y derecho
            CellStyle totalCapacityStyle = workbook.createCellStyle();
            totalCapacityStyle.cloneStyleFrom(totalRowBaseStyle);
            totalCapacityStyle.setBorderLeft(BorderStyle.THIN);
            totalCapacityStyle.setBorderRight(BorderStyle.THIN);

            // Generar la fila total según la nueva estructura (13 columnas):
            // Grupo 0: columnas 0-3
            Cell totCell0 = totalRow.createCell(0);
            totCell0.setCellValue("Total");
            CellStyle tempStyle = workbook.createCellStyle();
            tempStyle.cloneStyleFrom(totalRowBaseStyle);
            tempStyle.setBorderLeft(BorderStyle.THIN);
            totCell0.setCellStyle(tempStyle);
            Cell totCell1 = totalRow.createCell(1);
            totCell1.setCellFormula("SUM(B4:B" + (dataRowCount + 3) + ")");
            totCell1.setCellStyle(totalNoBorderStyle);
            Cell totCell2 = totalRow.createCell(2);
            totCell2.setCellFormula("SUM(C4:C" + (dataRowCount + 3) + ")");
            totCell2.setCellStyle(totalNoBorderStyle);
            Cell totCell3 = totalRow.createCell(3);
            totCell3.setCellFormula("SUM(D4:D" + (dataRowCount + 3) + ")");
            totCell3.setCellStyle(totalCapacityStyle);

            // Grupo 1: Realistic -> columnas 4-6
            int baseCol = 4;
            Cell totCell4 = totalRow.createCell(baseCol);
            totCell4.setCellFormula("SUM(E4:E" + (dataRowCount + 3) + ")");
            totCell4.setCellStyle(totalNoBorderStyle);
            Cell totCell5 = totalRow.createCell(baseCol + 1);
            totCell5.setCellFormula("SUM(F4:F" + (dataRowCount + 3) + ")");
            totCell5.setCellStyle(totalNoBorderStyle);
            Cell totCell6 = totalRow.createCell(baseCol + 2);
            totCell6.setCellFormula("SUM(G4:G" + (dataRowCount + 3) + ")");
            totCell6.setCellStyle(totalCapacityStyle);

            // Grupo 2: Validation -> columnas 7-9
            baseCol = 7;
            Cell totCell7 = totalRow.createCell(baseCol);
            totCell7.setCellFormula("SUM(H4:H" + (dataRowCount + 3) + ")");
            totCell7.setCellStyle(totalNoBorderStyle);
            Cell totCell8 = totalRow.createCell(baseCol + 1);
            totCell8.setCellFormula("SUM(I4:I" + (dataRowCount + 3) + ")");
            totCell8.setCellStyle(totalNoBorderStyle);
            Cell totCell9 = totalRow.createCell(baseCol + 2);
            totCell9.setCellFormula("SUM(J4:J" + (dataRowCount + 3) + ")");
            totCell9.setCellStyle(totalCapacityStyle);

            // Grupo 3: Optimistic -> columnas 10-12
            baseCol = 10;
            Cell totCell10 = totalRow.createCell(baseCol);
            totCell10.setCellFormula("SUM(K4:K" + (dataRowCount + 3) + ")");
            totCell10.setCellStyle(totalNoBorderStyle);
            Cell totCell11 = totalRow.createCell(baseCol + 1);
            totCell11.setCellFormula("SUM(L4:L" + (dataRowCount + 3) + ")");
            totCell11.setCellStyle(totalNoBorderStyle);
            Cell totCell12 = totalRow.createCell(baseCol + 2);
            totCell12.setCellFormula("SUM(M4:M" + (dataRowCount + 3) + ")");
            totCell12.setCellStyle(totalCapacityStyle);

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                workbook.write(bos);
                return bos.toByteArray();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                workbook.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public byte[] getReportEmployeesEndOfYear(
            MultiValueMap<String, String> params,
            String userSelected,
            int yearFilter) throws IOException {

        try {
            // Obtener especificaciones y datos necesarios
            Specification<Employee> employeeSpec = employeeSpecification.getSpecifications(params);
            List<Employee> employees = employeeRepository.findAll(employeeSpec);

            List<Siglum> siglumVisibilityList = utils.getSiglumVisibilityList(userSelected);

            List<Employee> employeesFiltered =
                    employeeRepository.getEmployeesFilteredBySiglumVisibility(employees, siglumVisibilityList);

            // Crear el libro de Excel
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("End of Year Report");

            // Crear estilos
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setBorderBottom(BorderStyle.THIN);
            cellStyle.setBorderTop(BorderStyle.THIN);
            cellStyle.setBorderLeft(BorderStyle.THIN);
            cellStyle.setBorderRight(BorderStyle.THIN);


            // Crear sub-encabezados (fila 0)
            Row subHeaderRow = sheet.createRow(0);
            String[] subHeaders = {"Siglum6", "Siglum5", "Siglum4", "Country", "Site", "id", "FirstName",
                    "LastName", "Job", "AvailabilityReason", "ActiveWorkforce", "Direct/Indirect", "ContractType",
                    "KAPIS code", "CostCenter", "Collar", "FTE"};

            // Crear estilo con fondo azul más claro "SKY_BLUE"
            CellStyle subHeaderStyleWithColor = workbook.createCellStyle();
            subHeaderStyleWithColor.cloneStyleFrom(headerStyle); // Clonar estilo base
            subHeaderStyleWithColor.setFillForegroundColor(IndexedColors.SKY_BLUE.getIndex());
            subHeaderStyleWithColor.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Pintar el recuadro alrededor del grupo de celdas de A1 a M1
            for (int i = 0; i < subHeaders.length; i++) {
                Cell cell = subHeaderRow.createCell(i);
                cell.setCellValue(subHeaders[i]);
                cell.setCellStyle(subHeaderStyleWithColor);
            }

            // Llenar todas las filas de datos
            int rowIndex = 1; // Contador manual para las filas

            for (Employee employee : employeesFiltered) {
                Row row = sheet.createRow(rowIndex++);

                try {
                    for (int j = 0; j < subHeaders.length; j++) {
                        Cell cell = row.createCell(j);

                        Object cellValue = switch (subHeaders[j]) {
                            case "Siglum6" -> employee.getSiglum() != null ? employee.getSiglum().getSiglum6() : "0";
                            case "Siglum5" -> employee.getSiglum() != null ? employee.getSiglum().getSiglum5() : "0";
                            case "Siglum4" -> employee.getSiglum() != null ? employee.getSiglum().getSiglum4() : "0";
                            case "Country" -> {
                                CostCenter costCenter = employee.getCostCenter();
                                yield (costCenter != null && costCenter.getLocation() != null) ? costCenter.getLocation().getCountry() : "";
                            }
                            case "Site" -> {
                                CostCenter costCenter = employee.getCostCenter();
                                yield (costCenter != null && costCenter.getLocation() != null) ? costCenter.getLocation().getSite() : "";
                            }
                            case "id" -> employee.getId();
                            case "FirstName" -> employee.getFirstName();
                            case "LastName" -> employee.getLastName();
                            case "Job" -> employee.getJob();
                            case "AvailabilityReason" -> employee.getAvailabilityReason();
                            case "ActiveWorkforce" -> employee.getActiveWorkforce();
                            case "Direct/Indirect" -> employee.getDirect();
                            case "ContractType" -> employee.getContractType();
                            case "KAPIS code" -> {
                                CostCenter costCenter = employee.getCostCenter();
                                yield (costCenter != null && costCenter.getLocation() != null) ? costCenter.getLocation().getKapisCode() : "0";
                            }
                            case "CostCenter" -> employee.getCostCenter() != null ? employee.getCostCenter().getId() : "0";
                            case "Collar" -> employee.getCollar();
                            case "FTE" -> employee.getFTE();
                            default -> "";
                        };

                        if (cellValue != null) {
                            if (cellValue instanceof Number) {
                                cell.setCellValue(((Number) cellValue).doubleValue());
                            } else {
                                cell.setCellValue(cellValue.toString());
                            }
                        } else {
                            cell.setCellValue("");
                        }

                        cell.setCellStyle(cellStyle); // Aplicar estilo con bordes y alineación a la izquierda
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Error procesando empleado: " + employee.getId() + " - " + e.getMessage());
                }
            }

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                workbook.write(bos);
                return bos.toByteArray();
            } finally {
                workbook.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
}