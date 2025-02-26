package com.airbus.optim.controller;

import com.airbus.optim.entity.Siglum;
import com.airbus.optim.repository.SiglumRepository;
import com.airbus.optim.service.SiglumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("siglums")
public class SiglumController {

    @Autowired
    private SiglumService siglumService;

    @Autowired
    private SiglumRepository siglumRepository;

    @GetMapping
    public ResponseEntity<Page<Siglum>> getAllSiglums(Pageable pageable) {
        Page<Siglum> siglumPage = siglumRepository.findAll(pageable);
        return ResponseEntity.ok(siglumPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Siglum> getSiglumById(@PathVariable Long id) {
        Optional<Siglum> siglum = siglumRepository.findById(id);
        return siglum.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/visible-siglum")
    public ResponseEntity<List<Siglum>> getVisiblesSiglums(@RequestParam String siglumHR) {
        List<Siglum> result = siglumService.getVisiblesSiglums(siglumHR);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Siglum> createSiglum(@RequestBody Siglum siglum) {
        Siglum savedSiglum = siglumService.saveOrUpdateSiglum(siglum);
        return new ResponseEntity<>(savedSiglum, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Siglum> updateSiglum(@PathVariable Long id, @RequestBody Siglum siglumDetails) {
        Optional<Siglum> updatedSiglum = siglumService.updateSiglum(id, siglumDetails);
        return updatedSiglum.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSiglum(@PathVariable Long id) {
        if (siglumRepository.existsById(id)) {
            siglumRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

