package com.airbus.optim.repository;

import com.airbus.optim.dto.WorkloadPerProgramDTO;
import com.airbus.optim.dto.WorkloadFteDTO;
import com.airbus.optim.entity.Siglum;
import com.airbus.optim.entity.Workload;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkloadRepository extends JpaRepository<Workload, Long>, JpaSpecificationExecutor<Workload> {

    List<Workload>  findAll(Specification<Workload> spec);

    @Query("""
    SELECT w
    FROM Workload w
    JOIN w.ppsid p
    WHERE w.exercise IN :latestExercises
      AND w.siglum IN :siglumFiltered
      AND w IN :workload
      AND go = true 
      AND p.scenario = 'Nominal'
    """)
    List<Workload> findWorkloadsByExerciseAndSiglums(
            @Param("latestExercises") List<String> latestExercises,
            @Param("siglumFiltered") List<Siglum> siglumFiltered,
            @Param("workload") List<Workload> workload
    );


    @Query("""
    SELECT w
    FROM Workload w
    JOIN w.ppsid p
    WHERE w.exercise IN :latestExercises
      AND w.siglum IN :siglumFiltered
      AND go = true 
      AND p.scenario = 'Nominal'
      AND LOWER(w.direct) IN ('direct', 'indirect')
      AND LOWER(w.own) IN ('own', 'sub')
      AND EXTRACT(YEAR FROM w.startDate) <= :yearFilter
      AND EXTRACT(YEAR FROM w.endDate) >= :yearFilter
    """)
    List<Workload> findWorkloadsByExerciseAndSiglumsAndStartDate(
            @Param("latestExercises") List<String> latestExercises,
            @Param("siglumFiltered") List<Siglum> siglumFiltered,
            @Param("yearFilter") int yearFilter
    );



    @Query("SELECT w FROM Workload w " +
            "JOIN w.ppsid p " +
            "WHERE w.exercise = :exercise " +
            "AND p.scenario = 'Nominal' " +
            "AND EXTRACT(YEAR FROM w.startDate) <= :yearFilter " +
            "AND EXTRACT(YEAR FROM w.endDate) >= :yearFilter " +
            "AND w.siglum IN :siglumList ")
    List<Workload> getForSubmitButton(@Param("siglumList") List<Siglum> siglumList,
                                      @Param("exercise") String exercise,
                                      @Param("yearFilter") int yearFilter);


    @Query("SELECT COALESCE(SUM(w.kHrs), 0) FROM Workload w " +
            "WHERE w.exercise = 'BU' " +
            "AND EXTRACT(YEAR FROM w.startDate) >= :yearFilter")
    Double getKhrsByExerciseEditionByYear(@Param("yearFilter") int yearFilter);

    @Query("SELECT COALESCE(SUM(w.kHrs), 0) FROM Workload w " +
            "WHERE w IN :lastExerciseList " +
            "AND EXTRACT(YEAR FROM w.startDate) >= :yearFilter")
    Double getKhrsByExercise(
            @Param("lastExerciseList") List<Workload> lastExerciseList,
            @Param("yearFilter") int yearFilter);

    @Query("SELECT w FROM Workload w " +
            "JOIN w.ppsid p " +
            "WHERE w.exercise LIKE CONCAT(:exercise, '%') " +
            "AND LOWER(w.direct) IN ('direct', 'indirect') " +
            "AND p.scenario = 'Nominal' " +
            "AND go = true " +
            "AND EXTRACT(YEAR FROM w.startDate) <= :yearFilter " +
            "AND EXTRACT(YEAR FROM w.endDate) >= :yearFilter " +
            "AND (w.siglum IN :siglumList AND w IN :workloadList) ")
    List<Workload> getIndirectRatioForBottomUp(@Param("workloadList") List<Workload> workloadList,
                                               @Param("siglumList") List<Siglum> siglumList,
                                               @Param("exercise") String exercise,
                                               @Param("yearFilter") int yearFilter);

    @Query("SELECT w FROM Workload w " +
            "JOIN w.ppsid p " +
            "WHERE LOWER(w.direct) IN ('direct', 'indirect') " +
            "AND p.scenario = 'Nominal' " +
            "AND go = true " +
            "AND EXTRACT(YEAR FROM w.startDate) <= :yearFilter " +
            "AND EXTRACT(YEAR FROM w.endDate) >= :yearFilter " +
            "AND w IN :workloadList ")
    List<Workload> getIndirectRatioForLastExercise(@Param("workloadList") List<Workload> workloadList,
                                                   @Param("yearFilter") int yearFilter);

    @Query("SELECT w FROM Workload w " +
            "JOIN w.ppsid p " +
            "WHERE w.exercise LIKE CONCAT(:exercise, '%') " +
            "AND LOWER(w.own) IN ('own', 'sub') " +
            "AND p.scenario = 'Nominal' " +
            "AND go = true " +
            "AND EXTRACT(YEAR FROM w.startDate) <= :yearFilter " +
            "AND EXTRACT(YEAR FROM w.endDate) >= :yearFilter " +
            "AND (w.siglum IN :siglumList AND w IN :workloadList) ")
    List<Workload> getOwnRatioForBottomUp(@Param("workloadList") List<Workload> workloadList,
                                          @Param("siglumList") List<Siglum> siglumList,
                                          @Param("exercise") String exercise,
                                          @Param("yearFilter") int yearFilter);

    @Query("SELECT w FROM Workload w " +
            "JOIN w.ppsid p " +
            "WHERE LOWER(w.own) IN ('own', 'sub') " +
            "AND p.scenario = 'Nominal' " +
            "AND go = true " +
            "AND EXTRACT(YEAR FROM w.startDate) <= :yearFilter " +
            "AND EXTRACT(YEAR FROM w.endDate) >= :yearFilter " +
            "AND w IN :workloadList ")
    List<Workload> getOwnRatioForLastExercise(@Param("workloadList") List<Workload> workloadList,
                                              @Param("yearFilter") int yearFilter);

    @Query("SELECT COALESCE(SUM(w.kEur*w.costCenter.rateSub), 0) " +
            "FROM Workload w " +
            "WHERE w IN :workloadFilteredList " +
            "AND w.siglum IN :siglumVisibleList " +
            "AND w.own = 'SUB' " +
            "AND (:currentYear = EXTRACT(YEAR FROM w.startDate) AND :currentYear = EXTRACT(YEAR FROM w.endDate)) ")
    Double getSubcontractingOwnRatio(List<Workload> workloadFilteredList, List<Siglum> siglumVisibleList, int currentYear);

    @Query("SELECT new com.airbus.optim.dto.WorkloadPerProgramDTO(p.programLine, SUM(w.kHrs), COUNT(1)) " +
            "FROM Workload w INNER JOIN PPSID p " +
            "ON p.id = w.ppsid.id " +
            "WHERE w.exercise LIKE :exercise" + "% " +
            "AND w.kHrs <> 0 " +
            "AND go = true " +
            "AND programLine <> 'null' " +
            "AND EXTRACT(YEAR FROM w.startDate) <= :yearFilter " +
            "AND EXTRACT(YEAR FROM w.endDate) >= :yearFilter " +
            "AND (w.siglum IN (:siglumList) AND w IN :workloadList) " +
            "GROUP BY p.programLine")
    List<WorkloadPerProgramDTO> getWorkloadPerProgram(@Param("workloadList") List<Workload> workloadList,
                                                      @Param("siglumList") List<Siglum> siglumList,
                                                      @Param("exercise") String exercise,
                                                      @Param("yearFilter") int yearFilter);

    @Query("""
            SELECT new com.airbus.optim.dto.WorkloadPerProgramDTO(p.programLine, SUM(w.kHrs), COUNT(1))
            FROM Workload w INNER JOIN PPSID p
            ON p.id = w.ppsid.id
            AND w.kHrs <> 0
            AND p.programLine <> 'null'
            AND EXTRACT(YEAR FROM w.startDate) >= :yearFilter
            AND EXTRACT(YEAR FROM w.endDate) <= :yearFilter
            AND w.id IN :workloadList
            GROUP BY p.programLine
    """)
    List<WorkloadPerProgramDTO> getPerProgramLastExercise(@Param("workloadList") List<Long> workloadList,
                                                          @Param("yearFilter") int yearFilter);

    @Query("SELECT COALESCE(SUM(kHrs), 0) " +
            "FROM Workload w " +
            "WHERE LOWER(exercise) = LOWER(:exercise) " +
            "AND w.siglum IN (:siglumList) " +
            "AND go = true " +
            "AND LOWER(own) = LOWER(:own) " +
            "AND LOWER(direct) = LOWER(:direct)")
    double getWorkloadEvolutionLastExerciseApproved(@Param("siglumList") List<Siglum> siglumList,
                                                    @Param("exercise") String exercise,
                                                    @Param("own") String own,
                                                    @Param("direct") String direct);

    @Query("SELECT COALESCE(SUM(kHrs), 0) " +
            "FROM Workload w " +
            "WHERE LOWER(exercise) = LOWER(:exercise) " +
            "AND w.siglum IN (:siglumList) " +
            "AND go = true " +
            "AND LOWER(direct) = LOWER(:direct)")
    List<Workload> getWorkloadEvolutionLastExerciseBU(@Param("siglumList") List<Siglum> siglumList,
                                                    @Param("exercise") String exercise,
                                                    @Param("direct") String direct);

    @Query("SELECT new com.airbus.optim.dto.WorkloadFteDTO(" +
            "    count(EXTRACT(MONTH FROM w.startDate)), " +
            "    COALESCE(EXTRACT(MONTH FROM w.startDate), 0), " +
            "    w.exercise, " +
            "    COALESCE(SUM(w.kHrs), 0.0), " +
            "    COALESCE(w.costCenter.efficiency, 0.0), " +
            "    0.0) " +
            "FROM Workload w " +
            "WHERE LOWER(w.exercise) = LOWER(:exercise) " +
            "AND w.siglum IN (:siglumList) " +
            "AND w IN :workloadList " +
            "AND w.go = true " +
            "GROUP BY EXTRACT(MONTH FROM w.startDate), w.exercise, w.costCenter.efficiency")
    List<WorkloadFteDTO> getWorkloadWorkforceExerciseFTE(@Param("workloadList") List<Workload> workloadList,
                                                         @Param("siglumList") List<Siglum> siglumList,
                                                         @Param("exercise") String exercise);

    @Query("SELECT COALESCE(fTE, 0) " +
            "FROM WorkloadEvolution " +
            "WHERE LOWER(exercise) = LOWER(:exercise) " +
            "AND siglum IN (:siglumList) " +
            "AND LOWER(status) = LOWER(:status) " +
            "ORDER BY submitDate DESC " +
            "LIMIT 1")
    Double getWorkloadWorkforceExerciseEditionFTE(@Param("siglumList") List<Siglum> siglumList,
                                                  @Param("exercise") String exercise,
                                                  @Param("status") String status);

    @Query("SELECT new com.airbus.optim.dto.WorkloadFteDTO(" +

            "count(EXTRACT(MONTH FROM w.startDate)), " +
            "COALESCE(EXTRACT(MONTH FROM w.startDate), 0), " +
            "w.exercise, " +
            "COALESCE(SUM(w.kHrs), 0.0), " +
            "COALESCE(w.costCenter.efficiency, 0.0), " +
            "0.0) " +

            "FROM Workload w " +
            "WHERE :yearFilter = EXTRACT(YEAR FROM w.startDate) " +
            "AND :exercise = w.exercise " +
            "AND w.siglum IN (:siglumList) " +
            "AND w IN :workloadList " +
            "GROUP BY EXTRACT(MONTH FROM w.startDate), w.exercise, w.costCenter.efficiency " +
            "ORDER BY EXTRACT(MONTH FROM w.startDate) ASC ")
    List<WorkloadFteDTO> getWorkloadMontlyDistribution(@Param("workloadList") List<Workload> workloadList,
                                                       @Param("siglumList") List<Siglum> siglumList,
                                                       @Param("yearFilter") int yearFilter,
                                                       @Param("exercise") String exercise);

    @Query("SELECT status " +
            "FROM WorkloadEvolution we " +
            "WHERE we.submitDate IN (select MAX(w.submitDate) from WorkloadEvolution w) " +
            "AND we.siglum IN (:siglumList) " +
            "ORDER BY we.submitDate DESC " +
            "LIMIT 1 ")
    String getWorkloadWIP(@Param("siglumList") List<Siglum> siglumList);

    @Query("SELECT new com.airbus.optim.dto.WorkloadFteDTO(count(EXTRACT(MONTH FROM w.startDate)), 0, w.exercise, COALESCE(SUM(w.kHrs), 0.0), COALESCE(w.costCenter.efficiency, 0.0), 0.0) " +
            "FROM Workload w " +
            "WHERE exercise = :exercise " +
            "AND w.siglum IN (:siglumList) " +
            "GROUP BY w.exercise, w.costCenter.efficiency")
    List<WorkloadFteDTO> workloadFTEbyExercise(@Param("siglumList") List<Siglum> siglumList,
                                               @Param("exercise") String exercise);

    @Query("SELECT new com.airbus.optim.dto.WorkloadFteDTO(count(EXTRACT(MONTH FROM w.startDate)), 0, w.exercise, COALESCE(SUM(w.kHrs), 0.0), COALESCE(w.costCenter.efficiency, 0.0), 0.0) " +
            "FROM Workload w " +
            "WHERE w IN (:workloadList) " +
            "AND EXTRACT(YEAR FROM w.startDate) <= :yearFilter " +
            "AND EXTRACT(YEAR FROM w.endDate) >= :yearFilter " +
            "GROUP BY w.exercise, w.costCenter.efficiency")
    List<WorkloadFteDTO> workloadFTEbyLastExercise(@Param("workloadList") List<Workload> workloadList,
                                                   @Param("yearFilter") int yearFilter);

    void deleteBySiglumInAndGoTrueAndExercise(List<Siglum> siglums, String exercise);

    List<Workload> findByExercise(String exercise);

    List<Workload> findByExerciseAndSiglumInAndGoTrue(String exercise, List<Siglum> siglums);
}