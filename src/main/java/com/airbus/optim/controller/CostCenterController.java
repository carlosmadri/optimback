package com.airbus.optim.controller;

import com.airbus.optim.entity.CostCenter;
import com.airbus.optim.repository.CostCenterRepository;
import com.airbus.optim.service.CostCenterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("cost-centers")
public class CostCenterController {

    @Autowired
    private CostCenterService costCenterService;

    @Autowired
    private CostCenterRepository costCenterRepository;

    @GetMapping
    public ResponseEntity<Page<CostCenter>> getAllCostCenters(Pageable pageable) {
        Page<CostCenter> costCenters = costCenterRepository.findAll(pageable);
        return ResponseEntity.ok(costCenters);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CostCenter> getCostCenterById(@PathVariable Long id) {
        Optional<CostCenter> costCenter = costCenterRepository.findById(id);
        return costCenter.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CostCenter> createCostCenter(@RequestBody CostCenter costCenter) {
        CostCenter savedCostCenter = costCenterService.saveCostCenter(costCenter);
        return new ResponseEntity<>(savedCostCenter, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CostCenter> updateCostCenter(@PathVariable Long id, @RequestBody CostCenter costCenterDetails) {
        Optional<CostCenter> updatedCostCenter = costCenterService.updateCostCenter(id, costCenterDetails);
        return updatedCostCenter.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCostCenter(@PathVariable Long id) {
        if (costCenterRepository.existsById(id)) {
            costCenterRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}