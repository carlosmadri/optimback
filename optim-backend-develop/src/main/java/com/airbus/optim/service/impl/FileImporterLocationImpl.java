package com.airbus.optim.service.impl;

import com.airbus.optim.entity.CostCenter;
import com.airbus.optim.entity.Employee;
import com.airbus.optim.entity.Location;
import com.airbus.optim.entity.Siglum;
import com.airbus.optim.repository.CostCenterRepository;
import com.airbus.optim.repository.EmployeeRepository;
import com.airbus.optim.repository.LocationRepository;
import com.airbus.optim.repository.SiglumRepository;
import com.airbus.optim.service.FileImporter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
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
public class FileImporterLocationImpl implements FileImporter {

    private ExecutorService executorService;

    @Autowired
    LocationRepository locationRepository;

    @Autowired
    EmployeeRepository employeeRepository;

    @Autowired
    CostCenterRepository costCenterRepository;

    public void readFile() {
        log.info("START FileImporterLocationImpl");

        InputStream inputStream = null;
        if (LOCAL.equalsIgnoreCase(ENVIRONMENT)) {
            ClassLoader classLoader = getClass().getClassLoader();
            inputStream = classLoader.getResourceAsStream("entities/Location.xlsx");
        } else {
            //TODO: Add real environment
        }

        if (inputStream != null) {
            processFileInParallel(inputStream);
        } else {
            log.warn("File not found");
        }

        log.info("END FileImporterLocationImpl");
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
        if(row.getCell(0).getStringCellValue().equalsIgnoreCase("country") && row.getCell(1).getStringCellValue().equalsIgnoreCase("site")){
            return;
        }

        Optional<Location> locationOptional = locationRepository.findByCountryAndSite(row.getCell(0).getStringCellValue(), row.getCell(1).getStringCellValue());
        Location location = locationOptional.isPresent() ? locationOptional.get() : new Location();
        try {
            if (row.getCell(0) != null && !row.getCell(0).getStringCellValue().isBlank()) {
                location.setCountry(row.getCell(0).getStringCellValue());
            }
            if (row.getCell(1) != null && !row.getCell(1).getStringCellValue().isBlank()) {
                location.setSite(row.getCell(1).getStringCellValue());
            }
            if (row.getCell(2) != null) {
                Cell cell = row.getCell(2);
                String costCenterCode = null;

                if (cell.getCellType() == CellType.STRING) {
                    costCenterCode = cell.getStringCellValue();
                } else if (cell.getCellType() == CellType.NUMERIC) {
                    costCenterCode = String.valueOf((int) cell.getNumericCellValue());
                }

//                if (costCenterCode != null && !costCenterCode.isEmpty()) {
//                    Optional<CostCenter> costCenterOptional = costCenterRepository.findByCostCenterCode(costCenterCode);
//                    costCenterOptional.ifPresent(location::setCostCenters);
//                }
            }

            if (row.getCell(3) != null) {
                location.setKapisCode(String.valueOf((int) row.getCell(3).getNumericCellValue()));
            }

            locationRepository.save(location);
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
