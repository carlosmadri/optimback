package com.airbus.optim.sheet;

import com.airbus.optim.entity.CostCenter;
import com.airbus.optim.entity.PPSID;
import com.airbus.optim.entity.Siglum;
import com.airbus.optim.entity.Workload;
import com.airbus.optim.repository.CostCenterRepository;
import com.airbus.optim.repository.PPSIDRepository;
import com.airbus.optim.repository.SiglumRepository;
import com.airbus.optim.repository.WorkloadRepository;
import com.airbus.optim.service.SiglumService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ImportWorkloadSheetComponent {

    @Autowired
    private SiglumService siglumService;

    @Autowired
    private WorkloadRepository workloadRepository;

    @Autowired
    private SiglumRepository siglumRepository;

    @Autowired
    private PPSIDRepository ppsidRepository;

    @Autowired
    private CostCenterRepository costCenterRepository;

    @Transactional
    public void importWorkloadSheet(Workbook workbook) {
        Sheet sheet = workbook.getSheet("Workloads");
        if (sheet == null) {
            throw new IllegalArgumentException("La hoja 'Workloads' no existe en el archivo.");
        }

        List<Siglum> siglumsToDelete = siglumService.getVisiblesSiglums("T1Q");

        if (!siglumsToDelete.isEmpty()) {
            workloadRepository.deleteBySiglumIn(siglumsToDelete);
        }

        List<Workload> workloads = new ArrayList<>();

        for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {

            Row row = sheet.getRow(rowIdx);
            if (row == null) {
                continue;
            }

            Workload workload = new Workload();

            workload.setSiglum(getSiglumFromCell(row.getCell(0), rowIdx));
            workload.setDescription(getStringFromCell(row.getCell(1)));
            workload.setOwn(getStringFromCell(row.getCell(2)));
            workload.setCostCenter(getCostCenterFromCell(row.getCell(5), rowIdx));
            workload.setPpsid(getPpsidFromCell(row.getCell(5), rowIdx));
            workload.setCore(getStringFromCell(row.getCell(6)));
            workload.setEac(getBooleanFromYesNoCell(row.getCell(7)));
            workload.setCollar(getStringFromCell(row.getCell(8)));
            workload.setGo(getBooleanFromCell(row.getCell(9)));
            workload.setStartDate(getDateFromCell(row.getCell(10)));
            workload.setEndDate(getDateFromCell(row.getCell(11)));
            workload.setKHrs(getDoubleFromCell(row.getCell(12)));

            workloads.add(workload);
        }

        workloadRepository.saveAll(workloads);
    }

    private String getStringFromCell(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        return cell.getCellType() == CellType.STRING ? cell.getStringCellValue().trim() : cell.toString();
    }

    private Double getDoubleFromCell(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        try {
            return cell.getCellType() == CellType.NUMERIC ? cell.getNumericCellValue() : Double.parseDouble(cell.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El valor en la celda no es numérico.");
        }
    }

    private Boolean getBooleanFromCell(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        return cell.getCellType() == CellType.BOOLEAN ? cell.getBooleanCellValue() : "Go".equalsIgnoreCase(cell.toString());
    }

    private Boolean getBooleanFromYesNoCell(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        String value = getStringFromCell(cell);
        if ("Yes".equalsIgnoreCase(value)) {
            return true;
        } else if ("No".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalArgumentException("El valor en la celda no es válido para 'Yes/No'. Valor: " + value);
    }

    private Instant getDateFromCell(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        try {
            if (cell.getCellType() == CellType.STRING) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                LocalDate date = LocalDate.parse(cell.getStringCellValue(), formatter);
                return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
            } else if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue().toInstant();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("La celda no contiene una fecha válida o el formato es incorrecto. Valor: " + cell.toString());
        }
        throw new IllegalArgumentException("La celda no contiene una fecha válida.");
    }

    private Siglum getSiglumFromCell(Cell cell, int rowIdx) {
        String siglumHR = getStringFromCell(cell);

        if (siglumHR == null || siglumHR.trim().isEmpty()) {
            throw new IllegalArgumentException("El Siglum no está definido en la fila " + (rowIdx + 1));
        }

        return siglumRepository.findBySiglumHR(siglumHR)
                .orElseThrow(() -> new IllegalArgumentException("El Siglum '" + siglumHR + "' no existe en la fila " + (rowIdx + 1)));
    }

    private PPSID getPpsidFromCell(Cell cell, int rowIdx) {
        String ppsidValue = getStringFromCell(cell);

        if (ppsidValue == null || ppsidValue.trim().isEmpty()) {
            throw new IllegalArgumentException("El PPSID no está definido en la fila " + (rowIdx + 1));
        }

        return ppsidRepository.findByPpsid(ppsidValue)
                .orElseThrow(() -> new IllegalArgumentException("El PPSID '" + ppsidValue + "' no existe en la fila " + (rowIdx + 1)));
    }

    private CostCenter getCostCenterFromCell(Cell cell, int rowIdx) {
        String costCenterCode = getStringFromCell(cell);

        if (costCenterCode == null || costCenterCode.trim().isEmpty()) {
            throw new IllegalArgumentException("El Cost Center no está definido en la fila " + (rowIdx + 1));
        }

        return costCenterRepository.findByCostCenterCode(costCenterCode)
                .orElseThrow(() -> new IllegalArgumentException("El Cost Center '" + costCenterCode + "' no existe en la fila " + (rowIdx + 1)));
    }

}