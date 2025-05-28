package com.airbus.optim.service.workloadImpl;

import com.airbus.optim.dto.WorkloadFteDTO;
import com.airbus.optim.dto.WorkloadMonthlyDistributionDTO;
import com.airbus.optim.entity.Siglum;
import com.airbus.optim.entity.Workload;
import com.airbus.optim.repository.WorkloadRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WorkloadMontlyDistributionImpl {

    private static Logger logger = LoggerFactory.getLogger(WorkloadMontlyDistributionImpl.class);

    @Autowired
    WorkloadRepository workloadRepository;

    public List<WorkloadFteDTO> calculaFTE(List<WorkloadFteDTO> workloadFteDTOList) {
        for (WorkloadFteDTO wfte : workloadFteDTOList) {
            wfte.setFte(wfte.getKHrs() / (wfte.getEfficiency() * ((double) wfte.getMonthsCount() / 12)));
        }
        return workloadFteDTOList;
    }
    public List<WorkloadFteDTO> agrupaMesesPorCostCenter(List<WorkloadFteDTO> workloadFteDTOList) {
        double fTE = 0.0;
        List<WorkloadFteDTO> workloadFteDTOList2 = new ArrayList<>();
        for (int i=0; i<workloadFteDTOList.size(); i++) {
            for (int j=i; j<workloadFteDTOList.size(); j++) {
                if (workloadFteDTOList.get(i).getMonth() == workloadFteDTOList.get(j).getMonth()) {
                    fTE += workloadFteDTOList.get(j).getFte();
                } else {
                    workloadFteDTOList2.add(new WorkloadFteDTO(
                            workloadFteDTOList.get(i).getMonthsCount(),
                            workloadFteDTOList.get(i).getMonth(),
                            workloadFteDTOList.get(i).getExercise(),
                            workloadFteDTOList.get(i).getKHrs(),
                            0.0,
                            fTE
                    ));
                    fTE = 0.0;
                    i = j-1;
                    break;
                }
                if(i == workloadFteDTOList.size()-1) {
                    workloadFteDTOList2.add(new WorkloadFteDTO(
                            workloadFteDTOList.get(i).getMonthsCount(),
                            workloadFteDTOList.get(i).getMonth(),
                            workloadFteDTOList.get(i).getExercise(),
                            workloadFteDTOList.get(i).getKHrs(),
                            0.0,
                            fTE
                    ));
                }
            }
        }
        return workloadFteDTOList2;
    }
    public List<WorkloadMonthlyDistributionDTO> getMontlyDistributionByExercise(
            List<Workload> workloadList,
            List<Siglum> siglumList,
            int yearFilter,
            String exercise) {

        List<WorkloadFteDTO> workloadFteDTOList =
                workloadRepository.getWorkloadMontlyDistribution(workloadList, siglumList, yearFilter, exercise);

        workloadFteDTOList = calculaFTE(workloadFteDTOList);
        workloadFteDTOList = agrupaMesesPorCostCenter(workloadFteDTOList);

        showResults(workloadFteDTOList);

        List<WorkloadMonthlyDistributionDTO> exerciseList = new ArrayList<>();
        for (WorkloadFteDTO www : workloadFteDTOList) {
            exerciseList.add(new WorkloadMonthlyDistributionDTO(www.getMonth(),www.getFte()));
        }

        // TODO add monthly distribution end of year (key:13, value:fte-end-of-year)
        exerciseList.add(new WorkloadMonthlyDistributionDTO(13, 0.0));

        return exerciseList;
    }
    public void showResults(List<WorkloadFteDTO> workloadFteDTOList) {
        for (WorkloadFteDTO ww : workloadFteDTOList) {
            logger.debug("MonthsCount: "+ww.getMonthsCount());
            logger.debug("Month: "+ww.getMonth());
            logger.debug("Exercise: "+ww.getExercise());
            logger.debug("KHrs: "+ww.getKHrs());
            logger.debug("Fte: "+ww.getFte());
            logger.debug("Efficiency: "+ww.getEfficiency());
            logger.debug("--------------------------------");
        }
    }

}

