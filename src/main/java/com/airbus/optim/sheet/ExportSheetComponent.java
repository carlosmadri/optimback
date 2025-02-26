package com.airbus.optim.sheet;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class ExportSheetComponent {

    @Autowired
    private LocationAndCostCenterSheetComponent locationAndCostCenterSheetComponent;

    @Autowired
    private PpsidSheetComponent ppsidSheetComponent;

    @Autowired
    private SiglumSheetComponent siglumSheetComponent;

    @Autowired
    private UtilsSheetComponent utilsSheetComponent;

    @Autowired
    private WorkloadSheetComponent workloadSheetComponent;

    public byte[] export() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (Workbook workbook = new XSSFWorkbook()) {
            locationAndCostCenterSheetComponent.createLocationSheet(workbook);
            ppsidSheetComponent.createPpsidSheet(workbook);
            siglumSheetComponent.createSiglumSheet(workbook);
            workloadSheetComponent.createWorkloadSheet(workbook);

            workbook.write(outputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outputStream.toByteArray();
    }

}
