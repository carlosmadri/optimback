package com.airbus.optim.service;

import com.airbus.optim.entity.PurchaseOrders;
import com.airbus.optim.entity.Siglum;
import com.airbus.optim.entity.Location;
import com.airbus.optim.repository.PurchaseOrdersRepository;
import com.airbus.optim.repository.SiglumRepository;
import com.airbus.optim.repository.LocationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PurchaseOrdersService {

    @Autowired
    private PurchaseOrdersRepository purchaseOrdersRepository;

    @Autowired
    private SiglumRepository siglumRepository;

    @Autowired
    private LocationRepository locationRepository;

    public PurchaseOrders savePurchaseOrder(PurchaseOrders purchaseOrder) {
        handleSiglum(purchaseOrder);
        handleLocations(purchaseOrder);
        return purchaseOrdersRepository.save(purchaseOrder);
    }

    public ResponseEntity<PurchaseOrders> updatePurchaseOrder(Long id, PurchaseOrders purchaseOrderDetails) {
        Optional<PurchaseOrders> optionalPurchaseOrder = purchaseOrdersRepository.findById(id);

        if (optionalPurchaseOrder.isPresent()) {
            PurchaseOrders existingPurchaseOrder = optionalPurchaseOrder.get();
            updatePurchaseOrderFields(existingPurchaseOrder, purchaseOrderDetails);

            handleSiglum(purchaseOrderDetails);
            handleLocations(purchaseOrderDetails);

            PurchaseOrders updatedPurchaseOrder = purchaseOrdersRepository.save(existingPurchaseOrder);
            return ResponseEntity.ok(updatedPurchaseOrder);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    private void updatePurchaseOrderFields(PurchaseOrders existingPurchaseOrder, PurchaseOrders purchaseOrderDetails) {
        existingPurchaseOrder.setDescription(purchaseOrderDetails.getDescription());
        existingPurchaseOrder.setProvider(purchaseOrderDetails.getProvider());
        existingPurchaseOrder.setOrderRequest(purchaseOrderDetails.getOrderRequest());
        existingPurchaseOrder.setPurchaseDocument(purchaseOrderDetails.getPurchaseDocument());
        existingPurchaseOrder.setHmg(purchaseOrderDetails.getHmg());
        existingPurchaseOrder.setPep(purchaseOrderDetails.getPep());
        existingPurchaseOrder.setQuarter(purchaseOrderDetails.getQuarter());
        existingPurchaseOrder.setYear(purchaseOrderDetails.getYear());
        existingPurchaseOrder.setKEur(purchaseOrderDetails.getKEur());
    }

    private void handleSiglum(PurchaseOrders purchaseOrder) {
        if (purchaseOrder.getSiglum() != null) {
            Siglum siglum = purchaseOrder.getSiglum();

            if (siglum.getId() == null) {
                throw new IllegalArgumentException("Cannot assign a new Siglum. Only existing Siglums are allowed.");
            }

            Siglum existingSiglum = siglumRepository.findById(siglum.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Siglum not found with ID: " + siglum.getId()));

            purchaseOrder.setSiglum(existingSiglum);
        }
    }

    private void handleLocations(PurchaseOrders purchaseOrder) {
        if (purchaseOrder.getLocations() != null && !purchaseOrder.getLocations().isEmpty()) {
            Set<Location> savedLocations = purchaseOrder.getLocations().stream().map(location -> {
                return location.getId() != null
                        ? locationRepository.findById(location.getId())
                        .orElseThrow(() -> new EntityNotFoundException("Location not found"))
                        : locationRepository.save(location);
            }).collect(Collectors.toSet());
            purchaseOrder.setLocations(new ArrayList<>(savedLocations));
        }
    }
}