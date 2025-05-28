package com.airbus.optim.controller;

import com.airbus.optim.dto.*;
import com.airbus.optim.dto.ReportEndOfYear.ReportEndOfYearDTO;
import com.airbus.optim.entity.Employee;
import com.airbus.optim.repository.EmployeeRepository;
import com.airbus.optim.service.EmployeeService;
import com.airbus.optim.utils.Utils;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @Autowired
    private Utils utils;

    @GetMapping("/sum-levers-by-type")
    public List<Map<String, Object>> getSumOfLeversGroupedByType(@RequestParam MultiValueMap<String, String> params) {
        return employeeService.getSumOfLeversGroupedByType(params);
    }

    @GetMapping("/monthly-distribution")
    public ResponseEntity<MonthlyDistributionDTO> getMonthlyDistribution(
            @RequestParam MultiValueMap<String, String> params,
            @RequestParam String userSelected,
            @RequestParam int yearFilter) {
        return new ResponseEntity<>(employeeService.getMonthlyDistribution(
                params, utils.getSiglumVisibilityList(userSelected), userSelected, yearFilter), HttpStatus.OK);
    }

    @GetMapping("/team-outlook")
    public ResponseEntity<TeamOutlookDTO> getTeamOutlook(
            @RequestParam MultiValueMap<String, String> params,
            @RequestParam String userSelected,
            @RequestParam int yearFilter) {
        return employeeService.getTeamOutlook(params, utils.getSiglumVisibilityList(userSelected), userSelected, yearFilter);
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
    public IndirectRadioDTO getIndirectRadio(
            @RequestParam MultiValueMap<String, String> params, @RequestParam String userSelected) {
        return employeeService.getIndirectRadio(params, utils.getSiglumVisibilityList(userSelected));
    }

    @GetMapping("/naws-by-reason")
    public ResponseEntity<List<NawsGroupedByReasonDTO>> getNawsGroupedByReason(
            @RequestParam MultiValueMap<String, String> params, @RequestParam String userSelected) {
        List<NawsGroupedByReasonDTO> result =
                employeeService.getNawsGroupedByReason(params, utils.getSiglumVisibilityList(userSelected));
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/borrowed-vs-leased")
    public ResponseEntity<BorrowedLeasedDTO> getBorrowedLeasedDTO(
            @RequestParam MultiValueMap<String, String> params,
            @RequestParam String userSelected,
            @RequestParam int yearFilter) {
        BorrowedLeasedDTO result = employeeService.getBorrowedLeasedDTO(
                params, utils.getSiglumVisibilityList(userSelected), yearFilter);
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
  
    @GetMapping("/reports/employee-workforce")
    public ResponseEntity<byte[]> getReportFromEmployee(
            @RequestParam MultiValueMap<String, String> params,
            @RequestParam String userSelected,
            @RequestParam int yearFilter) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "EmployeeWorkforceReport.xlsx");

        return new ResponseEntity<>(
                employeeService.getReportFromEmployeeeWorkforce(params, userSelected, yearFilter),
                headers,
                HttpStatus.OK);
    }

    @GetMapping("/reports/siglum4-end-of-year")
    public ResponseEntity<byte[]> getReportSiglum4EndOfYear(
            @RequestParam MultiValueMap<String, String> params,
            @RequestParam String userSelected,
            @RequestParam int yearFilter) {

        byte[] reportContent = employeeService.getReportSiglum4EndOfYear(params, userSelected, yearFilter);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename("EmployeeSiglum4Report.xlsx") // Cambiado para reflejar el formato correcto (.xlsx en lugar de .csv)
                .build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(reportContent);
    }  
    
    @GetMapping("/reports/siglum5-end-of-year")
    public ResponseEntity<byte[]> getReportSiglum5EndOfYear(
            @RequestParam MultiValueMap<String, String> params,
            @RequestParam String userSelected,
            @RequestParam int yearFilter) {

        byte[] reportBytes = employeeService.getReportSiglum5EndOfYear(params, userSelected, yearFilter);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "EmployeeSiglum5Report.xls");

        return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
    }

 
    @GetMapping("/reports/site-end-of-year")
    public ResponseEntity<byte[]> getReportSiteEndOfYear(
            @RequestParam MultiValueMap<String, String> params,
            @RequestParam String userSelected,
            @RequestParam int yearFilter) {

        byte[] reportBytes = employeeService.getReportSiteEndOfYear(params, userSelected, yearFilter);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "EmployeeSiteReport.xls");

        return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/reports/employees-end-of-year")
    public ResponseEntity<byte[]> getReportEmployeesEndOfYear(
            @RequestParam MultiValueMap<String, String> params,
            @RequestParam String userSelected,
            @RequestParam int yearFilter) {

        byte[] reportContent;
        try {
            reportContent = employeeService.getReportEmployeesEndOfYear(params, userSelected, yearFilter);
            if (reportContent == null) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename("EmployeeReport.xlsx")
                .build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(reportContent);
    }

}
