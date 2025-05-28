package com.airbus.optim.controller;

import com.airbus.optim.dto.LocationFteSumDTO;
import com.airbus.optim.entity.Location;
import com.airbus.optim.repository.LocationRepository;
import com.airbus.optim.service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("locations")
public class LocationController {

    @Autowired
    private LocationService locationService;

    @Autowired
    private LocationRepository locationRepository;

    @GetMapping
    public ResponseEntity<List<Location>> getAllLocations() {
        List<Location> locations = locationRepository.findAll();
        return ResponseEntity.ok(locations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Location> getLocationById(@PathVariable Long id) {
        Optional<Location> location = locationRepository.findById(id);
        return location.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/coordinates-by-location")
    public ResponseEntity<List<LocationFteSumDTO>> getAllLocationsWithCount(
            @RequestParam MultiValueMap<String, String> params) {
        List<LocationFteSumDTO> locations = locationService.getAllLocationsWithFteSum(params);
        return new ResponseEntity<>(locations, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Location> createLocation(@RequestBody Location location) {
        Location savedLocation = locationService.saveOrUpdateLocation(location);
        return new ResponseEntity<>(savedLocation, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Location> updateLocation(@PathVariable Long id, @RequestBody Location locationDetails) {
        return locationService.updateLocation(id, locationDetails);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocation(@PathVariable Long id) {
        if (locationRepository.existsById(id)) {
            locationRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
