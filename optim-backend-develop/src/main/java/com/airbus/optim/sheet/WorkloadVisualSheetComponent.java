package com.airbus.optim.sheet;

import com.airbus.optim.entity.Siglum;
import com.airbus.optim.entity.Workload;
import com.airbus.optim.repository.WorkloadRepository;
import com.airbus.optim.service.WorkloadEvolutionService;
import com.airbus.optim.utils.Constants;
import com.airbus.optim.utils.Utils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Component
public class WorkloadVisualSheetComponent {

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
        Sheet sheet = workbook.createSheet("Visible Workloads");
        sheet.protectSheet("password");

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
    }
}