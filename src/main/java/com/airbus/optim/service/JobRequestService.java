package com.airbus.optim.service;

import com.airbus.optim.dto.JobRequestTypeCountDTO;
import com.airbus.optim.entity.CostCenter;
import com.airbus.optim.entity.Employee;
import com.airbus.optim.entity.JobRequest;
import com.airbus.optim.entity.Siglum;
import com.airbus.optim.repository.CostCenterRepository;
import com.airbus.optim.repository.EmployeeRepository;
import com.airbus.optim.repository.JobRequestRepository;
import com.airbus.optim.repository.SiglumRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JobRequestService {

    @Autowired
    private JobRequestRepository jobRequestRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CostCenterRepository costCenterRepository;

    @Autowired
    private SiglumRepository siglumRepository;

    @Autowired
    private EmployeeSpecification employeeSpecification;

    @Autowired
    private JobRequestSpecification jobRequestSpecification;

    public JobRequest updateJobRequest(Long id, JobRequest jobRequestDetails) {

        JobRequest existingJobRequest = jobRequestRepository.findById(jobRequestDetails.getId())
                .orElseThrow(() -> new EntityNotFoundException("JobRequest not found: " + jobRequestDetails.getId()));

        updateJobRequestFields(existingJobRequest, jobRequestDetails);

        handleEmployees(existingJobRequest, jobRequestDetails);
        handleCostCenter(existingJobRequest, jobRequestDetails);
        handleSiglum(existingJobRequest, jobRequestDetails);

        JobRequest updatedJobRequest = jobRequestRepository.save(existingJobRequest);
        return updatedJobRequest;
    }

    public JobRequest createJobRequest(JobRequest jobRequest) {
        handleEmployees(jobRequest);
        handleCostCenter(jobRequest);
        handleSiglum(jobRequest);

        return jobRequestRepository.save(jobRequest);
    }

    private void updateJobRequestFields(JobRequest existingJobRequest, JobRequest jobRequestDetails) {
        existingJobRequest.setWorkdayNumber(jobRequestDetails.getWorkdayNumber());
        existingJobRequest.setType(jobRequestDetails.getType());
        existingJobRequest.setStatus(jobRequestDetails.getStatus());
        existingJobRequest.setDescription(jobRequestDetails.getDescription());
        existingJobRequest.setCandidate(jobRequestDetails.getCandidate());
        existingJobRequest.setStartDate(jobRequestDetails.getStartDate());
        existingJobRequest.setReleaseDate(jobRequestDetails.getReleaseDate());
        existingJobRequest.setPostingDate(jobRequestDetails.getPostingDate());
        existingJobRequest.setExternal(jobRequestDetails.getExternal());
        existingJobRequest.setEarlyCareer(jobRequestDetails.getEarlyCareer());
        existingJobRequest.setOnTopHct(jobRequestDetails.getOnTopHct());
        existingJobRequest.setIsCritical(jobRequestDetails.getIsCritical());
        existingJobRequest.setActiveWorkforce(jobRequestDetails.getActiveWorkforce());
        existingJobRequest.setApprovedQMC(jobRequestDetails.getApprovedQMC());
        existingJobRequest.setApprovedSHRBPHOT1Q(jobRequestDetails.getApprovedSHRBPHOT1Q());
        existingJobRequest.setApprovedHOCOOHOHRCOO(jobRequestDetails.getApprovedHOCOOHOHRCOO());
        existingJobRequest.setApprovedEmploymentCommitee(jobRequestDetails.getApprovedEmploymentCommitee());
        existingJobRequest.setDirect(jobRequestDetails.getDirect());
        existingJobRequest.setCollar(jobRequestDetails.getCollar());
    }

    private void handleEmployees(JobRequest jobRequest) {
        if (jobRequest.getEmployees() != null && !jobRequest.getEmployees().isEmpty()) {
            Set<Employee> savedEmployees = jobRequest.getEmployees().stream().map(employee -> {
                return employee.getId() != null
                        ? employeeRepository.findById(employee.getId())
                        .orElseThrow(() -> new EntityNotFoundException("Employee not found"))
                        : employeeRepository.save(employee);
            }).collect(Collectors.toSet());
            jobRequest.setEmployees(new ArrayList<>(savedEmployees));
        }
    }

    private void handleEmployees(JobRequest existingJobRequest, JobRequest jobRequestDetails) {
        if (jobRequestDetails.getEmployees() != null && !jobRequestDetails.getEmployees().isEmpty()) {
            Set<Employee> savedEmployees = jobRequestDetails.getEmployees().stream().map(employee -> {
                return employee.getId() != null
                        ? employeeRepository.findById(employee.getId())
                        .orElseThrow(() -> new EntityNotFoundException("Employee not found"))
                        : employeeRepository.save(employee);
            }).collect(Collectors.toSet());
            existingJobRequest.setEmployees(new ArrayList<>(savedEmployees));
        }
    }

    private void handleCostCenter(JobRequest jobRequest) {
        saveCostCenter(jobRequest, jobRequest.getCostCenter());
    }

    private void handleCostCenter(JobRequest existingJobRequest, JobRequest jobRequestDetails) {
        saveCostCenter(existingJobRequest, jobRequestDetails.getCostCenter());
    }

    private void saveCostCenter(JobRequest jobRequest, CostCenter costCenter) {
        if (costCenter != null) {
            CostCenter savedCostCenter = costCenter.getId() != null
                    ? costCenterRepository.findById(costCenter.getId())
                    .orElseThrow(() -> new EntityNotFoundException("CostCenter not found with id: " + costCenter.getId()))
                    : costCenterRepository.save(costCenter);
            jobRequest.setCostCenter(savedCostCenter);
        }
    }

    private void handleSiglum(JobRequest jobRequest) {
        saveSiglum(jobRequest, jobRequest.getSiglum());
    }

    private void handleSiglum(JobRequest existingJobRequest, JobRequest jobRequestDetails) {
        saveSiglum(existingJobRequest, jobRequestDetails.getSiglum());
    }

    private void saveSiglum(JobRequest jobRequest, Siglum siglum) {
        if (siglum != null) {
            Siglum savedSiglum = siglum.getId() != null
                    ? siglumRepository.findById(siglum.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Siglum not found with id: " + siglum.getId()))
                    : siglumRepository.save(siglum);
            jobRequest.setSiglum(savedSiglum);
        }
    }

    public List<JobRequestTypeCountDTO> countJobRequestsByTypeAndYear(MultiValueMap<String, String> params, int yearFilter) {
        Specification<JobRequest> spec = jobRequestSpecification.getSpecifications(params);
        List<JobRequest> jobRequestList = jobRequestRepository.findAll(spec);
        return jobRequestRepository.countJobRequestsByType(jobRequestList);
    }

    public Page<JobRequest> filterJobRequests(MultiValueMap<String, String> params, Pageable pageable) {
        Specification<JobRequest> spec = jobRequestSpecification.getSpecifications(params);
        return jobRequestRepository.findAll(spec, pageable);
    }
}