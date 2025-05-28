package com.airbus.optim.service;

import com.airbus.optim.dto.JobRequestTypeCountDTO;
import com.airbus.optim.dto.ReportEndOfYear.EmployeeReportDTO;
import com.airbus.optim.entity.CostCenter;
import com.airbus.optim.entity.Employee;
import com.airbus.optim.entity.JobRequest;
import com.airbus.optim.entity.Siglum;
import com.airbus.optim.repository.CostCenterRepository;
import com.airbus.optim.repository.EmployeeRepository;
import com.airbus.optim.repository.JobRequestRepository;
import com.airbus.optim.repository.SiglumRepository;
import com.airbus.optim.utils.Constants;
import com.airbus.optim.utils.Utils;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
public class JobRequestService {

    @Autowired
    private JobRequestRepository jobRequestRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CostCenterRepository costCenterRepository;

    @Autowired
    private SiglumRepository siglumRepository;

    @Autowired
    private EmployeeSpecification employeeSpecification;

    @Autowired
    private JobRequestSpecification jobRequestSpecification;

    @Autowired
    private Utils utils;

    public JobRequest updateJobRequest(Long id, JobRequest jobRequestDetails) {

        JobRequest existingJobRequest = jobRequestRepository.findById(jobRequestDetails.getId())
                .orElseThrow(() -> new EntityNotFoundException("JobRequest not found: " + jobRequestDetails.getId()));

        updateJobRequestFields(existingJobRequest, jobRequestDetails);

        handleEmployees(existingJobRequest, jobRequestDetails);
        handleCostCenter(existingJobRequest, jobRequestDetails);
        handleSiglum(existingJobRequest, jobRequestDetails);

        JobRequest updatedJobRequest = jobRequestRepository.save(existingJobRequest);
        return updatedJobRequest;
    }

    public JobRequest createJobRequest(JobRequest jobRequest) {
        handleEmployees(jobRequest);
        handleCostCenter(jobRequest);
        handleSiglum(jobRequest);

        return jobRequestRepository.save(jobRequest);
    }

    private void updateJobRequestFields(JobRequest existingJobRequest, JobRequest jobRequestDetails) {
        existingJobRequest.setWorkdayNumber(jobRequestDetails.getWorkdayNumber());
        existingJobRequest.setType(jobRequestDetails.getType());
        existingJobRequest.setStatus(jobRequestDetails.getStatus());
        existingJobRequest.setDescription(jobRequestDetails.getDescription());
        existingJobRequest.setCandidate(jobRequestDetails.getCandidate());
        existingJobRequest.setStartDate(jobRequestDetails.getStartDate());
        existingJobRequest.setReleaseDate(jobRequestDetails.getReleaseDate());
        existingJobRequest.setPostingDate(jobRequestDetails.getPostingDate());
        existingJobRequest.setExternal(jobRequestDetails.getExternal());
        existingJobRequest.setEarlyCareer(jobRequestDetails.getEarlyCareer());
        existingJobRequest.setOnTopHct(jobRequestDetails.getOnTopHct());
        existingJobRequest.setIsCritical(jobRequestDetails.getIsCritical());
        existingJobRequest.setActiveWorkforce(jobRequestDetails.getActiveWorkforce());
        existingJobRequest.setApprovedQMC(jobRequestDetails.getApprovedQMC());
        existingJobRequest.setApprovedSHRBPHOT1Q(jobRequestDetails.getApprovedSHRBPHOT1Q());
        existingJobRequest.setApprovedHOCOOHOHRCOO(jobRequestDetails.getApprovedHOCOOHOHRCOO());
        existingJobRequest.setApprovedEmploymentCommitee(jobRequestDetails.getApprovedEmploymentCommitee());
        existingJobRequest.setDirect(jobRequestDetails.getDirect());
        existingJobRequest.setCollar(jobRequestDetails.getCollar());
    }

    private void handleEmployees(JobRequest jobRequest) {
        if (jobRequest.getEmployees() != null && !jobRequest.getEmployees().isEmpty()) {
            Set<Employee> savedEmployees = jobRequest.getEmployees().stream().map(employee -> {
                return employee.getId() != null
                        ? employeeRepository.findById(employee.getId())
                        .orElseThrow(() -> new EntityNotFoundException("Employee not found"))
                        : employeeRepository.save(employee);
            }).collect(Collectors.toSet());
            jobRequest.setEmployees(new ArrayList<>(savedEmployees));
        }
    }

    private void handleEmployees(JobRequest existingJobRequest, JobRequest jobRequestDetails) {
        if (jobRequestDetails.getEmployees() != null && !jobRequestDetails.getEmployees().isEmpty()) {
            Set<Employee> savedEmployees = jobRequestDetails.getEmployees().stream().map(employee -> {
                return employee.getId() != null
                        ? employeeRepository.findById(employee.getId())
                        .orElseThrow(() -> new EntityNotFoundException("Employee not found"))
                        : employeeRepository.save(employee);
            }).collect(Collectors.toSet());
            existingJobRequest.setEmployees(new ArrayList<>(savedEmployees));
        }
    }

    private void handleCostCenter(JobRequest jobRequest) {
        saveCostCenter(jobRequest, jobRequest.getCostCenter());
    }

    private void handleCostCenter(JobRequest existingJobRequest, JobRequest jobRequestDetails) {
        saveCostCenter(existingJobRequest, jobRequestDetails.getCostCenter());
    }

    private void saveCostCenter(JobRequest jobRequest, CostCenter costCenter) {
        if (costCenter != null) {
            CostCenter savedCostCenter = costCenter.getId() != null
                    ? costCenterRepository.findById(costCenter.getId())
                    .orElseThrow(() -> new EntityNotFoundException("CostCenter not found with id: " + costCenter.getId()))
                    : costCenterRepository.save(costCenter);
            jobRequest.setCostCenter(savedCostCenter);
        }
    }

    private void handleSiglum(JobRequest jobRequest) {
        saveSiglum(jobRequest, jobRequest.getSiglum());
    }

    private void handleSiglum(JobRequest existingJobRequest, JobRequest jobRequestDetails) {
        saveSiglum(existingJobRequest, jobRequestDetails.getSiglum());
    }

    private void saveSiglum(JobRequest jobRequest, Siglum siglum) {
        if (siglum != null) {
            Siglum savedSiglum = siglum.getId() != null
                    ? siglumRepository.findById(siglum.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Siglum not found with id: " + siglum.getId()))
                    : siglumRepository.save(siglum);
            jobRequest.setSiglum(savedSiglum);
        }
    }

    public List<JobRequestTypeCountDTO> countJobRequestsByTypeAndYear(MultiValueMap<String, String> params, int yearFilter) {
        Specification<JobRequest> spec = jobRequestSpecification.getSpecifications(params);
        spec = spec.and((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(criteriaBuilder.function("date_part", Integer.class, criteriaBuilder.literal("year"), root.get("startDate")), yearFilter)
        );
        List<JobRequest> jobRequestList = jobRequestRepository.findAll(spec);
        return jobRequestRepository.countJobRequestsByType(jobRequestList);
    }

    public Page<JobRequest> filterJobRequests(MultiValueMap<String, String> params, Pageable pageable) {
        Specification<JobRequest> spec = jobRequestSpecification.getSpecifications(params);
        return jobRequestRepository.findAll(spec, pageable);
    }
    
    public byte[] getReportJobRerquestByStatus(
            MultiValueMap<String, String> params,
            String userSelected,
            int yearFilter) {

    	    Workbook workbook = new XSSFWorkbook();

    	    try {
    	        // Especificaciones y recuperación de registros
    	        Specification<JobRequest> spec = jobRequestSpecification.getSpecifications(params);
    	        List<JobRequest> jobRequestList = jobRequestRepository.findAll(spec);

    	        // Filtrar registros según los estados especificados
    	        List<JobRequest> jobRequests = jobRequestList.stream()
    	                .filter(jr -> {
    	                    String status = jr.getStatus();
    	                    return "Validation Required".equalsIgnoreCase(status) ||
    	                           "QMC Approved".equalsIgnoreCase(status) ||
    	                           "SHRBP/HO T1Q Approved".equalsIgnoreCase(status) ||
    	                           "COO Approved".equalsIgnoreCase(status);
    	                })
    	                .toList();

    	        // Crear hoja de Excel
                Sheet sheet = workbook.createSheet("Job Request Snapshot");

                // Configuración de encabezados base: se establecen todos los bordes
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

                // Creación de estilos específicos para la fila de títulos:
                // Primer columna: conservar borde izquierdo pero quitar el derecho.
                CellStyle headerStyleLeft = workbook.createCellStyle();
                headerStyleLeft.cloneStyleFrom(headerStyleBase);
                headerStyleLeft.setBorderRight(BorderStyle.NONE);

                // Columnas intermedias: quitar ambos bordes laterales.
                CellStyle headerStyleMiddle = workbook.createCellStyle();
                headerStyleMiddle.cloneStyleFrom(headerStyleBase);
                headerStyleMiddle.setBorderLeft(BorderStyle.NONE);
                headerStyleMiddle.setBorderRight(BorderStyle.NONE);

                // Última columna: conservar borde derecho pero quitar el izquierdo.
                CellStyle headerStyleRight = workbook.createCellStyle();
                headerStyleRight.cloneStyleFrom(headerStyleBase);
                headerStyleRight.setBorderLeft(BorderStyle.NONE);

                // Configuración y creación de la fila de encabezados
                Row headerRow = sheet.createRow(0);
                String[] headers = {
                        "Id", "WorkdayNumber", "Type", "Status", "Description", "Candidate",
                        "StartDate", "ReleaseDate", "PostingDate", "External", "EarlyCareer",
                        "OnTopHct", "IsCritical", "ActiveWorkforce", "ApprovedQMC",
                        "ApprovedSHRBPH1Q", "ApprovedHOCOOHOHRCOO", "ApprovedEmploymentCommitee",
                        "SiglumId", "SiglumName", "Direct", "Collar", "CostCenterId", "CostCenterCode"
                };

                for (int i = 0; i < headers.length; i++) {
                    // Agregamos cuatro espacios al final de cada título
                    String headerText = headers[i] + "    ";
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headerText);
                    // Asignar estilos según la posición
                    if (i == 0) {
                        cell.setCellStyle(headerStyleLeft);
                    } else if (i == headers.length - 1) {
                        cell.setCellStyle(headerStyleRight);
                    } else {
                        cell.setCellStyle(headerStyleMiddle);
                    }
                }

                // Formato de fecha
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault());

                // Agregar datos al Excel
                int rowIndex = 1;
                for (JobRequest jr : jobRequests) {
                    Row row = sheet.createRow(rowIndex);

                    // Columna 0
                    Cell cell0 = row.createCell(0);
                    cell0.setCellValue(jr.getId() != null ? jr.getId() : 0);
                    // Columna 1
                    Cell cell1 = row.createCell(1);
                    cell1.setCellValue(jr.getWorkdayNumber() != null ? jr.getWorkdayNumber() : "");
                    // Columna 2
                    Cell cell2 = row.createCell(2);
                    cell2.setCellValue(jr.getType() != null ? jr.getType() : "");
                    // Columna 3
                    Cell cell3 = row.createCell(3);
                    cell3.setCellValue(jr.getStatus() != null ? jr.getStatus() : "");
                    // Columna 4
                    Cell cell4 = row.createCell(4);
                    cell4.setCellValue(jr.getDescription() != null ? jr.getDescription() : "");
                    // Columna 5
                    Cell cell5 = row.createCell(5);
                    cell5.setCellValue(jr.getCandidate() != null ? jr.getCandidate() : "");
                    // Columna 6
                    Cell cell6 = row.createCell(6);
                    cell6.setCellValue(jr.getStartDate() != null ? formatter.format(jr.getStartDate()) : "");
                    // Columna 7
                    Cell cell7 = row.createCell(7);
                    cell7.setCellValue(jr.getReleaseDate() != null ? formatter.format(jr.getReleaseDate()) : "");
                    // Columna 8
                    Cell cell8 = row.createCell(8);
                    cell8.setCellValue(jr.getPostingDate() != null ? formatter.format(jr.getPostingDate()) : "");
                    // Columna 9 (booleano)
                    Cell cell9 = row.createCell(9);
                    cell9.setCellValue(jr.getExternal() != null && jr.getExternal() ? "Yes" : "No");
                    // Columna 10 (booleano)
                    Cell cell10 = row.createCell(10);
                    cell10.setCellValue(jr.getEarlyCareer() != null && jr.getEarlyCareer() ? "Yes" : "No");
                    // Columna 11 (booleano)
                    Cell cell11 = row.createCell(11);
                    cell11.setCellValue(jr.getOnTopHct() != null && jr.getOnTopHct() ? "Yes" : "No");
                    // Columna 12 (booleano)
                    Cell cell12 = row.createCell(12);
                    cell12.setCellValue(jr.getIsCritical() != null && jr.getIsCritical() ? "Yes" : "No");
                    // Columna 13
                    Cell cell13 = row.createCell(13);
                    cell13.setCellValue(jr.getActiveWorkforce() != null ? jr.getActiveWorkforce() : "");
                    // Columna 14 (booleano)
                    Cell cell14 = row.createCell(14);
                    cell14.setCellValue(jr.getApprovedQMC() != null && jr.getApprovedQMC() ? "Yes" : "No");
                    // Columna 15 (booleano)
                    Cell cell15 = row.createCell(15);
                    cell15.setCellValue(jr.getApprovedSHRBPHOT1Q() != null && jr.getApprovedSHRBPHOT1Q() ? "Yes" : "No");
                    // Columna 16 (booleano)
                    Cell cell16 = row.createCell(16);
                    cell16.setCellValue(jr.getApprovedHOCOOHOHRCOO() != null && jr.getApprovedHOCOOHOHRCOO() ? "Yes" : "No");
                    // Columna 17 (booleano)
                    Cell cell17 = row.createCell(17);
                    cell17.setCellValue(jr.getApprovedEmploymentCommitee() != null && jr.getApprovedEmploymentCommitee() ? "Yes" : "No");
                    // Columna 18
                    Cell cell18 = row.createCell(18);
                    cell18.setCellValue(jr.getSiglum() != null ? jr.getSiglum().getId() : 0);
                    // Columna 19
                    Cell cell19 = row.createCell(19);
                    cell19.setCellValue(jr.getSiglum() != null ? jr.getSiglum().getSiglum5() : "");
                    // Columna 20
                    Cell cell20 = row.createCell(20);
                    cell20.setCellValue(jr.getDirect() != null ? jr.getDirect() : "");
                    // Columna 21
                    Cell cell21 = row.createCell(21);
                    cell21.setCellValue(jr.getCollar() != null ? jr.getCollar() : "");
                    // Columna 22
                    Cell cell22 = row.createCell(22);
                    cell22.setCellValue(jr.getCostCenter() != null ? jr.getCostCenter().getId() : 0);
                    // Columna 23
                    Cell cell23 = row.createCell(23);
                    cell23.setCellValue(jr.getCostCenter() != null ? jr.getCostCenter().getCostCenterCode() : "");

                    rowIndex++;
                }

                // Post-procesado para asignar bordes externos a las celdas de datos:
                // - A la celda de la primera columna se le asigna borde izquierdo.
                // - A la celda de la última columna se le asigna borde derecho.
                // - A todas las celdas de la última fila de datos se les asigna borde inferior.
                int lastDataRowIndex = rowIndex - 1; // Recordando que los datos inician en la fila 1.
                int numCols = headers.length;
                for (int r = 1; r <= lastDataRowIndex; r++) {
                    Row dataRow = sheet.getRow(r);
                    if (dataRow == null)
                        continue;
                    for (int c = 0; c < numCols; c++) {
                        Cell cell = dataRow.getCell(c);
                        if (cell == null)
                            continue;
                        // Crear un nuevo estilo basado en el existente para no sobrescribir otros estilos.
                        CellStyle currentStyle = cell.getCellStyle();
                        CellStyle newStyle = workbook.createCellStyle();
                        if (currentStyle != null) {
                            newStyle.cloneStyleFrom(currentStyle);
                        }
                        if (c == 0) { // Primera columna: borde izquierdo
                            newStyle.setBorderLeft(BorderStyle.THIN);
                        }
                        if (c == numCols - 1) { // Última columna: borde derecho
                            newStyle.setBorderRight(BorderStyle.THIN);
                        }
                        if (r == lastDataRowIndex) { // Última fila de datos: borde inferior en todas las celdas
                            newStyle.setBorderBottom(BorderStyle.THIN);
                        }
                        cell.setCellStyle(newStyle);
                    }
                }

                // Ajustar tamaño de columnas
                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                }

                // Escribir al archivo y devolver
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

    public byte[] getReportJobRequestClosed(
            MultiValueMap<String, String> params,
            String userSelected,
            int yearFilter) {

        Workbook workbook = new XSSFWorkbook();

        try {
            // Especificaciones y recuperación de registros
            Specification<JobRequest> spec = jobRequestSpecification.getSpecifications(params);
            List<JobRequest> jobRequestList = jobRequestRepository.findAll(spec);

            List<JobRequest> jobRequests = jobRequestList.stream()
                    .filter(jr -> !"closed".equalsIgnoreCase(jr.getStatus()))
                    .toList();

            // Crear hoja de Excel
            Sheet sheet = workbook.createSheet("Job Request Snapshot");

            // Configuración de encabezados base: se establecen todos los bordes
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

            // Creación de estilos específicos para la fila de títulos:
            // Primer columna: conservar borde izquierdo pero quitar el derecho.
            CellStyle headerStyleLeft = workbook.createCellStyle();
            headerStyleLeft.cloneStyleFrom(headerStyleBase);
            headerStyleLeft.setBorderRight(BorderStyle.NONE);

            // Columnas intermedias: quitar ambos bordes laterales.
            CellStyle headerStyleMiddle = workbook.createCellStyle();
            headerStyleMiddle.cloneStyleFrom(headerStyleBase);
            headerStyleMiddle.setBorderLeft(BorderStyle.NONE);
            headerStyleMiddle.setBorderRight(BorderStyle.NONE);

            // Última columna: conservar borde derecho pero quitar el izquierdo.
            CellStyle headerStyleRight = workbook.createCellStyle();
            headerStyleRight.cloneStyleFrom(headerStyleBase);
            headerStyleRight.setBorderLeft(BorderStyle.NONE);

            // Configuración y creación de la fila de encabezados
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Id", "WorkdayNumber", "Type", "Status", "Description", "Candidate",
                    "StartDate", "ReleaseDate", "PostingDate", "External", "EarlyCareer",
                    "OnTopHct", "IsCritical", "ActiveWorkforce", "ApprovedQMC",
                    "ApprovedSHRBPH1Q", "ApprovedHOCOOHOHRCOO", "ApprovedEmploymentCommitee",
                    "SiglumId", "SiglumName", "Direct", "Collar", "CostCenterId", "CostCenterCode"
            };

            for (int i = 0; i < headers.length; i++) {
                // Agregamos cuatro espacios al final de cada título
                String headerText = headers[i] + "    ";
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headerText);
                // Asignar estilos según la posición
                if (i == 0) {
                    cell.setCellStyle(headerStyleLeft);
                } else if (i == headers.length - 1) {
                    cell.setCellStyle(headerStyleRight);
                } else {
                    cell.setCellStyle(headerStyleMiddle);
                }
            }

            // Formato de fecha
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault());

            // Agregar datos al Excel
            int rowIndex = 1;
            for (JobRequest jr : jobRequests) {
                Row row = sheet.createRow(rowIndex);

                // Columna 0
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(jr.getId() != null ? jr.getId() : 0);
                // Columna 1
                Cell cell1 = row.createCell(1);
                cell1.setCellValue(jr.getWorkdayNumber() != null ? jr.getWorkdayNumber() : "");
                // Columna 2
                Cell cell2 = row.createCell(2);
                cell2.setCellValue(jr.getType() != null ? jr.getType() : "");
                // Columna 3
                Cell cell3 = row.createCell(3);
                cell3.setCellValue(jr.getStatus() != null ? jr.getStatus() : "");
                // Columna 4
                Cell cell4 = row.createCell(4);
                cell4.setCellValue(jr.getDescription() != null ? jr.getDescription() : "");
                // Columna 5
                Cell cell5 = row.createCell(5);
                cell5.setCellValue(jr.getCandidate() != null ? jr.getCandidate() : "");
                // Columna 6
                Cell cell6 = row.createCell(6);
                cell6.setCellValue(jr.getStartDate() != null ? formatter.format(jr.getStartDate()) : "");
                // Columna 7
                Cell cell7 = row.createCell(7);
                cell7.setCellValue(jr.getReleaseDate() != null ? formatter.format(jr.getReleaseDate()) : "");
                // Columna 8
                Cell cell8 = row.createCell(8);
                cell8.setCellValue(jr.getPostingDate() != null ? formatter.format(jr.getPostingDate()) : "");
                // Columna 9 (booleano)
                Cell cell9 = row.createCell(9);
                cell9.setCellValue(jr.getExternal() != null && jr.getExternal() ? "Yes" : "No");
                // Columna 10 (booleano)
                Cell cell10 = row.createCell(10);
                cell10.setCellValue(jr.getEarlyCareer() != null && jr.getEarlyCareer() ? "Yes" : "No");
                // Columna 11 (booleano)
                Cell cell11 = row.createCell(11);
                cell11.setCellValue(jr.getOnTopHct() != null && jr.getOnTopHct() ? "Yes" : "No");
                // Columna 12 (booleano)
                Cell cell12 = row.createCell(12);
                cell12.setCellValue(jr.getIsCritical() != null && jr.getIsCritical() ? "Yes" : "No");
                // Columna 13
                Cell cell13 = row.createCell(13);
                cell13.setCellValue(jr.getActiveWorkforce() != null ? jr.getActiveWorkforce() : "");
                // Columna 14 (booleano)
                Cell cell14 = row.createCell(14);
                cell14.setCellValue(jr.getApprovedQMC() != null && jr.getApprovedQMC() ? "Yes" : "No");
                // Columna 15 (booleano)
                Cell cell15 = row.createCell(15);
                cell15.setCellValue(jr.getApprovedSHRBPHOT1Q() != null && jr.getApprovedSHRBPHOT1Q() ? "Yes" : "No");
                // Columna 16 (booleano)
                Cell cell16 = row.createCell(16);
                cell16.setCellValue(jr.getApprovedHOCOOHOHRCOO() != null && jr.getApprovedHOCOOHOHRCOO() ? "Yes" : "No");
                // Columna 17 (booleano)
                Cell cell17 = row.createCell(17);
                cell17.setCellValue(jr.getApprovedEmploymentCommitee() != null && jr.getApprovedEmploymentCommitee() ? "Yes" : "No");
                // Columna 18
                Cell cell18 = row.createCell(18);
                cell18.setCellValue(jr.getSiglum() != null ? jr.getSiglum().getId() : 0);
                // Columna 19
                Cell cell19 = row.createCell(19);
                cell19.setCellValue(jr.getSiglum() != null ? jr.getSiglum().getSiglum5() : "");
                // Columna 20
                Cell cell20 = row.createCell(20);
                cell20.setCellValue(jr.getDirect() != null ? jr.getDirect() : "");
                // Columna 21
                Cell cell21 = row.createCell(21);
                cell21.setCellValue(jr.getCollar() != null ? jr.getCollar() : "");
                // Columna 22
                Cell cell22 = row.createCell(22);
                cell22.setCellValue(jr.getCostCenter() != null ? jr.getCostCenter().getId() : 0);
                // Columna 23
                Cell cell23 = row.createCell(23);
                cell23.setCellValue(jr.getCostCenter() != null ? jr.getCostCenter().getCostCenterCode() : "");

                rowIndex++;
            }

            // Post-procesado para asignar bordes externos a las celdas de datos:
            // - A la celda de la primera columna se le asigna borde izquierdo.
            // - A la celda de la última columna se le asigna borde derecho.
            // - A todas las celdas de la última fila de datos se les asigna borde inferior.
            int lastDataRowIndex = rowIndex - 1; // Recordando que los datos inician en la fila 1.
            int numCols = headers.length;
            for (int r = 1; r <= lastDataRowIndex; r++) {
                Row dataRow = sheet.getRow(r);
                if (dataRow == null)
                    continue;
                for (int c = 0; c < numCols; c++) {
                    Cell cell = dataRow.getCell(c);
                    if (cell == null)
                        continue;
                    // Crear un nuevo estilo basado en el existente para no sobrescribir otros estilos.
                    CellStyle currentStyle = cell.getCellStyle();
                    CellStyle newStyle = workbook.createCellStyle();
                    if (currentStyle != null) {
                        newStyle.cloneStyleFrom(currentStyle);
                    }
                    if (c == 0) { // Primera columna: borde izquierdo
                        newStyle.setBorderLeft(BorderStyle.THIN);
                    }
                    if (c == numCols - 1) { // Última columna: borde derecho
                        newStyle.setBorderRight(BorderStyle.THIN);
                    }
                    if (r == lastDataRowIndex) { // Última fila de datos: borde inferior en todas las celdas
                        newStyle.setBorderBottom(BorderStyle.THIN);
                    }
                    cell.setCellStyle(newStyle);
                }
            }

            // Ajustar tamaño de columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Escribir al archivo y devolver
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