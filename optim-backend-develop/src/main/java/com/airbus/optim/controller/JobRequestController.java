package com.airbus.optim.controller;

import com.airbus.optim.dto.JobRequestTypeCountDTO;
import com.airbus.optim.dto.ReportEndOfYear.EmployeeReportDTO;
import com.airbus.optim.entity.JobRequest;
import com.airbus.optim.repository.JobRequestRepository;
import com.airbus.optim.service.JobRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("job-requests")
public class JobRequestController {

    @Autowired
    private JobRequestService jobRequestService;

    @Autowired
    private JobRequestRepository jobRequestRepository;

    @GetMapping
    public Page<JobRequest> getJobRequests(@RequestParam MultiValueMap<String, String> params, Pageable pageable) {
        return jobRequestService.filterJobRequests(params, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobRequest> getJobRequestById(@PathVariable Long id) {
        Optional<JobRequest> jobRequest = jobRequestRepository.findById(id);
        return jobRequest.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/count-by-type")
    public ResponseEntity<List<JobRequestTypeCountDTO>> countJobRequestsByTypeAndYear(
            @RequestParam MultiValueMap<String, String> params,
            @RequestParam int yearFilter) {
        List<JobRequestTypeCountDTO> result = jobRequestService.countJobRequestsByTypeAndYear(params, yearFilter);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<JobRequest> createJobRequest(@RequestBody JobRequest jobRequest) {
        JobRequest savedJobRequest = jobRequestService.createJobRequest(jobRequest);
        return new ResponseEntity<>(savedJobRequest, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobRequest> updateJobRequest(@PathVariable Long id, @RequestBody JobRequest jobRequestDetails) {
        JobRequest savedJobRequest = jobRequestService.updateJobRequest(id, jobRequestDetails);
        return new ResponseEntity<>(savedJobRequest, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJobRequest(@PathVariable Long id) {
        if (jobRequestRepository.existsById(id)) {
            jobRequestRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/reports/jobRequest-closed")
    public ResponseEntity<byte[]> getReportJobRequestClosed(
            @RequestParam MultiValueMap<String, String> params,
            @RequestParam String userSelected,
            @RequestParam int yearFilter) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "JobRequestClosedReport.xlsx"); // Actualizado a .xlsx

        return new ResponseEntity<>(
                jobRequestService.getReportJobRequestClosed(params, userSelected, yearFilter),
                headers,
                HttpStatus.OK);
    }
 
    @GetMapping("/reports/jobRequest-by-status")
    public ResponseEntity<byte[]> getReportJobRerquestByStatus(
            @RequestParam MultiValueMap<String, String> params,
            @RequestParam String userSelected,
            @RequestParam int yearFilter) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "JobRequestStatusReport.csv");

        return new ResponseEntity<>(
                jobRequestService.getReportJobRerquestByStatus(params, userSelected, yearFilter),
                headers,
                HttpStatus.OK);
    }    
}
