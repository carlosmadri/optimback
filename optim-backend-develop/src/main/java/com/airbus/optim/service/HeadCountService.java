package com.airbus.optim.service;

import com.airbus.optim.entity.HeadCount;
import com.airbus.optim.entity.Siglum;
import com.airbus.optim.repository.HeadCountRepository;
import com.airbus.optim.repository.SiglumRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HeadCountService {

    @Autowired
    private HeadCountRepository headCountRepository;

    @Autowired
    private SiglumRepository siglumRepository;

    public HeadCount saveOrUpdateHeadCount(HeadCount headCount) {
        handleSiglum(headCount);
        return headCountRepository.save(headCount);
    }

    public ResponseEntity<HeadCount> updateHeadCount(Long id, HeadCount headCountDetails) {
        return headCountRepository.findById(id).map(existingHeadCount -> {
            updateHeadCountDetails(existingHeadCount, headCountDetails);
            handleSiglum(headCountDetails);
            HeadCount updatedHeadCount = headCountRepository.save(existingHeadCount);
            return ResponseEntity.ok(updatedHeadCount);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private void updateHeadCountDetails(HeadCount existingHeadCount, HeadCount headCountDetails) {
        existingHeadCount.setYear(headCountDetails.getYear());
        existingHeadCount.setFTE(headCountDetails.getFTE());
        existingHeadCount.setRefCount(headCountDetails.getRefCount());
    }

    private void handleSiglum(HeadCount headCount) {
        if (headCount.getSiglum() != null) {
            Siglum siglum = headCount.getSiglum();

            if (siglum.getId() == null) {
                throw new IllegalArgumentException("Cannot assign a new Siglum. Only existing Siglums are allowed.");
            }

            Siglum existingSiglum = siglumRepository.findById(siglum.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Siglum not found with ID: " + siglum.getId()));

            headCount.setSiglum(existingSiglum);
        }
    }
}