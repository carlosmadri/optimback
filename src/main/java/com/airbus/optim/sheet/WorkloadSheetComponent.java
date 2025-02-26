package com.airbus.optim.sheet;

import com.airbus.optim.entity.Workload;
import com.airbus.optim.repository.WorkloadRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class WorkloadSheetComponent {

    @Autowired
    private WorkloadRepository workloadRepository;

    @Autowired
    private LocationAndCostCenterSheetComponent locationAndCostCenterSheetComponent;

    @Autowired
    private PpsidSheetComponent ppsidSheetComponent;

    @Autowired
    private SiglumSheetComponent siglumSheetComponent;

    @Autowired
    private UtilsSheetComponent utilsSheetComponent;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault());

    public void createWorkloadSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Workloads");
        sheet.protectSheet("password");

        CellStyle headerStyle = utilsSheetComponent.createLightBlueHeaderStyle(workbook);

        Row header = sheet.createRow(0);
        String[] headers = {
                "siglumHR", "description", "own", "site", "cost center", "ppsid",
                "core", "EAC", "WC/B", "Go / No go", "startDate", "endDate", "kHrs"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        CellStyle lockedStyle = workbook.createCellStyle();
        lockedStyle.setLocked(true);

        CellStyle unlockedStyle = workbook.createCellStyle();
        unlockedStyle.setLocked(false);

        List<Workload> workloads = workloadRepository.findAll();
        int rowIdx = 1;

        for (Workload workload : workloads) {
            Row row = sheet.createRow(rowIdx++);

            Cell siglumCell = row.createCell(0);
            siglumCell.setCellValue(workload.getSiglum() != null ? workload.getSiglum().getSiglumHR() : null);
            siglumCell.setCellStyle(unlockedStyle);

            Cell descriptionCell = row.createCell(1);
            descriptionCell.setCellValue(workload.getDescription() != null ? workload.getDescription() : null);
            descriptionCell.setCellStyle(unlockedStyle);

            Cell ownCell = row.createCell(2);
            ownCell.setCellValue(workload.getOwn() != null ? workload.getOwn() : null);
            ownCell.setCellStyle(unlockedStyle);

            Cell siteCell = row.createCell(3);
            if (workload.getCostCenter() != null && workload.getCostCenter().getLocation() != null) {
                siteCell.setCellValue(workload.getCostCenter().getLocation().getSite());
            } else {
                siteCell.setCellValue("");
            }

            Cell costCenterCell = row.createCell(4);
            costCenterCell.setCellValue(
                    workload.getCostCenter() != null
                            ? workload.getCostCenter().getCostCenterCode()
                            : null
            );
            costCenterCell.setCellStyle(unlockedStyle);

            Cell ppsidCell = row.createCell(5);
            ppsidCell.setCellValue(
                    workload.getPpsid() != null && workload.getPpsid().getPpsid() != null
                            ? workload.getPpsid().getPpsid()
                            : null
            );
            ppsidCell.setCellStyle(unlockedStyle);

            Cell coreCell = row.createCell(6);
            coreCell.setCellValue(workload.getCore() != null ? workload.getCore() : null);
            coreCell.setCellStyle(unlockedStyle);

            Cell eacCell = row.createCell(7);
            eacCell.setCellValue(workload.getEac() != null ? (workload.getEac() ? "Yes" : "No") : null);
            eacCell.setCellStyle(unlockedStyle);

            Cell wcCell = row.createCell(8);
            wcCell.setCellValue(workload.getCollar() != null ? workload.getCollar() : null);
            wcCell.setCellStyle(unlockedStyle);

            Cell goCell = row.createCell(9);
            goCell.setCellValue(workload.getGo() != null ? (workload.getGo() ? "Go" : "No go") : null);
            goCell.setCellStyle(lockedStyle);

            Cell startDateCell = row.createCell(10);
            startDateCell.setCellValue(
                    workload.getStartDate() != null
                            ? DATE_FORMATTER.format(workload.getStartDate())
                            : ""
            );
            startDateCell.setCellStyle(unlockedStyle);

            Cell endDateCell = row.createCell(11);
            endDateCell.setCellValue(
                    workload.getEndDate() != null
                            ? DATE_FORMATTER.format(workload.getEndDate())
                            : ""
            );
            endDateCell.setCellStyle(unlockedStyle);

            Cell kHrsCell = row.createCell(12);
            kHrsCell.setCellValue(workload.getKHrs() != null ? workload.getKHrs() : 0.0);
            kHrsCell.setCellStyle(unlockedStyle);
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        addOwnValidation(sheet);
        addCoreValidation(sheet);
        addEacValidation(sheet);
        addWcValidation(sheet);

        if (workbook.getSheet("Siglum List") != null) siglumSheetComponent.addSiglumValidation(sheet, workbook);
        if (workbook.getSheet("Location and Cost Center List") != null) {
            locationAndCostCenterSheetComponent.addCostCenterValidation(sheet);
            locationAndCostCenterSheetComponent.addSiteFormula(sheet);
        }
        if (workbook.getSheet("PPSID List") != null) ppsidSheetComponent.addPpsidValidation(sheet, workbook);
    }

    private void addOwnValidation(Sheet sheet) {
        DataValidationHelper validationHelper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = validationHelper.createExplicitListConstraint(new String[] { "OWN", "SUB" });
        CellRangeAddressList addressList = new CellRangeAddressList(1, 1000, 2, 2);
        DataValidation validation = validationHelper.createValidation(constraint, addressList);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    private void addCoreValidation(Sheet sheet) {
        DataValidationHelper validationHelper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = validationHelper.createExplicitListConstraint(new String[] { "Core", "Non core" });
        CellRangeAddressList addressList = new CellRangeAddressList(1, 1000, 6, 6);
        DataValidation validation = validationHelper.createValidation(constraint, addressList);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    private void addEacValidation(Sheet sheet) {
        DataValidationHelper validationHelper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = validationHelper.createExplicitListConstraint(new String[] { "Yes", "No" });
        CellRangeAddressList addressList = new CellRangeAddressList(1, 1000, 7, 7);
        DataValidation validation = validationHelper.createValidation(constraint, addressList);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    private void addWcValidation(Sheet sheet) {
        DataValidationHelper validationHelper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = validationHelper.createExplicitListConstraint(new String[] { "WC", "BC" });
        CellRangeAddressList addressList = new CellRangeAddressList(1, 1000, 8, 8);
        DataValidation validation = validationHelper.createValidation(constraint, addressList);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }
}