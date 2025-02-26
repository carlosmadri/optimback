package com.airbus.optim.sheet;

import com.airbus.optim.entity.CostCenter;
import com.airbus.optim.entity.Location;
import com.airbus.optim.repository.LocationRepository;
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

import java.util.List;

@Component
public class LocationAndCostCenterSheetComponent {

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private UtilsSheetComponent utilsSheetComponent;

    public void createLocationSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Location and Cost Center List");
        sheet.protectSheet("password");

        CellStyle headerStyle = utilsSheetComponent.createLightBlueHeaderStyle(workbook);

        Row header = sheet.createRow(0);
        String[] headers = {
                "Country", "Site", "Kapis Code", "Cost Center",
                "Cost Center Financial Code", "Efficiency",
                "Rate Own", "Rate Sub"
        };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        List<Location> locations = locationRepository.findAll();
        int rowIdx = 1;

        for (Location location : locations) {
            if (location.getCostCenters() != null && !location.getCostCenters().isEmpty()) {
                for (CostCenter costCenter : location.getCostCenters()) {
                    Row row = sheet.createRow(rowIdx++);

                    row.createCell(0).setCellValue(location.getCountry() != null ? location.getCountry() : "");
                    row.createCell(1).setCellValue(location.getSite() != null ? location.getSite() : "");
                    row.createCell(2).setCellValue(location.getKapisCode() != null ? location.getKapisCode() : "");
                    row.createCell(3).setCellValue(costCenter.getCostCenterCode() != null ? costCenter.getCostCenterCode() : "");
                    row.createCell(4).setCellValue(costCenter.getCostCenterFinancialCode() != null ? costCenter.getCostCenterFinancialCode() : "");
                    row.createCell(5).setCellValue(costCenter.getEfficiency() != null ? costCenter.getEfficiency() : 0.0);
                    row.createCell(6).setCellValue(costCenter.getRateOwn() != null ? costCenter.getRateOwn() : 0.0);
                    row.createCell(7).setCellValue(costCenter.getRateSub() != null ? costCenter.getRateSub() : 0.0);
                }
            } else {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(location.getCountry() != null ? location.getCountry() : "");
                row.createCell(1).setCellValue(location.getSite() != null ? location.getSite() : "");
                row.createCell(2).setCellValue(location.getKapisCode() != null ? location.getKapisCode() : "");
                for (int i = 3; i < headers.length; i++) {
                    row.createCell(i).setCellValue("");
                }
            }
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    public void addCostCenterValidation(Sheet sheet) {
        DataValidationHelper validationHelper = sheet.getDataValidationHelper();
        CellRangeAddressList addressList = new CellRangeAddressList(1, 1000, 4, 4);
        DataValidationConstraint constraint = validationHelper.createFormulaListConstraint(
                "'Location and Cost Center List'!$D$2:$D$100"
        );
        DataValidation validation = validationHelper.createValidation(constraint, addressList);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    public void addSiteFormula(Sheet workloadSheet) {
        String locationSheetName = "Location and Cost Center List";

        String referenceRange = "'%s'!$D$2:$E$100";

        for (int rowIdx = 1; rowIdx <= 1000; rowIdx++) {
            Row row = workloadSheet.getRow(rowIdx);
            if (row == null) continue;

            Cell siteCell = row.getCell(3);
            if (siteCell == null) siteCell = row.createCell(3);

            String formula = String.format(
                    "IFERROR(VLOOKUP(E%d," + referenceRange + ",2,FALSE),\"\")",
                    rowIdx + 1, locationSheetName
            );

            siteCell.setCellFormula(formula);
        }
    }
}