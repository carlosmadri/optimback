package com.airbus.optim.service;

import com.airbus.optim.dto.LocationFteSumDTO;
import com.airbus.optim.entity.Location;
import com.airbus.optim.entity.CostCenter;
import com.airbus.optim.entity.Employee;
import com.airbus.optim.entity.PurchaseOrders;
import com.airbus.optim.repository.LocationRepository;
import com.airbus.optim.repository.CostCenterRepository;
import com.airbus.optim.repository.EmployeeRepository;
import com.airbus.optim.repository.PurchaseOrdersRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class LocationService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private CostCenterRepository costCenterRepository;

    @Autowired
    private PurchaseOrdersRepository purchaseOrdersRepository;

    @Autowired
    private EmployeeSpecification employeeSpecification;

    public Location saveOrUpdateLocation(Location location) {
        handleCostCenters(location);
        handlePurchaseOrders(location);

        return locationRepository.save(location);
    }

    public ResponseEntity<Location> updateLocation(Long id, Location locationDetails) {
        Optional<Location> optionalLocation = locationRepository.findById(id);

        if (optionalLocation.isPresent()) {
            Location existingLocation = optionalLocation.get();
            updateLocationFields(existingLocation, locationDetails);

            handleCostCenters(locationDetails);
            handlePurchaseOrders(locationDetails);

            Location updatedLocation = locationRepository.save(existingLocation);
            return ResponseEntity.ok(updatedLocation);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    private void updateLocationFields(Location existingLocation, Location locationDetails) {
        existingLocation.setCountry(locationDetails.getCountry());
        existingLocation.setSite(locationDetails.getSite());
        existingLocation.setKapisCode(locationDetails.getKapisCode());
    }

    private void handleCostCenters(Location location) {
        if (location.getCostCenters() != null && !location.getCostCenters().isEmpty()) {
            List<CostCenter> savedCostCenters = new ArrayList<>();

            for (CostCenter costCenter : location.getCostCenters()) {
                CostCenter savedCostCenter = costCenter.getId() != null
                        ? costCenterRepository.findById(costCenter.getId())
                        .orElseThrow(() -> new EntityNotFoundException("CostCenter not found"))
                        : costCenterRepository.save(costCenter);
                savedCostCenters.add(savedCostCenter);
            }

            location.setCostCenters(savedCostCenters);
        }
    }

    private void handlePurchaseOrders(Location location) {
        if (location.getPurchaseOrders() != null) {
            PurchaseOrders savedPurchaseOrders = location.getPurchaseOrders().getId() != null
                    ? purchaseOrdersRepository.findById(location.getPurchaseOrders().getId())
                    .orElseThrow(() -> new EntityNotFoundException("PurchaseOrders not found"))
                    : purchaseOrdersRepository.save(location.getPurchaseOrders());
            location.setPurchaseOrders(savedPurchaseOrders);
        }
    }

    public List<LocationFteSumDTO> getAllLocationsWithFteSum(MultiValueMap<String, String> params) {
        Specification<Employee> spec = employeeSpecification.getSpecifications(params);
        List<Employee> employees = employeeRepository.findAll(spec);

        Map<String, LocationFteSumDTO> locationMap = new HashMap<>();

        for (Employee employee : employees) {
            CostCenter costCenter = employee.getCostCenter();

            if (costCenter != null && costCenter.getLocation() != null) {
                Location location = costCenter.getLocation();
                Double longitude = location.getLongitude() != null ? location.getLongitude() : 0.0;
                Double latitude = location.getLatitude() != null ? location.getLatitude() : 0.0;

                String key = location.getSite() + location.getCountry() + longitude + latitude;

                if (locationMap.containsKey(key)) {
                    LocationFteSumDTO existingDto = locationMap.get(key);
                    existingDto.setFteSum(existingDto.getFteSum() + (employee.getFTE() != null ? employee.getFTE() : 0.0));
                } else {
                    LocationFteSumDTO newDto = new LocationFteSumDTO(
                            location.getCountry(),
                            location.getSite(),
                            employee.getFTE() != null ? employee.getFTE() : 0.0,
                            longitude,
                            latitude
                    );
                    locationMap.put(key, newDto);
                }
            }
        }

        return new ArrayList<>(locationMap.values());
    }
}