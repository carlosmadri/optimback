package com.airbus.optim.controller;

import com.airbus.optim.dto.subcontractingDto.SubcontractingDTO;
import com.airbus.optim.dto.subcontractingDto.SubcontractingDataDTO;
import com.airbus.optim.entity.PurchaseOrders;
import com.airbus.optim.repository.PurchaseOrdersRepository;
import com.airbus.optim.service.PurchaseOrdersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
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

    @GetMapping("/subcontracting-graphics")
    public ResponseEntity<SubcontractingDTO> getSubcontractingData(
            @RequestParam MultiValueMap<String, String> params,
            @RequestParam String userSelected,
            @RequestParam String yearFilter) {
        return new ResponseEntity<>(
                purchaseOrdersService.getSubcontractingData(params, userSelected, yearFilter),
                HttpStatus.OK);
    }

    @GetMapping("/subcontracting-table")
    public ResponseEntity<Page<SubcontractingDataDTO>> getSubcontractingTable(
            @RequestParam MultiValueMap<String, String> params,
            Pageable pageable,
            @RequestParam String userSelected) {
        return new ResponseEntity<>(
                purchaseOrdersService.getSubcontractingTable(params, pageable, userSelected),
                HttpStatus.OK);
    }

    @GetMapping("/report/subcontracting-table")
    public ResponseEntity<byte[]> getReportSubcontractingTable(
            @RequestParam MultiValueMap<String, String> params,
            @RequestParam String userSelected) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "SubcontractingReport.csv");

        return new ResponseEntity<>(
                purchaseOrdersService.getReportSubcontractingTable(params, userSelected),
                headers,
                HttpStatus.OK);
    }   
    
}