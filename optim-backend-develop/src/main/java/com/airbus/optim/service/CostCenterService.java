package com.airbus.optim.service;

import com.airbus.optim.entity.CostCenter;
import com.airbus.optim.entity.Location;
import com.airbus.optim.repository.CostCenterRepository;
import com.airbus.optim.repository.LocationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CostCenterService {

    @Autowired
    private CostCenterRepository costCenterRepository;

    @Autowired
    private LocationRepository locationRepository;

    public CostCenter saveCostCenter(CostCenter costCenter) {
        handleLocation(costCenter);
        return costCenterRepository.save(costCenter);
    }

    public Optional<CostCenter> updateCostCenter(Long id, CostCenter costCenterDetails) {
        if (id == null) {
            throw new IllegalArgumentException("CostCenter ID cannot be null.");
        }

        CostCenter existingCostCenter = costCenterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("CostCenter not found with ID: " + id));

        updateCostCenterFields(existingCostCenter, costCenterDetails);
        handleLocation(existingCostCenter, costCenterDetails);

        return Optional.of(costCenterRepository.save(existingCostCenter));
    }

    private void updateCostCenterFields(CostCenter existingCostCenter, CostCenter costCenterDetails) {
        if (costCenterDetails.getCostCenterCode() != null) {
            existingCostCenter.setCostCenterCode(costCenterDetails.getCostCenterCode());
        }
        if (costCenterDetails.getCostCenterFinancialCode() != null) {
            existingCostCenter.setCostCenterFinancialCode(costCenterDetails.getCostCenterFinancialCode());
        }
        if (costCenterDetails.getEfficiency() != null) {
            existingCostCenter.setEfficiency(costCenterDetails.getEfficiency());
        }
        if (costCenterDetails.getRateOwn() != null) {
            existingCostCenter.setRateOwn(costCenterDetails.getRateOwn());
        }
        if (costCenterDetails.getRateSub() != null) {
            existingCostCenter.setRateSub(costCenterDetails.getRateSub());
        }
    }

    private void handleLocation(CostCenter source) {
        if (source.getLocation() != null) {
            if (source.getLocation().getId() != null) {
                Optional<Location> existingLocation = locationRepository.findById(source.getLocation().getId());
                source.setLocation(existingLocation.orElse(null));
            }
        }
    }

    private void handleLocation(CostCenter existingCostCenter, CostCenter costCenterDetails) {
        if (costCenterDetails.getLocation() != null && costCenterDetails.getLocation().getId() != null) {
            Optional<Location> existingLocation = locationRepository.findById(costCenterDetails.getLocation().getId());
            existingCostCenter.setLocation(existingLocation.orElse(null));
        }
    }
}