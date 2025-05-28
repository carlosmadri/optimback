package com.airbus.optim.service.EmployeeImpl;

import com.airbus.optim.dto.ReportEndOfYear.ActiveWorkforceReportCapacityDTO;
import com.airbus.optim.dto.ReportEndOfYear.ActiveWorkforceReportDTO;
import com.airbus.optim.dto.TeamOutlookDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class EmployeeActiveWorkforceCapacityReport {

    public List<ActiveWorkforceReportCapacityDTO> getActualsActiveWorkforceCapacity(
            List<ActiveWorkforceReportDTO> activeWorkforceReportList,
            List<ActiveWorkforceReportDTO> tempWorkforceReportList) {

        // Se obtienen los registros AWF y TEMP por separado. LA informacion viene agrupada por siglum
        // sumanto los valores FTE, por lo que no habra ningun siglum repetido
        List<ActiveWorkforceReportDTO> eLawf = activeWorkforceReportList;
        List<ActiveWorkforceReportDTO> eLtemp = tempWorkforceReportList;

        List<ActiveWorkforceReportCapacityDTO> actualsAwfList = new ArrayList<>();
        List<ActiveWorkforceReportCapacityDTO> actualsTempList = new ArrayList<>();
        List<ActiveWorkforceReportCapacityDTO> actualsList = new ArrayList<>();

        ActiveWorkforceReportCapacityDTO activeWorkforceAndSiglumCapacity =
                new ActiveWorkforceReportCapacityDTO("",0.0,0.0, 0.0);

        // Se crea la lista de objetos final con la info de AWF
        for (int i=0; i<eLawf.size(); i++) {
            actualsAwfList.add(new ActiveWorkforceReportCapacityDTO(
                    eLawf.get(i).getReport(),
                    eLawf.get(i).getFTE(),
                    0.0,
                    0.0
            ));
        }

        // Se crea la lista de objetos final con la info de TEMP
        for (int i=0; i<eLtemp.size(); i++) {
            actualsTempList.add(new ActiveWorkforceReportCapacityDTO(
                    eLtemp.get(i).getReport(),
                    0.0,
                    eLtemp.get(i).getFTE(),
                    0.0
            ));
        }

        // Para cada siglum de la lista de registros de TEMP, se añade el valor correspondiente de AWF
        for (int i=0; i<actualsTempList.size(); i++) { //TEMP
            for (int j=0; j<actualsAwfList.size(); j++) { //AWF
                if(actualsTempList.get(0).getReport().equals(actualsAwfList.get(j).getReport())) { //siglum==
                    actualsTempList.get(i).setAwfFTE(actualsAwfList.get(j).getAwfFTE());
                    actualsTempList.get(i).setCapacity(
                            actualsTempList.get(i).getTempFTE()+actualsTempList.get(i).getAwfFTE()
                    );
                    break;
                }
            }
        }

        // Se añaden los siglums de AWF que no estan en la lista de refistros TMP
        boolean exists = false;
        for (int i=0; i<actualsAwfList.size(); i++) {
            exists = false;
            for (int j=0; j<actualsTempList.size(); j++) {
                if(actualsAwfList.get(0).getReport().equals(actualsTempList.get(j).getReport())) {
                    exists = true;
                    break;
                }
            }
            if(!exists) {
                actualsTempList.add(new ActiveWorkforceReportCapacityDTO(
                        actualsAwfList.get(i).getReport(),
                        0.0,
                        actualsAwfList.get(i).getAwfFTE(),
                        actualsAwfList.get(i).getAwfFTE()
                ));
            }
        }

        return actualsTempList;
    }

    public List<ActiveWorkforceReportCapacityDTO> getRealisticActiveWorkforceCapacity(
            List<ActiveWorkforceReportCapacityDTO> actualsList, TeamOutlookDTO teamOutlookDTO) {

        List<ActiveWorkforceReportCapacityDTO> realisticList = new ArrayList<>();
        ActiveWorkforceReportCapacityDTO realistic =
                new ActiveWorkforceReportCapacityDTO("",0.0,0.0,0.0);

        for(ActiveWorkforceReportCapacityDTO actualForRealistic : actualsList) {
            realistic.setReport(actualForRealistic.getReport());
            realistic.setAwfFTE(actualForRealistic.getAwfFTE()
                    + (Objects.requireNonNull(teamOutlookDTO).getLeavers() != null ? teamOutlookDTO.getLeavers() : 0.0f)
                    + (teamOutlookDTO.getRecoveries() != null ? teamOutlookDTO.getRecoveries() : 0.0f)
                    + (teamOutlookDTO.getRedeployment() != null ? teamOutlookDTO.getRedeployment() : 0.0f)
                    + (teamOutlookDTO.getPerimeterChanges() != null ? teamOutlookDTO.getPerimeterChanges() : 0.0f)
                    + (teamOutlookDTO.getFilled() != null ? teamOutlookDTO.getFilled() : 0.0f)
                    + (teamOutlookDTO.getOpened() != null ? teamOutlookDTO.getOpened() : 0.0f)
            );
            realistic.setTempFTE(actualForRealistic.getTempFTE()
                    + (teamOutlookDTO.getLeavers() != null ? teamOutlookDTO.getLeavers() : 0.0f)
                    + (teamOutlookDTO.getRecoveries() != null ? teamOutlookDTO.getRecoveries() : 0.0f)
                    + (teamOutlookDTO.getRedeployment() != null ? teamOutlookDTO.getRedeployment() : 0.0f)
                    + (teamOutlookDTO.getPerimeterChanges() != null ? teamOutlookDTO.getPerimeterChanges() : 0.0f)
                    + (teamOutlookDTO.getFilled() != null ? teamOutlookDTO.getFilled() : 0.0f)
                    + (teamOutlookDTO.getOpened() != null ? teamOutlookDTO.getOpened() : 0.0f)
            );
            realistic.setCapacity(realistic.getAwfFTE() + realistic.getTempFTE());
            realisticList.add(realistic);
        }

        return realisticList;
    }

    public List<ActiveWorkforceReportCapacityDTO> getValidationActiveWorkforceCapacity(
            List<ActiveWorkforceReportCapacityDTO> realisticList, TeamOutlookDTO teamOutlookDTO) {

        List<ActiveWorkforceReportCapacityDTO> validationList = new ArrayList<>();
        ActiveWorkforceReportCapacityDTO validation =
                new ActiveWorkforceReportCapacityDTO("",0.0,0.0,0.0);

        for(ActiveWorkforceReportCapacityDTO realisticForValidation : realisticList) {
            validation.setReport(realisticForValidation.getReport());
            validation.setAwfFTE(realisticForValidation.getAwfFTE()
                    + (teamOutlookDTO.getValidationProcess() != null ? teamOutlookDTO.getValidationProcess() : 0.0f)
            );
            validation.setTempFTE(realisticForValidation.getTempFTE()
                    + (teamOutlookDTO.getValidationProcess() != null ? teamOutlookDTO.getValidationProcess() : 0.0f)
            );
            validation.setCapacity(validation.getAwfFTE() + validation.getTempFTE());
            validationList.add(validation);
        }

        return validationList;
    }

    public List<ActiveWorkforceReportCapacityDTO> getOptimisticActiveWorkforceCapacity(
            List<ActiveWorkforceReportCapacityDTO> validationList, TeamOutlookDTO teamOutlookDTO) {

        List<ActiveWorkforceReportCapacityDTO> optimisticList = new ArrayList<>();
        ActiveWorkforceReportCapacityDTO optimistic =
                new ActiveWorkforceReportCapacityDTO("",0.0,0.0,0.0);

        for(ActiveWorkforceReportCapacityDTO validationForOptimistic : validationList) {
            optimistic.setReport(validationForOptimistic.getReport());
            optimistic.setAwfFTE(validationForOptimistic.getAwfFTE()
                    + (teamOutlookDTO.getOnHold() != null ? teamOutlookDTO.getOnHold() : 0.0f)
            );
            optimistic.setTempFTE(validationForOptimistic.getTempFTE()
                    + (teamOutlookDTO.getOnHold() != null ? teamOutlookDTO.getOnHold() : 0.0f)
            );
            optimistic.setCapacity(optimistic.getAwfFTE() + optimistic.getTempFTE());
            optimisticList.add(optimistic);
        }

        return optimisticList;
    }

}
