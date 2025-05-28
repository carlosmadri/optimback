package com.airbus.optim.service;

import com.airbus.optim.dto.subcontractingDto.SubcontractingDataDTO;
import com.airbus.optim.dto.subcontractingDto.SubcontractingQuarterDTO;
import com.airbus.optim.entity.Employee;
import com.airbus.optim.entity.PurchaseOrders;
import com.airbus.optim.entity.Siglum;
import com.airbus.optim.entity.Location;
import com.airbus.optim.entity.Workload;
import com.airbus.optim.repository.PurchaseOrdersRepository;
import com.airbus.optim.repository.SiglumRepository;
import com.airbus.optim.repository.LocationRepository;
import com.airbus.optim.repository.WorkloadRepository;
import com.airbus.optim.utils.Utils;
import jakarta.persistence.EntityNotFoundException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.airbus.optim.dto.subcontractingDto.SubcontractingDTO;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.MultiValueMap;


import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


@Service
public class PurchaseOrdersService {

    @Autowired
    private PurchaseOrdersRepository purchaseOrdersRepository;

    @Autowired
    private PurchaseOrdersSpecitication purchaseOrdersSpecification;

    @Autowired
    private WorkloadSpecification workloadSpecification;

    @Autowired
    private WorkloadRepository workloadRepository;

    @Autowired
    private SiglumRepository siglumRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private Utils utils;

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

    public SubcontractingDTO getSubcontractingData(
            MultiValueMap<String, String> params, String userSelected, String yearFilter){
        int year = Integer.parseInt(yearFilter);
        LocalDate currentDate = LocalDate.now(ZoneId.systemDefault());

        Specification<PurchaseOrders> spec = purchaseOrdersSpecification.getSpecifications(params);
        List<PurchaseOrders> purchaseOrderFilteredList = purchaseOrdersRepository.findAll(spec);

        List<Siglum> siglumVisibleList = utils.getSiglumVisibilityList(userSelected);

        List<PurchaseOrders> purchaseOrderSubcontractingList =
                purchaseOrdersRepository.findAllPurchaseOrdersFiltered(purchaseOrderFilteredList, siglumVisibleList);

        return new SubcontractingDTO(
                getSubcontractingAproved(purchaseOrderSubcontractingList, year),
                getSubcontractingNotAproved(purchaseOrderSubcontractingList, year),
                getSubcontractingBaseline(params, siglumVisibleList, year),
                getSubcontractingPerQuarter(purchaseOrderSubcontractingList, year)
        );
    }
    public Double getSubcontractingAproved(List<PurchaseOrders> purchaseOrderList, int currentYear) {
        return purchaseOrderList.stream()
                .filter(purchaseOrder -> "true".equals(purchaseOrder.getApproved()) &&
                        purchaseOrder.getYear().equals(Integer.toString(currentYear)))
                .mapToDouble(PurchaseOrders::getKEur)
                .sum();
    }
    public Double getSubcontractingNotAproved(List<PurchaseOrders> purchaseOrderList, int currentYear) {
        return purchaseOrderList.stream()
                .filter(purchaseOrder -> "false".equals(purchaseOrder.getApproved()) &&
                        purchaseOrder.getYear().equals(Integer.toString(currentYear)))
                .mapToDouble(PurchaseOrders::getKEur)
                .sum();
    }
    public Double getSubcontractingBaseline(
            MultiValueMap<String, String> params, List<Siglum> siglumVisibleList, int currentYear) {

        Specification<Workload> spec = workloadSpecification.getSpecifications(params);
        List<Workload> workloadFilteredList = workloadRepository.findAll(spec);

        return workloadRepository.getSubcontractingOwnRatio(workloadFilteredList, siglumVisibleList, currentYear);
    }
    public SubcontractingQuarterDTO getSubcontractingPerQuarter(
            List<PurchaseOrders> purchaseOrderList, int currentYear) {
        return new SubcontractingQuarterDTO(
                purchaseOrdersRepository.groupPerQuarter(purchaseOrderList,"true", currentYear),
                purchaseOrdersRepository.groupPerQuarter(purchaseOrderList,"false", currentYear)
        );
    }
  
    public Page<SubcontractingDataDTO> getSubcontractingTable(
            MultiValueMap<String, String> params, Pageable pageable, String userSelected) {

        Specification<PurchaseOrders> spec = purchaseOrdersSpecification.getSpecifications(params);
        List<PurchaseOrders> purchaseOrderFilteredList = purchaseOrdersRepository.findAll(spec);

        return purchaseOrdersRepository.findAllSelectiveTableElements(
                pageable, purchaseOrderFilteredList, utils.getSiglumVisibilityList(userSelected));
    }  

    public byte[] getReportSubcontractingTable(
            MultiValueMap<String, String> params, String userSelected) {

        Workbook workbook = new XSSFWorkbook();

        try {
            PageRequest pageRequest = PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "id"));
            List<SubcontractingDataDTO> subcontractingDataList = getSubcontractingTable(params, pageRequest, userSelected).getContent();

            Sheet sheet = workbook.createSheet("Subcontracting Table");

            CellStyle headerStyleBase = workbook.createCellStyle();
            headerStyleBase.setFillForegroundColor(IndexedColors.SKY_BLUE.getIndex());
            headerStyleBase.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyleBase.setBorderBottom(BorderStyle.THIN);
            headerStyleBase.setBorderTop(BorderStyle.THIN);
            headerStyleBase.setBorderLeft(BorderStyle.THIN);
            headerStyleBase.setBorderRight(BorderStyle.THIN);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyleBase.setFont(headerFont);

            CellStyle headerStyleLeft = workbook.createCellStyle();
            headerStyleLeft.cloneStyleFrom(headerStyleBase);
            headerStyleLeft.setBorderRight(BorderStyle.NONE);

            CellStyle headerStyleMiddle = workbook.createCellStyle();
            headerStyleMiddle.cloneStyleFrom(headerStyleBase);
            headerStyleMiddle.setBorderLeft(BorderStyle.NONE);
            headerStyleMiddle.setBorderRight(BorderStyle.NONE);

            CellStyle headerStyleRight = workbook.createCellStyle();
            headerStyleRight.cloneStyleFrom(headerStyleBase);
            headerStyleRight.setBorderLeft(BorderStyle.NONE);

            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Siglum", "Site", "Description", "Provider", "Approved", "Quarter", "Year",
                    "kEur", "OrderRequest", "OrderId", "hmg", "pep"
            };

            for (int i = 0; i < headers.length; i++) {
                String headerText = headers[i] + "    ";
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headerText);
                if (i == 0) {
                    cell.setCellStyle(headerStyleLeft);
                } else if (i == headers.length - 1) {
                    cell.setCellStyle(headerStyleRight);
                } else {
                    cell.setCellStyle(headerStyleMiddle);
                }
            }

            int rowIndex = 1;
            for (SubcontractingDataDTO s : subcontractingDataList) {
                Row row = sheet.createRow(rowIndex);
                row.createCell(0).setCellValue(s.getSiglum());
                row.createCell(1).setCellValue(s.getSite());
                row.createCell(2).setCellValue(s.getDescription());
                row.createCell(3).setCellValue(s.getProvider());
                row.createCell(4).setCellValue(s.getApproved().equals("false") ? "No" : "Yes");
                row.createCell(5).setCellValue(s.getQuarter());
                row.createCell(6).setCellValue(s.getYear());
                row.createCell(7).setCellValue(s.getKEur());
                row.createCell(8).setCellValue(s.getOrderRequest());
                row.createCell(9).setCellValue(s.getOrderId());
                row.createCell(10).setCellValue(s.getHmg());
                row.createCell(11).setCellValue(s.getPep());
                rowIndex++;
            }

            int lastDataRowIndex = rowIndex - 1;
            int numCols = headers.length;
            for (int r = 1; r <= lastDataRowIndex; r++) {
                Row dataRow = sheet.getRow(r);
                if (dataRow == null) continue;
                for (int c = 0; c < numCols; c++) {
                    Cell cell = dataRow.getCell(c);
                    if (cell == null) continue;
                    CellStyle currentStyle = cell.getCellStyle();
                    CellStyle newStyle = workbook.createCellStyle();
                    if (currentStyle != null) {
                        newStyle.cloneStyleFrom(currentStyle);
                    }
                    if (c == 0) {
                        newStyle.setBorderLeft(BorderStyle.THIN);
                    }
                    if (c == numCols - 1) {
                        newStyle.setBorderRight(BorderStyle.THIN);
                    }
                    if (r == lastDataRowIndex) {
                        newStyle.setBorderBottom(BorderStyle.THIN);
                    }
                    cell.setCellStyle(newStyle);
                }
            }

            for (int cont = 0; cont < headers.length; cont++) {
                sheet.autoSizeColumn(cont);
            }

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                workbook.write(bos);
                return bos.toByteArray();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
 }