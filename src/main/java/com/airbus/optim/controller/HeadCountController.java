package com.airbus.optim.controller;

import com.airbus.optim.entity.HeadCount;
import com.airbus.optim.repository.HeadCountRepository;
import com.airbus.optim.service.HeadCountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("head-counts")
public class HeadCountController {

    @Autowired
    private HeadCountService headCountService;

    @Autowired
    private HeadCountRepository headCountRepository;

    @GetMapping
    public ResponseEntity<Page<HeadCount>> getAllHeadCounts(Pageable pageable) {
        Page<HeadCount> headCounts = headCountRepository.findAll(pageable);
        return ResponseEntity.ok(headCounts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HeadCount> getHeadCountById(@PathVariable Long id) {
        Optional<HeadCount> headCount = headCountRepository.findById(id);
        return headCount.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<HeadCount> createHeadCount(@RequestBody HeadCount headCount) {
        HeadCount savedHeadCount = headCountService.saveOrUpdateHeadCount(headCount);
        return new ResponseEntity<>(savedHeadCount, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HeadCount> updateHeadCount(@PathVariable Long id, @RequestBody HeadCount headCountDetails) {
        return headCountService.updateHeadCount(id, headCountDetails);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHeadCount(@PathVariable Long id) {
        if (headCountRepository.existsById(id)) {
            headCountRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

