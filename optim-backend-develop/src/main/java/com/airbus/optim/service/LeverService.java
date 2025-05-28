package com.airbus.optim.service;

import com.airbus.optim.entity.Employee;
import com.airbus.optim.entity.Lever;
import com.airbus.optim.exception.LeverConflictException;
import com.airbus.optim.repository.CostCenterRepository;
import com.airbus.optim.repository.LeverRepository;
import com.airbus.optim.repository.EmployeeRepository;
import com.airbus.optim.repository.SiglumRepository;
import com.airbus.optim.utils.Utils;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LeverService {

    @Autowired
    private LeverRepository leverRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private SiglumRepository siglumRepository;

    @Autowired
    private CostCenterRepository costCenterRepository;

    @Autowired
    private Utils utils;

    public Lever saveLever(Lever lever) {
        if(lever.getEmployee() != null && lever.getEmployee().getId() != null
                && employeeRepository.findById(lever.getEmployee().getId()).isPresent() && utils.checkOverlapOfLevers(lever, lever.getEmployee().getId())){
            throw new LeverConflictException("Levers duplicated in time");
        }

        lever.setId(leverRepository.findNextAvailableId());

        handleEmployee(lever);
        handleSiglumDestination(lever);
        handleSiglumOrigin(lever);
        handleCostCenter(lever);
        return leverRepository.save(lever);
    }

    public Lever updateLever(Long id, Lever updatedLever) {
        if (id == null) {
            throw new EntityNotFoundException("Employee not found: " + id);
        }

        Lever existingLever = leverRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found: " + id));

        updatedLever.setId(id);

        updateLeverFields(existingLever, updatedLever);
        handleEmployee(existingLever, updatedLever);
        handleSiglumDestination(existingLever, updatedLever);
        handleSiglumOrigin(existingLever, updatedLever);
        handleCostCenter(existingLever, updatedLever);

        return leverRepository.save(existingLever);
    }

    private void updateLeverFields(Lever existingLever, Lever leverDetails) {
        existingLever.setLeverType(leverDetails.getLeverType());
        existingLever.setHighlights(leverDetails.getHighlights());
        existingLever.setStartDate(leverDetails.getStartDate());
        existingLever.setEndDate(leverDetails.getEndDate());
        existingLever.setFTE(leverDetails.getFTE());
        existingLever.setDirect(leverDetails.getDirect());
        existingLever.setActiveWorkforce(leverDetails.getActiveWorkforce());
    }

    private void handleEmployee(Lever lever) {
        Employee employee = lever.getEmployee();

        if (employee == null) {
            lever.setEmployee(null);
        } else if (employee.getId() == null && employee.getImpersonal()) {
            lever.setEmployee(employeeService.saveEmployee(employee));
        } else {
            lever.setEmployee(employeeRepository.findById(employee.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Employee not found: " + employee.getId())));
        }
    }

    private void handleEmployee(Lever existingLever, Lever updatedLever) {
        Employee employee = updatedLever.getEmployee();

        if (employee == null || employee.getId() == null) {
            existingLever.setEmployee(null);
        } else if (employee.getImpersonal()) {
            existingLever.setEmployee(employeeService.updateEmployee(employee.getId(), employee));
        } else {
            Employee existingEmployee = employeeRepository.findById(employee.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Employee not found: " + employee.getId()));
            existingLever.setEmployee(existingEmployee);
        }
    }

    private void handleSiglumDestination(Lever lever) {
        if (lever.getSiglumDestination() != null && lever.getSiglumDestination().getId() != null) {
            lever.setSiglumDestination(
                    siglumRepository.findById(lever.getSiglumDestination().getId())
                            .orElseThrow(() -> new EntityNotFoundException("Siglum not found: " + lever.getSiglumDestination().getId()))
            );
        } else {
            lever.setSiglumDestination(null);
        }
    }
    
    private void handleSiglumOrigin(Lever lever) {
        if (lever.getSiglumOrigin() != null && lever.getSiglumOrigin().getId() != null) {
            lever.setSiglumOrigin(
                    siglumRepository.findById(lever.getSiglumOrigin().getId())
                            .orElseThrow(() -> new EntityNotFoundException("Siglum not found: " + lever.getSiglumOrigin().getId()))
            );
        } else {
            lever.setSiglumOrigin(null);
        }
    }
    
    private void handleSiglumDestination(Lever existingLever, Lever updatedLever) {
        if (updatedLever.getSiglumDestination() != null && updatedLever.getSiglumDestination().getId() != null) {
            existingLever.setSiglumDestination(
                    siglumRepository.findById(updatedLever.getSiglumDestination().getId())
                            .orElseThrow(() -> new EntityNotFoundException("Siglum not found: " + updatedLever.getSiglumDestination().getId()))
            );
        } else {
            existingLever.setSiglumDestination(null);
        }
    }

    private void handleSiglumOrigin(Lever existingLever, Lever updatedLever) {
        if (updatedLever.getSiglumOrigin() != null && updatedLever.getSiglumOrigin().getId() != null) {
            existingLever.setSiglumOrigin(
                    siglumRepository.findById(updatedLever.getSiglumOrigin().getId())
                            .orElseThrow(() -> new EntityNotFoundException("Siglum not found: " + updatedLever.getSiglumOrigin().getId()))
            );
        } else {
        	existingLever.setSiglumOrigin(null);  
        }
    }
    
    private void handleCostCenter(Lever lever) {
        if (lever.getCostCenter() != null && lever.getCostCenter().getId() != null) {
            lever.setCostCenter(
                    costCenterRepository.findById(lever.getCostCenter().getId())
                            .orElseThrow(() -> new EntityNotFoundException("CostCenter not found: " + lever.getCostCenter().getId()))
            );
        } else {
            lever.setCostCenter(null);
        }
    }

    private void handleCostCenter(Lever existingLever, Lever updatedLever) {
        if (updatedLever.getCostCenter() != null && updatedLever.getCostCenter().getId() != null) {
            existingLever.setCostCenter(
                    costCenterRepository.findById(updatedLever.getCostCenter().getId())
                            .orElseThrow(() -> new EntityNotFoundException("CostCenter not found: " + updatedLever.getCostCenter().getId()))
            );
        } else {
            existingLever.setCostCenter(null);
        }
    }
}
