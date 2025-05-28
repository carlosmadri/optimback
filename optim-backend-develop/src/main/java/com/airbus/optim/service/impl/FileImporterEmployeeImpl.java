package com.airbus.optim.service.impl;

import com.airbus.optim.entity.Employee;
import com.airbus.optim.repository.EmployeeRepository;
import com.airbus.optim.service.FileImporter;
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
public class FileImporterEmployeeImpl implements FileImporter {

    private ExecutorService executorService;

    @Autowired
    EmployeeRepository employeeRepository;

    public void readFile() {
        log.info("START FileImporterEmployeeImpl");

        InputStream inputStream = null;
        if (LOCAL.equalsIgnoreCase(ENVIRONMENT)) {
            ClassLoader classLoader = getClass().getClassLoader();
            inputStream = classLoader.getResourceAsStream("entities/Employee.xlsx");
        } else {
            //TODO: Add real environment
        }

        if (inputStream != null) {
            processFileInParallel(inputStream);
        } else {
            log.warn("File not found");
        }

        log.info("END FileImporterEmployeeImpl");
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
        Optional<Employee> employeeOptional = employeeRepository.findByEmployeeId((long) row.getCell(2).getNumericCellValue());
        Employee employee = employeeOptional.isPresent() ? employeeOptional.get() : new Employee();

        try {
            if (row.getCell(2) != null && row.getCell(2).getCellType() == CellType.NUMERIC) {
                employee.setEmployeeId((int) row.getCell(2).getNumericCellValue());
            }
            if (row.getCell(3) != null && !row.getCell(3).getStringCellValue().trim().isEmpty()) {
                employee.setDirect(row.getCell(3).getStringCellValue());
            }
            if (row.getCell(4) != null && !row.getCell(4).getStringCellValue().trim().isEmpty()) {
                employee.setJob(row.getCell(4).getStringCellValue());
            }
            if (row.getCell(5) != null && !row.getCell(5).getStringCellValue().trim().isEmpty()) {
                employee.setCollar(row.getCell(5).getStringCellValue());
            }
            if (row.getCell(6) != null && !row.getCell(6).getStringCellValue().trim().isEmpty()) {
                employee.setLastName(row.getCell(6).getStringCellValue());
            }
            if (row.getCell(7) != null && !row.getCell(7).getStringCellValue().trim().isEmpty()) {
                employee.setFirstName(row.getCell(7).getStringCellValue());
            }
            if (row.getCell(8) != null && !row.getCell(8).getStringCellValue().trim().isEmpty()) {
                employee.setActiveWorkforce(row.getCell(8).getStringCellValue());
            }
            if (row.getCell(9) != null && !row.getCell(9).getStringCellValue().trim().isEmpty()) {
                employee.setAvailabilityReason(row.getCell(9).getStringCellValue());
            }
            if (row.getCell(10) != null && !row.getCell(10).getStringCellValue().trim().isEmpty()) {
                employee.setContractType(row.getCell(10).getStringCellValue());
            }
            if (row.getCell(11) != null && row.getCell(11).getCellType() == CellType.NUMERIC) {
                employee.setFTE((float) row.getCell(11).getNumericCellValue());
            }

            employeeRepository.save(employee);
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
