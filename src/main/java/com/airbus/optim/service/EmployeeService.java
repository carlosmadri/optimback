package com.airbus.optim.service;

import com.airbus.optim.dto.*;
import com.airbus.optim.entity.*;
import com.airbus.optim.exception.LeverConflictException;
import com.airbus.optim.repository.*;
import com.airbus.optim.repository.projections.LeverTypeFteSum;
import com.airbus.optim.utils.Utils;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

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

    public IndirectRadioDTO getIndirectRadio(MultiValueMap<String, String> params) {
        Specification<Employee> spec = employeeSpecification.getSpecifications(params);
        List<Employee> employees = employeeRepository.findAll(spec);

        double directFteSum = employees.stream()
                .filter(employee -> ("AWF".equalsIgnoreCase(employee.getActiveWorkforce()) ||
                        "TEMP".equalsIgnoreCase(employee.getActiveWorkforce())) &&
                        "Direct".equalsIgnoreCase(employee.getDirect()) &&
                        !employee.getImpersonal())
                .mapToDouble(Employee::getFTE)
                .sum();

        double indirectFteSum = employees.stream()
                .filter(employee -> ("AWF".equalsIgnoreCase(employee.getActiveWorkforce()) ||
                        "TEMP".equalsIgnoreCase(employee.getActiveWorkforce())) &&
                        "Indirect".equalsIgnoreCase(employee.getDirect())&&
                        !employee.getImpersonal())
                .mapToDouble(Employee::getFTE)
                .sum();

        return new IndirectRadioDTO(directFteSum, indirectFteSum);
    }

    public List<NawsGroupedByReasonDTO> getNawsGroupedByReason(MultiValueMap<String, String> params) {
        Specification<Employee> spec = employeeSpecification.getSpecifications(params);
        List<Employee> employees = employeeRepository.findAll(spec);

        return employees.stream()
                .filter(employee -> "NAWF".equals(employee.getActiveWorkforce()))
                .collect(Collectors.groupingBy(
                        employee -> Optional.ofNullable(employee.getAvailabilityReason()).orElse("Unknown"),
                        Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new NawsGroupedByReasonDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public BorrowedLeasedDTO getBorrowedLeasedDTO(MultiValueMap<String, String> params, int yearFilter) {
        Specification<Employee> spec = employeeSpecification.getSpecifications(params);
        List<Employee> employees = employeeRepository.findAll(spec);

        List<Double> borrowedMonthly = new ArrayList<>(Collections.nCopies(12, 0.0));
        List<Double> leasedMonthly = new ArrayList<>(Collections.nCopies(12, 0.0));

        for (int i = 0; i < 12; i++) {
            final Month month = Month.of(i + 1);

            List<Lever> leversForMonth = employees.stream()
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

    public ResponseEntity<TeamOutlookDTO> getTeamOutlook(MultiValueMap<String, String> params, int year) {
        Specification<Employee> spec = employeeSpecification.getSpecifications(params);
        List<Employee> employees = employeeRepository.findAll(spec);

        TeamOutlookDTO teamOutlookDTO = new TeamOutlookDTO();

        LocalDate currentDate = LocalDate.now();
        Instant currentDateInstant = currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant();

        LocalDate startOfNextYear = LocalDate.of(currentDate.getYear() + 1, 1, 1);
        Instant endOfYearInstant = startOfNextYear.atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<Employee> borrowedList = leverRepository.findEmployeesWithBorrowedLever(currentDateInstant, endOfYearInstant, employees);
        List<Employee> leasedList = leverRepository.findEmployeesWithLeasedLever(currentDateInstant, endOfYearInstant, employees);

        Float fteAwf = employeeRepository.sumFTEsByActiveWorkforce("AWF", employees);
        Float fteTemp = employeeRepository.sumFTEsByActiveWorkforce("TEMP", employees);
        Float fteActuals = (fteAwf != null ? fteAwf : 0.0f) + (fteTemp != null ? fteTemp : 0.0f);
        teamOutlookDTO.setFteActives(fteActuals);

        Float fteNonActives = employeeRepository.sumFTEsByActiveWorkforce("NAWF", employees);
        teamOutlookDTO.setFteNonActives(fteNonActives != null ? fteNonActives : 0.0f);

        List<Employee> filteredEmployees = employees.stream()
                .filter(emp -> !borrowedList.contains(emp) && !leasedList.contains(emp))
                .collect(Collectors.toList());

        Float leavers = leverRepository.sumFTEsLeaversAfterStartDate(currentDateInstant, endOfYearInstant, filteredEmployees);
        teamOutlookDTO.setLeavers(leavers != null ? leavers : 0.0f);

        Float recoveries = leverRepository.sumFTEsRecoveriesBeforeEndOfYear(currentDateInstant, endOfYearInstant, filteredEmployees);
        teamOutlookDTO.setRecoveries(recoveries != null ? recoveries : 0.0f);

        Float redeployment = leverRepository.sumFTEsForRedeployment(currentDateInstant, filteredEmployees);
        teamOutlookDTO.setRedeployment(redeployment != null ? redeployment : 0.0f);

        Float perimeterChanges = leverRepository.sumFTEsForPerimeterChanges(currentDateInstant, filteredEmployees);
        teamOutlookDTO.setPerimeterChanges(perimeterChanges != null ? perimeterChanges : 0.0f);

        Specification<JobRequest> jobRequestSpec = jobRequestSpecification.getSpecifications(params);
        List<JobRequest> filteredJobRequests = jobRequestRepository.findAll(jobRequestSpec);

        Float filled = jobRequestRepository.countFilledJobRequestsAfterStartDate(filteredJobRequests, currentDateInstant, endOfYearInstant);
        teamOutlookDTO.setFilled(filled != null ? filled : 0.0f);

        Float opened = jobRequestRepository.countOpenedJobRequestsAfterStartDate(filteredJobRequests, currentDateInstant, endOfYearInstant);
        teamOutlookDTO.setOpened(opened != null ? opened : 0.0f);

        Float validationProcess = jobRequestRepository.countValidationProcessJobRequests(filteredJobRequests, currentDateInstant, endOfYearInstant);
        teamOutlookDTO.setValidationProcess(validationProcess != null ? validationProcess : 0.0f);

        Float onHold = jobRequestRepository.countOnHoldJobRequestsAfterStartDate(filteredJobRequests, currentDateInstant, endOfYearInstant);
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

        Float hcCeiling = headCountRepository.sumTotalFTEForCurrentYear(String.valueOf(currentDateInstant.atZone(ZoneId.systemDefault()).getYear()))
                + (perimeterChanges != null ? perimeterChanges : 0.0f)
                + (redeployment != null ? redeployment : 0.0f);
        teamOutlookDTO.setHcCeiling(hcCeiling != null ? hcCeiling : 0.0f);

        Float internalMobility = leverRepository.sumFTEsForInternalMobility(currentDateInstant, endOfYearInstant, filteredEmployees);
        teamOutlookDTO.setInternalMobility(internalMobility != null ? internalMobility : 0.0f);

        MonthlyDistributionDTO monthlyDistribution = getMonthlyDistribution(params, year);
        Float realisticViewMonthlyAverage = (float) monthlyDistribution.getRealisticView().stream()
                .mapToDouble(value -> value - realisticView)
                .average()
                .orElse(0.0);

        Float validationViewMonthlyAverage = (float) monthlyDistribution.getValidationView().stream()
                .mapToDouble(value -> value - validationView)
                .average()
                .orElse(0.0);

        Float optimisticViewMonthlyAverage = (float) monthlyDistribution.getOptimisticView().stream()
                .mapToDouble(value -> value - optimisticView)
                .average()
                .orElse(0.0);

        teamOutlookDTO.setRealisticViewAverage(realisticViewMonthlyAverage);
        teamOutlookDTO.setValidationViewAverage(validationViewMonthlyAverage);
        teamOutlookDTO.setOptimisticViewAverage(optimisticViewMonthlyAverage);

        return ResponseEntity.ok(teamOutlookDTO);
    }

    public MonthlyDistributionDTO getMonthlyDistribution(MultiValueMap<String, String> params, int year) {
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

            Float leavers = leverRepository.sumFTEsLeaversAfterStartDate(startOfMonthInstant, endOfMonthInstant, employees);
            leaversMonthly.set(i, (double) (leavers != null ? leavers : 0.0f));

            Float recoveries = leverRepository.sumFTEsRecoveriesBeforeEndOfYear(startOfMonthInstant, endOfMonthInstant, employees);
            recoveriesMonthly.set(i, (double) (recoveries != null ? recoveries : 0.0f));

            Float redeployment = leverRepository.sumFTEsForRedeployment(startOfMonthInstant, employees);
            redeploymentMonthly.set(i, (double) (redeployment != null ? redeployment : 0.0f));

            Float perimeterChanges = leverRepository.sumFTEsForPerimeterChanges(startOfMonthInstant, employees);
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

        Float leavers = leverRepository.sumFTEsLeaversAfterStartDate(startOfNextJanuaryInstant, endOfNextJanuaryInstant, employees);
        leaversMonthly.set(12, (double) (leavers != null ? leavers : 0.0f));

        Float recoveries = leverRepository.sumFTEsRecoveriesBeforeEndOfYear(startOfNextJanuaryInstant, endOfNextJanuaryInstant, employees);
        recoveriesMonthly.set(12, (double) (recoveries != null ? recoveries : 0.0f));

        Float redeployment = leverRepository.sumFTEsForRedeployment(startOfNextJanuaryInstant, employees);
        redeploymentMonthly.set(12, (double) (redeployment != null ? redeployment : 0.0f));

        Float perimeterChanges = leverRepository.sumFTEsForPerimeterChanges(startOfNextJanuaryInstant, employees);
        perimeterChangesMonthly.set(12, (double) (perimeterChanges != null ? perimeterChanges : 0.0f));

        Float jobRequestWithFilledOrOpenedStatus = Float.valueOf(jobRequestRepository.countJobRequestsWithFilledOrOpenedStatus(startOfNextJanuaryInstant, endOfNextJanuaryInstant, filteredJobRequests));
        jobRequestWithFilledOrOpenedStatusMonthly.set(12, (double) (jobRequestWithFilledOrOpenedStatus != null ? jobRequestWithFilledOrOpenedStatus : 0.0f));

        Float jobRequestWithValidationOrProgressStatus = Float.valueOf(jobRequestRepository.countJobRequestsWithValidationOrProgressStatus(startOfNextJanuaryInstant, endOfNextJanuaryInstant, filteredJobRequests));
        jobRequestWithValidationOrProgressStatusMonthly.set(12, (double) (jobRequestWithValidationOrProgressStatus != null ? jobRequestWithValidationOrProgressStatus : 0.0f));

        Float jobRequestWithOnHoldStatus = Float.valueOf(jobRequestRepository.countJobRequestsWithOnHoldStatus(startOfNextJanuaryInstant, endOfNextJanuaryInstant, filteredJobRequests));
        jobRequestWithOnHoldStatusMonthly.set(12, (double) (jobRequestWithOnHoldStatus != null ? jobRequestWithOnHoldStatus : 0.0f));

        double realisticViewNextYear = fteActualsMonthly.get(12) + leaversMonthly.get(12) + recoveriesMonthly.get(12) +
                redeploymentMonthly.get(12) + perimeterChangesMonthly.get(12) +
                jobRequestWithFilledOrOpenedStatusMonthly.get(12);
        realisticViewMonthly.set(12, realisticViewNextYear);

        double validationViewNextYear = realisticViewNextYear + jobRequestWithValidationOrProgressStatusMonthly.get(12);
        validationViewMonthly.set(12, validationViewNextYear);

        double optimisticViewNextYear = validationViewNextYear + jobRequestWithOnHoldStatusMonthly.get(12);
        optimisticViewMonthly.set(12, optimisticViewNextYear);

        LocalDate currentDate = LocalDate.now();
        Instant currentDateInstant = currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        String currentYear = String.valueOf(currentDateInstant.atZone(ZoneId.systemDefault()).getYear());
        Float totalFTEForCurrentYear = headCountRepository.sumTotalFTEForCurrentYear(currentYear);
        Float perimeterChangesGeneric = leverRepository.sumFTEsForPerimeterChanges(currentDateInstant, employees);
        Float redeploymentGeneric = leverRepository.sumFTEsForRedeployment(currentDateInstant, employees);
        Float hcCeiling = (totalFTEForCurrentYear != null ? totalFTEForCurrentYear : 0.0f)
                + (perimeterChangesGeneric != null ? redeploymentGeneric : 0.0f);

        return new MonthlyDistributionDTO(realisticViewMonthly, validationViewMonthly, optimisticViewMonthly, op, fcii, bottomUp, hcCeiling);
    }
}