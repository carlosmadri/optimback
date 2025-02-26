package com.airbus.optim.controller;

import com.airbus.optim.dto.IndirectRadioDTO;
import com.airbus.optim.dto.OwnRatioDTO;
import com.airbus.optim.dto.WorkloadEvolutionStructureDTO;
import com.airbus.optim.dto.WorkloadPerProgramDTO;
import com.airbus.optim.dto.WorkloadPreviewDTO;
import com.airbus.optim.dto.WorkloadWorkforceDTO;
import com.airbus.optim.entity.Workload;
import com.airbus.optim.repository.WorkloadRepository;
import com.airbus.optim.service.WorkloadService;
import com.airbus.optim.sheet.ExportSheetComponent;
import com.airbus.optim.sheet.ImportWorkloadSheetComponent;
import com.airbus.optim.utils.Utils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.List;

@RestController
@RequestMapping("/workloads")
public class WorkloadController {

    @Autowired
    private WorkloadService workloadService;

    @Autowired
    private ExportSheetComponent exportSheetComponent;

    @Autowired
    private ImportWorkloadSheetComponent importWorkloadSheetComponent;

    @Autowired
    private WorkloadRepository workloadRepository;

    @Autowired
    private Utils utils;

    @GetMapping
    public ResponseEntity<Page<Workload>> getAllWorkloads(Pageable pageable) {
        Page<Workload> workloads = workloadRepository.findAll(pageable);
        return ResponseEntity.ok(workloads);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Workload> getWorkloadById(@PathVariable Long id) {
        Optional<Workload> workload = workloadRepository.findById(id);
        return workload.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @GetMapping("/indirect-ratio")
    public ResponseEntity<IndirectRadioDTO> getIndirectRadio(@RequestParam String userSelected, @RequestParam int yearFilter) {
        if (utils.filterYearComprobation(yearFilter)) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(workloadService.getIndirectRatio(
                utils.getSiglumVisibilityList(userSelected), yearFilter), HttpStatus.OK);
    }

    @GetMapping("/own-sub-ratio")
    public ResponseEntity<OwnRatioDTO> getOwnRatio(@RequestParam String userSelected, @RequestParam int yearFilter) {
        if (utils.filterYearComprobation(yearFilter)) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(workloadService.getOwnRatio(
                utils.getSiglumVisibilityList(userSelected), yearFilter), HttpStatus.OK);
    }

    @GetMapping("/per-program")
    public ResponseEntity<List<WorkloadPerProgramDTO>> getWorkloadPerProgram(@RequestParam String userSelected, @RequestParam int yearFilter) {
        if (utils.filterYearComprobation(yearFilter)) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(workloadService.getWorkloadPerProgram(
                utils.getSiglumVisibilityList(userSelected), yearFilter), HttpStatus.OK);
    }

    @GetMapping("/evolution")
    public ResponseEntity<WorkloadEvolutionStructureDTO> getWorkloadEvolution(@RequestParam String userSelected, @RequestParam int yearFilter) {
        if (utils.filterYearComprobation(yearFilter)) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(workloadService.getWorkloadEvolution(
                utils.getSiglumVisibilityList(userSelected), yearFilter), HttpStatus.OK);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportToExcel() throws IOException {
        byte[] excelContent = exportSheetComponent.export();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.builder("attachment").filename("export.xlsx").build());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok().headers(headers).body(excelContent);
    }

    @Operation(
            summary = "Importar archivo Excel",
            description = "Importa datos desde un archivo Excel",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(mediaType = "multipart/form-data")
            )
    )
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importFromExcel(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("El archivo está vacío.");
        }

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            importWorkloadSheetComponent.importWorkloadSheet(workbook);
            return ResponseEntity.ok("Archivo importado exitosamente.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error en los datos del archivo: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al procesar el archivo: " + e.getMessage());
        }
    }

    @GetMapping("/workforce")
    public ResponseEntity<WorkloadWorkforceDTO> getWorkloadWorkforce(
            @RequestParam MultiValueMap<String, String> params, @RequestParam String userSelected, @RequestParam int yearFilter) {
        if (utils.filterYearComprobation(yearFilter)) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(workloadService.getWorkloadWorkforce(
                params, utils.getSiglumVisibilityList(userSelected), yearFilter), HttpStatus.OK);
    }

    @GetMapping("/preview")
    public ResponseEntity<WorkloadPreviewDTO> getWorkloadPreview(@RequestParam String userSelected, @RequestParam int yearFilter) {
        // El acceso a esta página de Manage Workload estará restringido para los roles de usuario HO Department, FBP (Finance Business Partner) y WL Delegate.
        if (utils.filterYearComprobation(yearFilter)) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(workloadService.getWorkloadPreview(
                utils.getSiglumVisibilityList(userSelected), yearFilter), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Workload> createWorkload(@RequestBody Workload workload) {
        Workload savedWorkload = workloadService.createWorkload(workload);
        return new ResponseEntity<>(savedWorkload, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Workload> updateWorkload(@PathVariable Long id, @RequestBody Workload workloadDetails) {
        Workload updatedWorkload = workloadService.updateWorkload(id, workloadDetails);
        return new ResponseEntity<>(updatedWorkload, HttpStatus.OK);
    }

    @PutMapping("/batch")
    public ResponseEntity<?> updateWorkloads(@RequestBody List<Workload> workloads) {
        try {
            List<Workload> updatedWorkloads = workloadService.updateWorkloads(workloads);
            return new ResponseEntity<>(updatedWorkloads, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("No se ha podido actualizar la lista de workloads: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkload(@PathVariable Long id) {
        if (workloadRepository.existsById(id)) {
            workloadRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

