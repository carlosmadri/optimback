package com.airbus.optim.controller;

import com.airbus.optim.entity.Lever;
import com.airbus.optim.repository.LeverRepository;
import com.airbus.optim.service.LeverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("levers")
public class LeverController {

    @Autowired
    LeverRepository leverRepository;

    @Autowired
    LeverService leverService;

    @GetMapping
    public ResponseEntity<List<Lever>> getAllLevers() {
        List<Lever> levers = leverRepository.findAll();
        return ResponseEntity.ok(levers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Lever> getLeverById(@PathVariable Long id) {
        Optional<Lever> lever = leverRepository.findById(id);
        return lever.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Lever> createLever(@RequestBody Lever lever) {
        Lever savedLever = leverService.saveLever(lever);
        return new ResponseEntity<>(savedLever, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Lever> updateLever(@PathVariable Long id, @RequestBody Lever lever) {
        Lever savedLever = leverService.updateLever(id, lever);
        return new ResponseEntity<>(savedLever, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLever(@PathVariable Long id) {
        if (leverRepository.existsById(id)) {
            leverRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}