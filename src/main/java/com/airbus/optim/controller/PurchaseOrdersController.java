package com.airbus.optim.controller;

import com.airbus.optim.entity.PurchaseOrders;
import com.airbus.optim.repository.PurchaseOrdersRepository;
import com.airbus.optim.service.PurchaseOrdersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("purchase-orders")
public class PurchaseOrdersController {

    @Autowired
    private PurchaseOrdersService purchaseOrdersService;

    @Autowired
    private PurchaseOrdersRepository purchaseOrdersRepository;

    @GetMapping
    public ResponseEntity<Page<PurchaseOrders>> getAllPurchaseOrders(Pageable pageable) {
        Page<PurchaseOrders> purchaseOrdersPage = purchaseOrdersRepository.findAll(pageable);
        return ResponseEntity.ok(purchaseOrdersPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrders> getPurchaseOrderById(@PathVariable Long id) {
        Optional<PurchaseOrders> purchaseOrder = purchaseOrdersRepository.findById(id);
        return purchaseOrder.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PurchaseOrders> createPurchaseOrder(@RequestBody PurchaseOrders purchaseOrder) {
        PurchaseOrders savedPurchaseOrder = purchaseOrdersService.savePurchaseOrder(purchaseOrder);
        return new ResponseEntity<>(savedPurchaseOrder, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PurchaseOrders> updatePurchaseOrder(
            @PathVariable Long id,
            @RequestBody PurchaseOrders purchaseOrderDetails) {
        return purchaseOrdersService.updatePurchaseOrder(id, purchaseOrderDetails);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePurchaseOrder(@PathVariable Long id) {
        if (purchaseOrdersRepository.existsById(id)) {
            purchaseOrdersRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}