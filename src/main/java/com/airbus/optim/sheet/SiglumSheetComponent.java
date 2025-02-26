package com.airbus.optim.sheet;

import com.airbus.optim.entity.Siglum;
import com.airbus.optim.repository.SiglumRepository;
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
public class SiglumSheetComponent {

    @Autowired
    private SiglumRepository siglumRepository;

    @Autowired
    private UtilsSheetComponent utilsSheetComponent;


    public void createSiglumSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Siglum List");
        sheet.protectSheet("password");

        CellStyle headerStyle = utilsSheetComponent.createLightBlueHeaderStyle(workbook);

        Row header = sheet.createRow(0);
        String[] headers = { "SiglumHR", "Siglum6", "Siglum5", "Siglum4", "Siglum3" };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        List<Siglum> siglums = siglumRepository.findAll();
        int rowIdx = 1;

        for (Siglum siglum : siglums) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(siglum.getSiglumHR() != null ? siglum.getSiglumHR() : "");
            row.createCell(1).setCellValue(siglum.getSiglum6() != null ? siglum.getSiglum6() : "");
            row.createCell(2).setCellValue(siglum.getSiglum5() != null ? siglum.getSiglum5() : "");
            row.createCell(3).setCellValue(siglum.getSiglum4() != null ? siglum.getSiglum4() : "");
            row.createCell(4).setCellValue(siglum.getSiglum3() != null ? siglum.getSiglum3() : "");
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    public void addSiglumValidation(Sheet sheet, Workbook workbook) {
        Sheet siglumSheet = workbook.getSheet("Siglum List");
        if (siglumSheet == null) {
            throw new RuntimeException("La hoja 'Siglum List' no existe. Verifica que se haya creado antes de agregar validaciones.");
        }

        int lastRow = siglumSheet.getLastRowNum();
        if (lastRow < 1) return;

        String range = "'Siglum List'!$A$2:$A$" + (lastRow + 1);

        DataValidationHelper validationHelper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = validationHelper.createFormulaListConstraint(range);
        CellRangeAddressList addressList = new CellRangeAddressList(1, 1000, 0, 0);

        DataValidation validation = validationHelper.createValidation(constraint, addressList);
        validation.setSuppressDropDownArrow(true);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }
}
