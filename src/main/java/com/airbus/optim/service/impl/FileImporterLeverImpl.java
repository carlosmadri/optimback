package com.airbus.optim.service.impl;

import com.airbus.optim.entity.Employee;
import com.airbus.optim.entity.Lever;
import com.airbus.optim.entity.Siglum;
import com.airbus.optim.repository.EmployeeRepository;
import com.airbus.optim.repository.LeverRepository;
import com.airbus.optim.repository.SiglumRepository;
import com.airbus.optim.service.FileImporter;
import com.airbus.optim.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.airbus.optim.utils.Constants.ENVIRONMENT;
import static com.airbus.optim.utils.Constants.LOCAL;

@Service
@Slf4j
public class FileImporterLeverImpl implements FileImporter {

    private ExecutorService executorService;

    @Autowired
    LeverRepository leverRepository;

    @Autowired
    EmployeeRepository employeeRepository;

    @Autowired
    SiglumRepository siglumRepository;

    public void readFile() {
        log.info("START FileImporterLeverImpl");

        InputStream inputStream = null;
        if (LOCAL.equalsIgnoreCase(ENVIRONMENT)) {
            ClassLoader classLoader = getClass().getClassLoader();
            inputStream = classLoader.getResourceAsStream("entities/Lever.xlsx");
        } else {
            //TODO: Add real environment
        }

        if (inputStream != null) {
            processFileInParallel(inputStream);
        } else {
            log.warn("File not found");
        }

        log.info("END FileImporterLeverImpl");
    }

    public void processFileInParallel(InputStream inputStream) {
        executorService = Executors.newFixedThreadPool(10);

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<Callable<Void>> tasks = new ArrayList<>();
            for (Row row : sheet) {
                tasks.add(() -> {
                    processRow(row);
                    return null;
                });
            }
            executorService.invokeAll(tasks);
        } catch (IOException | InterruptedException e) {
            log.error("Error processing Excel file", e);
            Thread.currentThread().interrupt();
        } finally {
            shutdown();
        }
    }

    public void processRow(Row row) {
        Optional<Employee> employeeOptional = employeeRepository.findByEmployeeId((long) row.getCell(0).getNumericCellValue());
        Lever lever = employeeOptional.flatMap(leverRepository::findByEmployee).orElseGet(Lever::new);
        try {
            if(lever.getEmployee() == null && employeeOptional.isPresent()){
                lever.setEmployee(employeeOptional.get());
            }
            if (row.getCell(1) != null && !row.getCell(1).getStringCellValue().isBlank()) {
                lever.setLeverType(row.getCell(1).getStringCellValue());
            }
            if (row.getCell(2) != null && !row.getCell(2).getStringCellValue().isBlank()) {
                lever.setHighlights(row.getCell(2).getStringCellValue());
            }
            if (row.getCell(3) != null) {
                lever.setStartDate(row.getCell(3).getDateCellValue().toInstant());
            }
            if (row.getCell(4) != null) {
                lever.setEndDate(row.getCell(4).getDateCellValue().toInstant());
            }
            if(row.getCell(5) != null && !row.getCell(5).getStringCellValue().isEmpty()){
                Optional<Siglum> siglumOptional = siglumRepository.findBySiglumHR(row.getCell(5).getStringCellValue());
                if(siglumOptional.isPresent()) {
                    lever.setSiglumDestination(siglumOptional.get());
                }
            }
            if (row.getCell(6) != null && row.getCell(6).getCellType() == CellType.NUMERIC) {
                lever.setFTE((float) row.getCell(6).getNumericCellValue());
            }

            leverRepository.save(lever);
        } catch (Exception e) {
            log.error("Error processing row: " + e.getMessage());
        }
    }

    public void shutdown() {
        try {
            executorService.shutdown();
            boolean terminated = executorService.awaitTermination(1, TimeUnit.MINUTES);

            if (!terminated) {
                log.error("Executor did not terminate in the specified time.");
                executorService.shutdownNow();

                if (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
                    log.error("Executor did not terminate after shutdownNow().");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            if (!executorService.isTerminated()) {
                log.error("Cancel non-finished tasks");
            }
        }
    }
}
