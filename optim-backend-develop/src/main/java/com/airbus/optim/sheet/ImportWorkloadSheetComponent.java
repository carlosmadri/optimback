package com.airbus.optim.sheet;

import com.airbus.optim.entity.CostCenter;
import com.airbus.optim.entity.PPSID;
import com.airbus.optim.entity.Siglum;
import com.airbus.optim.entity.Workload;
import com.airbus.optim.repository.CostCenterRepository;
import com.airbus.optim.repository.PPSIDRepository;
import com.airbus.optim.repository.SiglumRepository;
import com.airbus.optim.repository.WorkloadRepository;
import com.airbus.optim.service.WorkloadEvolutionService;
import com.airbus.optim.utils.Constants;
import com.airbus.optim.utils.Utils;
import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class ImportWorkloadSheetComponent {

    @Autowired
    private WorkloadEvolutionService workloadEvolutionService;

    @Autowired
    private WorkloadRepository workloadRepository;

    @Autowired
    private SiglumRepository siglumRepository;

    @Autowired
    private PPSIDRepository ppsidRepository;

    @Autowired
    private CostCenterRepository costCenterRepository;

    @Autowired
    private Utils utils;


    @Transactional
    public void importWorkloadSheet(Workbook workbook, String userSelected) {
        Sheet sheet = workbook.getSheet("Editable Workloads");
        if (sheet == null) {
            throw new IllegalArgumentException("La hoja 'Workloads Editable' no existe en el archivo.");
        }

        List<Siglum> editableSiglums = utils.getEditableSiglums(userSelected);

        if (!editableSiglums.isEmpty()) {
            workloadRepository.deleteBySiglumInAndGoTrueAndExercise(editableSiglums, Constants.WORKLOAD_STATUS_BOTTOM_UP);
        }

        List<Workload> workloads = new ArrayList<>();

        for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {

            Row row = sheet.getRow(rowIdx);
            if (row == null || isRowEmpty(row)) {
                continue;
            }

            Workload workload = new Workload();

            workload.setSiglum(validateMandatorySiglum(row.getCell(0), rowIdx));
            workload.setDescription(validateMandatoryString(row.getCell(1), "Description", rowIdx));
            workload.setOwn(validateMandatoryString(row.getCell(2), "OWN/SUB", rowIdx));
            workload.setCostCenter(validateMandatoryCostCenter(row.getCell(4), rowIdx));
            workload.setPpsid(getPpsidFromCell(row.getCell(5), rowIdx));
            workload.setCore(getStringFromCell(row.getCell(6)));
            workload.setEac(getBooleanFromYesNoCell(row.getCell(7)));
            workload.setCollar(getStringFromCell(row.getCell(8)));
            workload.setDirect(validateMandatoryDirect(row.getCell(9), rowIdx));
            workload.setStartDate(validateMandatoryDate(row.getCell(10), "Start Date", rowIdx));
            workload.setEndDate(validateMandatoryDate(row.getCell(11), "End Date", rowIdx));
            workload.setKHrs(validateMandatoryDouble(row.getCell(12), "kHrs", rowIdx));
            workload.setExercise(Constants.WORKLOAD_STATUS_BOTTOM_UP);
            workload.setGo(true);

            workloads.add(workload);
        }

        workloadRepository.saveAll(workloads);
    }

    private Siglum validateMandatorySiglum(Cell cell, int rowIdx) {
        String siglumHR = getStringFromCell(cell);
        if (siglumHR == null || siglumHR.trim().isEmpty()) {
            throw new IllegalArgumentException("El Siglum no está definido en la fila " + (rowIdx + 1));
        }
        return siglumRepository.findBySiglumHR(siglumHR)
                .orElseThrow(() -> new IllegalArgumentException("El Siglum '" + siglumHR + "' no existe en la fila " + (rowIdx + 1)));
    }

    private String validateMandatoryString(Cell cell, String fieldName, int rowIdx) {
        String value = getStringFromCell(cell);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("El campo '" + fieldName + "' es obligatorio en la fila " + (rowIdx + 1));
        }
        return value;
    }

    private CostCenter validateMandatoryCostCenter(Cell cell, int rowIdx) {
        String costCenterCode = getStringFromCell(cell);
        if (costCenterCode == null || costCenterCode.trim().isEmpty()) {
            throw new IllegalArgumentException("El Cost Center no está definido en la fila " + (rowIdx + 1));
        }
        return costCenterRepository.findByCostCenterCode(costCenterCode)
                .orElseThrow(() -> new IllegalArgumentException("El Cost Center '" + costCenterCode + "' no existe en la fila " + (rowIdx + 1)));
    }

    private String validateMandatoryDirect(Cell cell, int rowIdx) {
        String direct = getStringFromCell(cell);
        if (direct == null || direct.trim().isEmpty()) {
            throw new IllegalArgumentException("El campo 'Direct' no está definido en la fila " + (rowIdx + 1));
        }
        if (!direct.equalsIgnoreCase("direct") && !direct.equalsIgnoreCase("indirect")) {
            throw new IllegalArgumentException("El valor del campo 'Direct' es inválido en la fila " + (rowIdx + 1) + ". Debe ser 'direct' o 'indirect'.");
        }
        return direct.toLowerCase();
    }

    private Double validateMandatoryDouble(Cell cell, String fieldName, int rowIdx) {
        Double value = getDoubleFromCell(cell);
        if (value == null) {
            throw new IllegalArgumentException("El campo '" + fieldName + "' es obligatorio en la fila " + (rowIdx + 1));
        }
        return value;
    }

    private Instant validateMandatoryDate(Cell cell, String fieldName, int rowIdx) {
        Instant value = getDateFromCell(cell);
        if (value == null) {
            throw new IllegalArgumentException("El campo '" + fieldName + "' es obligatorio en la fila " + (rowIdx + 1));
        }
        return value;
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
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yyyy");
                YearMonth yearMonth = YearMonth.parse(cell.getStringCellValue(), formatter);
                return yearMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            } else if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue().toInstant();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("La celda no contiene una fecha válida o el formato es incorrecto. Valor: " + cell.toString(), e);
        }
        throw new IllegalArgumentException("La celda no contiene una fecha válida.");
    }

    private PPSID getPpsidFromCell(Cell cell, int rowIdx) {
        String ppsidValue = getStringFromCell(cell);

        if (ppsidValue == null || ppsidValue.trim().isEmpty()) {
            return null;
        }

        return ppsidRepository.findByPpsid(ppsidValue)
                .orElseThrow(() -> new IllegalArgumentException("El PPSID '" + ppsidValue + "' no existe en la fila " + (rowIdx + 1)));
    }

    private boolean isRowEmpty(Row row) {
        for (int cellIdx = 0; cellIdx < row.getLastCellNum(); cellIdx++) {
            if (cellIdx == 3) {
                continue;
            }

            Cell cell = row.getCell(cellIdx);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String cellValue = getStringFromCell(cell);
                if (cellValue != null && !cellValue.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
}