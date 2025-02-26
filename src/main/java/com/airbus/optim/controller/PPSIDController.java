package com.airbus.optim.controller;

import com.airbus.optim.entity.PPSID;
import com.airbus.optim.repository.PPSIDRepository;
import com.airbus.optim.service.PPSIDService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("ppsids")
public class PPSIDController {

    @Autowired
    private PPSIDService ppsidService;

    @Autowired
    private PPSIDRepository ppsidRepository;

    @GetMapping
    public ResponseEntity<Page<PPSID>> getAllPPSIDs(Pageable pageable) {
        Page<PPSID> ppsids = ppsidRepository.findAll(pageable);
        return ResponseEntity.ok(ppsids);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PPSID> getPPSIDById(@PathVariable Long id) {
        Optional<PPSID> ppsid = ppsidRepository.findById(id);
        return ppsid.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/ppsid/{ppsid}")
    public ResponseEntity<PPSID> getPPSIDById(@PathVariable String ppsid) {
        Optional<PPSID> ppsidOptional = ppsidRepository.findByPpsid(ppsid);
        return ppsidOptional.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/all")
    public ResponseEntity<List<String>> getAllPPSID() {
        List<String> ppsidNames = ppsidRepository.findAllPPSID();
        return ResponseEntity.ok(ppsidNames);
    }

    @PostMapping
    public ResponseEntity<PPSID> createPPSID(@RequestBody PPSID ppsid) {
        PPSID savedPPSID = ppsidService.savePPSID(ppsid);
        return new ResponseEntity<>(savedPPSID, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PPSID> updatePPSID(@PathVariable Long id, @RequestBody PPSID ppsidDetails) {
        return ppsidService.updatePPSID(id, ppsidDetails);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePPSID(@PathVariable Long id) {
        if (ppsidRepository.existsById(id)) {
            ppsidRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
