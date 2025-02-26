package com.airbus.optim.controller;

import com.airbus.optim.dto.*;
import com.airbus.optim.entity.Employee;
import com.airbus.optim.repository.EmployeeRepository;
import com.airbus.optim.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("employee")
public class EmployeeController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeService employeeService;

    @GetMapping("/sum-levers-by-type")
    public List<Map<String, Object>> getSumOfLeversGroupedByType(@RequestParam MultiValueMap<String, String> params) {
        return employeeService.getSumOfLeversGroupedByType(params);
    }

    @GetMapping("/monthly-distribution")
    public MonthlyDistributionDTO getMonthlyDistribution(@RequestParam MultiValueMap<String, String> params, @RequestParam int yearFilter) {
        return employeeService.getMonthlyDistribution(params, yearFilter);
    }

    @GetMapping("/team-outlook")
    public ResponseEntity<TeamOutlookDTO> getTeamOutlook(@RequestParam MultiValueMap<String, String> params, @RequestParam int yearFilter) {
        return employeeService.getTeamOutlook(params, yearFilter);
    }

    @GetMapping
    public Page<Employee> getEmployees(@RequestParam MultiValueMap<String, String> params, Pageable pageable) {
        return employeeService.filterEmployees(params, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable Long id) {
        Optional<Employee> employee = employeeRepository.findById(id);
        return employee.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("/indirect-radio")
    public IndirectRadioDTO getIndirectRadio(@RequestParam MultiValueMap<String, String> params) {
        return employeeService.getIndirectRadio(params);
    }

    @GetMapping("/naws-by-reason")
    public ResponseEntity<List<NawsGroupedByReasonDTO>> getNawsGroupedByReason(
            @RequestParam MultiValueMap<String, String> params) {
        List<NawsGroupedByReasonDTO> result = employeeService.getNawsGroupedByReason(params);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/borrowed-vs-leased")
    public ResponseEntity<BorrowedLeasedDTO> getBorrowedLeasedDTO(
            @RequestParam MultiValueMap<String, String> params,
            @RequestParam int yearFilter) {
        BorrowedLeasedDTO result = employeeService.getBorrowedLeasedDTO(params, yearFilter);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Employee> createEmployee(@RequestBody Employee employee) {
        Employee savedEmployee = employeeService.saveEmployee(employee);
        return new ResponseEntity<>(savedEmployee, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Employee> updateEmployee(@PathVariable Long id, @RequestBody Employee employeeDetails) {
        Employee savedEmployee = employeeService.updateEmployee(id, employeeDetails);
        return new ResponseEntity<>(savedEmployee, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        if (employeeRepository.existsById(id)) {
            employeeRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
