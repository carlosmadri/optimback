package com.airbus.optim.sheet;

import com.airbus.optim.entity.Siglum;
import com.airbus.optim.entity.Workload;
import com.airbus.optim.repository.WorkloadRepository;
import com.airbus.optim.service.WorkloadEvolutionService;
import com.airbus.optim.utils.Constants;
import com.airbus.optim.utils.Utils;
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

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class WorkloadEditableSheetComponent {

    @Autowired
    private WorkloadRepository workloadRepository;

    @Autowired
    private WorkloadEvolutionService workloadEvolutionService;

    @Autowired
    private LocationAndCostCenterSheetComponent locationAndCostCenterSheetComponent;

    @Autowired
    private PpsidSheetComponent ppsidSheetComponent;

    @Autowired
    private SiglumSheetComponent siglumSheetComponent;

    @Autowired
    private UtilsSheetComponent utilsSheetComponent;

    @Autowired
    private Utils utils;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy").withZone(ZoneId.systemDefault());

    public void createWorkloadSheet(Workbook workbook, String userSelected) {
        Sheet sheet = workbook.createSheet("Editable Workloads");

        CellStyle headerStyle = utilsSheetComponent.createLightBlueHeaderStyle(workbook);

        Row header = sheet.createRow(0);
        String[] headers = {
                "siglumHR", "description", "own", "site", "cost center", "ppsid",
                "core", "EAC", "WC/BC", "Direct", "startDate", "endDate", "kHrs"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        List<Siglum> editableSiglums = utils.getEditableSiglums(userSelected);
        List<Siglum> result = new ArrayList<>();
      
        for (var siglum : editableSiglums) {
            result = utils.getVisibleSiglums(siglum.getSiglumHR(), userSelected);
            if (result != null && !result.isEmpty()) {
                addSiglumValidationUsingRange(workbook, sheet, 0, result);
            }
        } 
      
        List<Siglum> visibleSiglums = utils.getVisibleSiglums(null, userSelected);
        List<Workload> workloads = workloadRepository.findByExerciseAndSiglumInAndGoTrue(Constants.WORKLOAD_STATUS_BOTTOM_UP, visibleSiglums);
        workloads.sort(Comparator.comparing(
                workload -> workload.getSiglum() != null && workload.getSiglum().getSiglumHR() != null
                        ? workload.getSiglum().getSiglumHR()
                        : "")
        );

        int rowIdx = 1;

        for (Workload workload : workloads) {
            Row row = sheet.createRow(rowIdx++);

            Cell siglumCell = row.createCell(0);
            siglumCell.setCellValue(workload.getSiglum() != null ? workload.getSiglum().getSiglumHR() : null);

            Cell descriptionCell = row.createCell(1);
            descriptionCell.setCellValue(workload.getDescription() != null ? workload.getDescription() : null);

            Cell ownCell = row.createCell(2);
            ownCell.setCellValue(workload.getOwn() != null ? workload.getOwn() : null);

            Cell siteCell = row.createCell(3);
            siteCell.setCellValue(workload.getCostCenter() != null && workload.getCostCenter().getLocation() != null
                    ? workload.getCostCenter().getLocation().getSite()
                    : "");

            Cell costCenterCell = row.createCell(4);
            costCenterCell.setCellValue(
                    workload.getCostCenter() != null
                            ? workload.getCostCenter().getCostCenterCode()
                            : null
            );

            Cell ppsidCell = row.createCell(5);
            ppsidCell.setCellValue(
                    workload.getPpsid() != null && workload.getPpsid().getPpsid() != null
                            ? workload.getPpsid().getPpsid()
                            : null
            );

            Cell coreCell = row.createCell(6);
            coreCell.setCellValue(workload.getCore() != null ? workload.getCore() : null);

            Cell eacCell = row.createCell(7);
            eacCell.setCellValue(workload.getEac() != null ? (workload.getEac() ? "Yes" : "No") : null);

            Cell wcCell = row.createCell(8);
            wcCell.setCellValue(workload.getCollar() != null ? workload.getCollar() : null);

            Cell directCell = row.createCell(9);
            directCell.setCellValue(workload.getDirect() != null ? workload.getDirect() : "");

            Cell startDateCell = row.createCell(10);
            startDateCell.setCellValue(
                    workload.getStartDate() != null
                            ? DATE_FORMATTER.format(workload.getStartDate())
                            : ""
            );

            Cell endDateCell = row.createCell(11);
            endDateCell.setCellValue(
                    workload.getEndDate() != null
                            ? DATE_FORMATTER.format(workload.getEndDate())
                            : ""
            );

            Cell kHrsCell = row.createCell(12);
            kHrsCell.setCellValue(workload.getKHrs() != null ? workload.getKHrs() : 0.0);
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        List<String> validMonths = generateMonthYearList();
        writeValidMonthsToHiddenSheet(workbook, validMonths);

        addOwnValidation(sheet);
        addCoreValidation(sheet);
        addEacValidation(sheet);
        addWcValidation(sheet);
        addDirectValidation(sheet);
        addMonthYearValidationUsingRange(sheet, 10, 11, workbook);

        if (workbook.getSheet("Siglum List") != null) siglumSheetComponent.addSiglumValidation(sheet, workbook);

        if (workbook.getSheet("Location and Cost Center List") != null) {
            locationAndCostCenterSheetComponent.addCostCenterValidation(sheet);
            int formulaRowLimit = rowIdx - 1;
            int additionalRows = 20;
            locationAndCostCenterSheetComponent.addSiteFormula(sheet, formulaRowLimit, additionalRows);
        }
        if (workbook.getSheet("PPSID List") != null) ppsidSheetComponent.addPpsidValidation(sheet, workbook);
    }

    private void addDirectValidation(Sheet sheet) {
        DataValidationHelper validationHelper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = validationHelper.createExplicitListConstraint(new String[] { Constants.WORKLOAD_STATUS_DIRECT, Constants.WORKLOAD_STATUS_INDIRECT });
        CellRangeAddressList addressList = new CellRangeAddressList(1, 1000, 9, 9);
        DataValidation validation = validationHelper.createValidation(constraint, addressList);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
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

    private void addMonthYearValidationUsingRange(Sheet sheet, int startColumn, int endColumn, Workbook workbook) {
        DataValidationHelper validationHelper = sheet.getDataValidationHelper();

        String formula = "'HiddenData'!$A$1:$A$100";
        DataValidationConstraint constraint = validationHelper.createFormulaListConstraint(formula);

        CellRangeAddressList addressList = new CellRangeAddressList(1, 1000, startColumn, endColumn);
        DataValidation validation = validationHelper.createValidation(constraint, addressList);

        validation.setShowErrorBox(true);
        validation.createErrorBox("Invalid Date", "Please select a valid month and year in MM/yyyy format.");
        sheet.addValidationData(validation);
    }

    private List<String> generateMonthYearList() {
        List<String> monthYearList = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();
        int startYear = currentDate.getYear();
        int endYear = startYear + 5;

        for (int year = startYear; year <= endYear; year++) {
            int startMonth = (year == startYear) ? currentDate.getMonthValue() : 1;
            for (int month = startMonth; month <= 12; month++) {
                monthYearList.add(String.format("%02d/%d", month, year));
            }
        }
        return monthYearList;
    }

    private void addSiglumValidationUsingRange(Workbook workbook, Sheet sheet, int column, List<Siglum> siglumList) {
        DataValidationHelper validationHelper = sheet.getDataValidationHelper();

        Sheet hiddenSheet = workbook.createSheet("HiddenSiglumData");
        workbook.setSheetHidden(workbook.getSheetIndex(hiddenSheet), true);

        for (int i = 0; i < siglumList.size(); i++) {
            Row row = hiddenSheet.createRow(i);
            Cell cell = row.createCell(0);
            cell.setCellValue(siglumList.get(i).getSiglumHR());
        }

        String formula = "'HiddenSiglumData'!$A$1:$A$" + siglumList.size();
        DataValidationConstraint constraint = validationHelper.createFormulaListConstraint(formula);

        CellRangeAddressList addressList = new CellRangeAddressList(1, 1000, column, column);
        DataValidation validation = validationHelper.createValidation(constraint, addressList);

        validation.setShowErrorBox(true);
        validation.createErrorBox("Invalid Siglum", "Please select a valid Siglum from the list.");
        sheet.addValidationData(validation);
    }
    
    private void writeValidMonthsToHiddenSheet(Workbook workbook, List<String> validMonths) {
        Sheet hiddenSheet = workbook.getSheet("HiddenData");
        if (hiddenSheet == null) {
            hiddenSheet = workbook.createSheet("HiddenData");
            workbook.setSheetHidden(workbook.getSheetIndex("HiddenData"), true);
        }

        for (int i = 0; i < validMonths.size(); i++) {
            Row row = hiddenSheet.getRow(i);
            if (row == null) {
                row = hiddenSheet.createRow(i);
            }
            Cell cell = row.createCell(0);
            cell.setCellValue(validMonths.get(i));
        }
    }
}