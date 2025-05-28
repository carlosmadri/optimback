package com.airbus.optim.service;

import com.airbus.optim.entity.Employee;
import com.airbus.optim.entity.HeadCount;
import com.airbus.optim.entity.JobRequest;
import com.airbus.optim.entity.PurchaseOrders;
import com.airbus.optim.entity.Siglum;
import com.airbus.optim.entity.User;
import com.airbus.optim.entity.Workload;
import com.airbus.optim.repository.SiglumRepository;
import com.airbus.optim.repository.EmployeeRepository;
import com.airbus.optim.repository.JobRequestRepository;
import com.airbus.optim.repository.HeadCountRepository;
import com.airbus.optim.repository.PurchaseOrdersRepository;
import com.airbus.optim.repository.WorkloadRepository;
import com.airbus.optim.utils.Utils;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SiglumService {

    @Autowired
    private SiglumRepository siglumRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private JobRequestRepository jobRequestRepository;

    @Autowired
    private HeadCountRepository headCountRepository;

    @Autowired
    private PurchaseOrdersRepository purchaseOrdersRepository;

    @Autowired
    private WorkloadRepository workloadRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private Utils utils;

    public Siglum saveOrUpdateSiglum(Siglum siglum) {
        handleEmployee(siglum);
        handleJobRequests(siglum);
        handleHeadCounts(siglum);
        handlePurchaseOrders(siglum);
        handleWorkload(siglum);
        return siglumRepository.save(siglum);
    }

    public Optional<Siglum> updateSiglum(Long id, Siglum siglumDetails) {
        Optional<Siglum> optionalSiglum = siglumRepository.findById(id);

        if (optionalSiglum.isPresent()) {
            Siglum existingSiglum = optionalSiglum.get();
            updateSiglumFields(existingSiglum, siglumDetails);

            handleEmployee(siglumDetails);
            handleJobRequests(siglumDetails);
            handleHeadCounts(siglumDetails);
            handlePurchaseOrders(siglumDetails);
            handleWorkload(siglumDetails);

            return Optional.of(siglumRepository.save(existingSiglum));
        } else {
            return Optional.empty();
        }
    }

    private void updateSiglumFields(Siglum existingSiglum, Siglum siglumDetails) {
        existingSiglum.setSiglumHR(siglumDetails.getSiglumHR());
        existingSiglum.setSiglum6(siglumDetails.getSiglum6());
        existingSiglum.setSiglum5(siglumDetails.getSiglum5());
        existingSiglum.setSiglum4(siglumDetails.getSiglum4());
        existingSiglum.setSiglum3(siglumDetails.getSiglum3());
    }

    private void handleEmployee(Siglum siglum) {
        if (siglum.getEmployees() != null && !siglum.getEmployees().isEmpty()) {
            for (Employee employee : siglum.getEmployees()) {
                if (employee.getId() != null) {
                    Employee existingEmployee = employeeRepository.findById(employee.getId())
                            .orElseThrow(() -> new EntityNotFoundException("Employee not found"));
                    existingEmployee.setSiglum(siglum);
                    employeeRepository.save(existingEmployee);
                } else {
                    employee.setSiglum(siglum);
                    employeeRepository.save(employee);
                }
            }
        }
    }

    private void handleJobRequests(Siglum siglum) {
        if (siglum.getJobRequests() != null && !siglum.getJobRequests().isEmpty()) {
            List<JobRequest> savedJobRequests = siglum.getJobRequests().stream().map(jobRequest -> {
                if (jobRequest.getId() == null) {
                    throw new IllegalArgumentException("Cannot assign a new JobRequest. Only existing JobRequests are allowed.");
                }
                return jobRequestRepository.findById(jobRequest.getId())
                        .orElseThrow(() -> new EntityNotFoundException("JobRequest not found with ID: " + jobRequest.getId()));
            }).collect(Collectors.toList());
            siglum.setJobRequests(savedJobRequests);
        }
    }

    private void handleHeadCounts(Siglum siglum) {
        if (siglum.getHeadCounts() != null && !siglum.getHeadCounts().isEmpty()) {
            List<HeadCount> savedHeadCounts = siglum.getHeadCounts().stream().map(headCount -> {
                if (headCount.getId() == null) {
                    throw new IllegalArgumentException("Cannot assign a new HeadCount. Only existing HeadCounts are allowed.");
                }
                return headCountRepository.findById(headCount.getId())
                        .orElseThrow(() -> new EntityNotFoundException("HeadCount not found with ID: " + headCount.getId()));
            }).collect(Collectors.toList());
            siglum.setHeadCounts(savedHeadCounts);
        }
    }

    private void handlePurchaseOrders(Siglum siglum) {
        if (siglum.getPurchaseOrders() != null && !siglum.getPurchaseOrders().isEmpty()) {
            List<PurchaseOrders> savedPurchaseOrders = siglum.getPurchaseOrders().stream().map(purchaseOrder -> {
                if (purchaseOrder.getId() == null) {
                    throw new IllegalArgumentException("Cannot assign a new PurchaseOrder. Only existing PurchaseOrders are allowed.");
                }
                return purchaseOrdersRepository.findById(purchaseOrder.getId())
                        .orElseThrow(() -> new EntityNotFoundException("PurchaseOrder not found with ID: " + purchaseOrder.getId()));
            }).collect(Collectors.toList());
            siglum.setPurchaseOrders(savedPurchaseOrders);
        }
    }

    private void handleWorkload(Siglum siglum) {
        if (siglum.getWorkloads() != null && !siglum.getWorkloads().isEmpty()) {
            List<Workload> savedWorkloads = new ArrayList<>();
            for (Workload workload : siglum.getWorkloads()) {
                Workload savedWorkload = workload.getId() != null
                        ? workloadRepository.findById(workload.getId())
                        .orElseThrow(() -> new EntityNotFoundException("Workload not found"))
                        : workloadRepository.save(workload);
                savedWorkload.setSiglum(siglum);
                savedWorkloads.add(savedWorkload);
            }
            siglum.setWorkloads(savedWorkloads);
        }
    }
}