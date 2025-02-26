package com.airbus.optim.sheet;

import com.airbus.optim.entity.PPSID;
import com.airbus.optim.repository.PPSIDRepository;
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
public class PpsidSheetComponent {

    @Autowired
    private PPSIDRepository ppsidRepository;

    @Autowired
    private UtilsSheetComponent utilsSheetComponent;


    public void createPpsidSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("PPSID List");
        sheet.protectSheet("password");

        CellStyle headerStyle = utilsSheetComponent.createLightBlueHeaderStyle(workbook);

        Row header = sheet.createRow(0);

        String[] headers = {
                "PPSID", "PPSID Name", "Business Line", "Program Line",
                "Production Center", "Business Activity", "Backlog Order Intake",
                "MU Code", "MU Text"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        List<PPSID> ppsids = ppsidRepository.findAll();
        int rowIdx = 1;

        for (PPSID ppsid : ppsids) {
            Row row = sheet.createRow(rowIdx++);

            row.createCell(0).setCellValue(ppsid.getPpsid() != null ? ppsid.getPpsid() : null);
            row.createCell(1).setCellValue(ppsid.getPpsidName() != null ? ppsid.getPpsidName() : null);
            row.createCell(2).setCellValue(ppsid.getBusinessLine() != null ? ppsid.getBusinessLine() : null);
            row.createCell(3).setCellValue(ppsid.getProgramLine() != null ? ppsid.getProgramLine() : null);
            row.createCell(4).setCellValue(ppsid.getProductionCenter() != null ? ppsid.getProductionCenter() : null);
            row.createCell(5).setCellValue(ppsid.getBusinessActivity() != null ? ppsid.getBusinessActivity() : null);
            row.createCell(6).setCellValue(ppsid.getBacklogOrderIntake() != null ? ppsid.getBacklogOrderIntake() : null);
            row.createCell(7).setCellValue(ppsid.getMuCode() != null ? ppsid.getMuCode() : null);
            row.createCell(8).setCellValue(ppsid.getMuText() != null ? ppsid.getMuText() : null);
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    public void addPpsidValidation(Sheet sheet, Workbook workbook) {
        Sheet ppsidSheet = workbook.getSheet("PPSID List");
        if (ppsidSheet == null) {
            throw new RuntimeException("La hoja 'PPSID List' no existe. Verifica que se haya creado antes de agregar validaciones.");
        }

        int lastRow = ppsidSheet.getLastRowNum();
        if (lastRow < 1) return;

        String range = "'PPSID List'!$A$2:$A$" + (lastRow + 1);

        DataValidationHelper validationHelper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = validationHelper.createFormulaListConstraint(range);
        CellRangeAddressList addressList = new CellRangeAddressList(1, 1000, 5, 5);

        DataValidation validation = validationHelper.createValidation(constraint, addressList);
        validation.setSuppressDropDownArrow(true);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }
}
