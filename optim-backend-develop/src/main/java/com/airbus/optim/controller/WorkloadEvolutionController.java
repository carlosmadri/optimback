package com.airbus.optim.controller;

import com.airbus.optim.dto.CanUpdateWorkloadDTO;
import com.airbus.optim.dto.ExistsOpenedExerciseDTO;
import com.airbus.optim.dto.SiglumStatusRequestDTO;
import com.airbus.optim.dto.SiglumStatusResponseDTO;
import com.airbus.optim.repository.WorkloadEvolutionRepository;
import com.airbus.optim.service.WorkloadEvolutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/workload-evolution")
public class WorkloadEvolutionController {

    @Autowired
    private WorkloadEvolutionService workloadEvolutionService;

    @Autowired
    private WorkloadEvolutionRepository workloadEvolutionRepository;

    @GetMapping("/siglums-first-submission")
    public ResponseEntity<SiglumStatusResponseDTO> getSiglumsFirstSubmission(@RequestParam(required = false) String userSelected) {
        SiglumStatusResponseDTO workloadEvolution = workloadEvolutionService.getSiglumsFirstSubmission(userSelected);
        return ResponseEntity.ok(workloadEvolution);
    }

    @GetMapping("/siglums-status")
    public ResponseEntity<SiglumStatusResponseDTO> getSiglumsStatus(@RequestParam(required = false) String userSelected) {
        SiglumStatusResponseDTO siglumStatus = workloadEvolutionService.getSiglumsStatusByRole(userSelected);
        return ResponseEntity.ok(siglumStatus);
    }

    @GetMapping("/can-update-workload")
    public ResponseEntity<CanUpdateWorkloadDTO> canUpdateWorkload(@RequestParam(required = false) String userSelected) {
        CanUpdateWorkloadDTO canUpdate = workloadEvolutionService.canUpdateWorkload(userSelected);
        return ResponseEntity.ok(canUpdate);
    }

    @GetMapping("/exists-opened-exercise")
    public ResponseEntity<ExistsOpenedExerciseDTO> existsOpenedExercise() {
        ExistsOpenedExerciseDTO existsOpenedExerciseDTO = workloadEvolutionService.existsOpenedExercise();
        return ResponseEntity.ok(existsOpenedExerciseDTO);
    }

    @PostMapping("/open-exercise")
    public ResponseEntity<Map<String, String>> openExercise(@RequestParam("exercise") String newExercise) {
        workloadEvolutionService.openExercise(newExercise);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Exercise '" + newExercise + "' created successfully.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/close-exercise")
    public ResponseEntity<Map<String, String>> closeExercise() {
        workloadEvolutionService.closeExercise();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Last opened exercise successfully closed.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/siglums-first-submission")
    public ResponseEntity<SiglumStatusResponseDTO> createSiglumsFirstSubmission(
            @RequestBody SiglumStatusRequestDTO siglumStatusRequestDTO,
            @RequestParam(required = false) String userSelected) {
        SiglumStatusResponseDTO response = workloadEvolutionService.createSiglumsFirstSubmission(siglumStatusRequestDTO.getApprovedList(), userSelected);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/siglums-status")
    public ResponseEntity<SiglumStatusResponseDTO> setSiglumStatus(
            @RequestBody SiglumStatusRequestDTO siglumStatusRequestDTO,
            @RequestParam(required = false) String userSelected) {
        SiglumStatusResponseDTO siglumStatus = workloadEvolutionService.setSiglumStatus(siglumStatusRequestDTO.getApprovedList(), siglumStatusRequestDTO.getRejectedList(), userSelected);
        return ResponseEntity.ok(siglumStatus);
    }
}