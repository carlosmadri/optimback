package com.airbus.optim.repository;

import com.airbus.optim.dto.WorkloadEvolutionDTO;
import com.airbus.optim.dto.WorkloadPerProgramDTO;
import com.airbus.optim.entity.Siglum;
import com.airbus.optim.dto.WorkloadPreviewDTO;
import com.airbus.optim.entity.Siglum;
import com.airbus.optim.entity.Workload;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkloadRepository extends JpaRepository<Workload, Long> {

    Page<Workload> findAll(Pageable pageable);

    @Query("SELECT w FROM Workload w " +
            "WHERE w.direct IN ('Direct', 'Indirect') " +
            "AND w.siglum IN (:siglumList) " +
            "AND w.scenario = 'Nominal' " +
            "AND EXTRACT(YEAR FROM w.startDate) = :yearFilter ")
    List<Workload> getIndirectRatio(@Param("siglumList") List<Siglum> siglumList,
                                    @Param("yearFilter") int yearFilter);

    @Query("SELECT w FROM Workload w " +
            "WHERE w.own IN ('OWN', 'SUB') " +
            "AND w.siglum IN (:siglumList) " +
            "AND w.scenario = 'Nominal' " +
            "AND EXTRACT(YEAR FROM w.startDate) = :yearFilter ")
    List<Workload> getOwnRatio(@Param("siglumList") List<Siglum> siglumList,
                               @Param("yearFilter") int yearFilter);

    @Query("SELECT new com.airbus.optim.dto.WorkloadPerProgramDTO(p.programLine, SUM(w.kHrs), COUNT(1)) " +
            "FROM Workload w INNER JOIN PPSID p " +
            "ON p.id = w.ppsid.id " +
            "WHERE w.kHrs <> 0 " +
            "AND w.siglum IN (:siglumList) " +
            "AND w.exercise = :exercise " +
            "AND :yearFilter = EXTRACT(YEAR FROM startDate) " +
            "GROUP BY p.programLine")
    List<WorkloadPerProgramDTO> getWorkloadPerProgram(@Param("siglumList") List<Siglum> siglumList,
                                                      @Param("yearFilter") int yearFilter,
                                                      @Param("exercise") String exercise);

    @Query("SELECT COALESCE(SUM(kHrs), 0) " +
            "FROM Workload w " +
            "WHERE exercise = :exercise " +
            "AND w.siglum IN (:siglumList) " +
            "AND scenario = 'Nominal' " +
            "AND :yearFilter = EXTRACT(YEAR FROM startDate) " +
            "AND go = true " +
            "AND own = :own " +
            "AND direct = :direct")
    double getWorkloadEvolutionLastExerciseApproved(@Param("siglumList") List<Siglum> siglumList,
                                                    @Param("exercise") String exercise,
                                                    @Param("own") String own,
                                                    @Param("direct") String direct,
                                                    @Param("yearFilter") int yearFilter);
    @Query("SELECT new com.airbus.optim.dto.WorkloadEvolutionDTO(we.exercise,we.status,COALESCE(we.kHrsOwnDirect,0),COALESCE(we.kHrsOwnIndirect,0),COALESCE(we.kHrsSubDirect,0),COALESCE(we.kHrsSubIndirect, 0)) " +
            "FROM WorkloadEvolution we " +
            "WHERE we.submitDate IN (select MAX(w.submitDate) from WorkloadEvolution w group by w.exercise, w.status) " +
            "AND we.siglum IN (:siglumList) " +
            "AND :yearFilter = EXTRACT(YEAR FROM we.submitDate) " +
            "AND we.exercise = 'BU' " +
            "ORDER BY we.submitDate DESC ")
    List<WorkloadEvolutionDTO> getWorkloadEvolutionExerciseEdited(@Param("siglumList") List<Siglum> siglumList,
                                                                  @Param("yearFilter") int yearFilter);

    @Query("SELECT COALESCE(SUM(w.fTE), 0) " +
            "FROM Workload w " +
            "WHERE w.exercise = :exercise " +
            "AND w.siglum IN (:siglumList) " +
            "AND w.scenario = 'Nominal' " +
            "AND w.go = true " +
            "AND :yearFilter = EXTRACT(YEAR FROM w.startDate) ")
    double getWorkloadWorkforceExerciseFTE(@Param("siglumList") List<Siglum> siglumList,
                                           @Param("yearFilter") int yearFilter,
                                           @Param("exercise") String exercise);

    @Query("SELECT COALESCE(fTE, 0) " +
            "FROM WorkloadEvolution " +
            "WHERE exercise = :exercise " +
            "AND siglum IN (:siglumList) " +
            "AND status = :status " +
            "AND :yearFilter = EXTRACT(YEAR FROM submitDate) " +
            "ORDER BY submitDate DESC " +
            "LIMIT 1 ")
    double getWorkloadWorkforceExerciseEditionFTE(@Param("siglumList") List<Siglum> siglumList,
                                                  @Param("yearFilter") int yearFilter,
                                                  @Param("exercise") String exercise,
                                                  @Param("status") String status);

    @Query("SELECT new com.airbus.optim.dto.WorkloadPreviewDTO(:exercise, COALESCE(SUM(w.kHrs), 0), COALESCE(SUM(w.fTE), 0)) " +
            "FROM Workload w " +
            "WHERE w.exercise = :exercise " +
            "AND w.siglum IN (:siglumList) " +
            "AND :yearFilter = EXTRACT(YEAR FROM startDate) ")
    WorkloadPreviewDTO getWorkloadPreviewExercise(@Param("siglumList") List<Siglum> siglumList,
                                                  @Param("yearFilter") int yearFilter,
                                                  @Param("exercise") String exercise);

    void deleteBySiglumIn(List<Siglum> siglums);
}
