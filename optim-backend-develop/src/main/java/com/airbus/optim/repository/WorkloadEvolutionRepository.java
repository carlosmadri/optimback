package com.airbus.optim.repository;

import com.airbus.optim.entity.Siglum;
import com.airbus.optim.entity.WorkloadEvolution;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkloadEvolutionRepository extends JpaRepository<WorkloadEvolution, Long> {
    List<WorkloadEvolution>  findAll(Specification<WorkloadEvolution> spec);
    Optional<WorkloadEvolution> findTopBySiglumAndExerciseOrderBySubmitDateDesc(Siglum siglum, String exercise);
    Optional<WorkloadEvolution> findTopByStatusOrderBySubmitDateDesc(String status);
    Optional<WorkloadEvolution> findBySiglumAndStatusAndExercise(Siglum siglum, String status, String exercise);
    List<WorkloadEvolution> findBySiglumAndExercise(Siglum siglum, String exercise);

    @Query("SELECT we.siglum.siglumHR, we.status " +
            "FROM WorkloadEvolution we " +
            "WHERE we.siglum IN :siglumVisible " +
            "AND we.exercise = :exercise " +
            "AND we.submitDate = (" +
            "    SELECT MAX(w.submitDate) " +
            "    FROM WorkloadEvolution w " +
            "    WHERE w.siglum = we.siglum " +
            "    AND w.exercise = :exercise" +
            ")")
    List<Object[]> findLatestStatusBySiglumAndExercise(@Param("siglumVisible") List<Siglum> siglumVisible,
                                                       @Param("exercise") String exercise);



    @Query("SELECT we.siglum.siglumHR " +
            "FROM WorkloadEvolution we " +
            "WHERE we.siglum IN :siglumVisible " +
            "AND we.status IN :statusList " +
            "AND we.exercise = :exercise " +
            "AND we.submitDate = (" +
            "    SELECT MAX(w.submitDate) " +
            "    FROM WorkloadEvolution w " +
            "    WHERE w.siglum = we.siglum " +
            "    AND w.status IN :statusList " +
            "    AND w.exercise = :exercise" +
            ")")
    List<String> findSiglumHRBySiglumAndStatusAndExerciseIn(
            @Param("siglumVisible") List<Siglum> siglumVisible,
            @Param("statusList") List<String> statusList,
            @Param("exercise") String exercise);

    @Query("SELECT we.siglum " +
            "FROM WorkloadEvolution we " +
            "WHERE we.siglum IN :siglumVisible " +
            "AND we.exercise = :exercise " +
            "AND we.submitDate = (" +
            "    SELECT MAX(w.submitDate) " +
            "    FROM WorkloadEvolution w " +
            "    WHERE w.siglum = we.siglum " +
            "    AND w.exercise = :exercise" +
            ") " +
            "AND we.status IN :statusList")
    List<Siglum> findSiglumBySiglumAndStatusAndExerciseIn(
            @Param("siglumVisible") List<Siglum> siglumVisible,
            @Param("statusList") List<String> statusList,
            @Param("exercise") String exercise);

    @Query("SELECT we.siglum.siglumHR " +
            "FROM WorkloadEvolution we " +
            "WHERE we.siglum IN :siglumVisible " +
            "AND (we.status IN :approvedStatuses OR we.status = :pendingStatus) " +
            "AND we.exercise = :exercise " +
            "AND we.submitDate = (" +
            "    SELECT MAX(w.submitDate) " +
            "    FROM WorkloadEvolution w " +
            "    WHERE w.siglum = we.siglum " +
            "    AND (w.status IN :approvedStatuses OR w.status = :pendingStatus) " +
            "    AND w.exercise = :exercise" +
            ")")
    List<String> findSiglumHRBySiglumAndStatusesAndExercise(@Param("siglumVisible") List<Siglum> siglumVisible,
                                                            @Param("approvedStatuses") List<String> approvedStatuses,
                                                            @Param("pendingStatus") String pendingStatus,
                                                            @Param("exercise") String exercise);

    @Query("SELECT we.status " +
            "FROM WorkloadEvolution we " +
            "WHERE we.siglum.siglumHR = :siglumHR " +
            "AND we.exercise = :exercise " +
            "ORDER BY we.submitDate DESC " +
            "LIMIT 1")
    String getStatusBySiglumAndExercise(@Param("siglumHR") String siglumHR,
                                        @Param("exercise") String exercise);

    @Query("SELECT we " +
            "FROM WorkloadEvolution we " +
            "WHERE we.siglum = :siglum " +
            "AND we.exercise = :exercise " +
            "AND we.submitDate = (" +
            "    SELECT MAX(w.submitDate) " +
            "    FROM WorkloadEvolution w " +
            "    WHERE w.siglum = :siglum " +
            "    AND w.exercise = :exercise" +
            ")")
    Optional<WorkloadEvolution> findBySiglumAndExerciseWithLatestSubmitDate(@Param("siglum") Siglum siglum,
                                                                            @Param("exercise") String exercise);

    boolean existsByExercise(String exercise);

    @Query("SELECT w FROM WorkloadEvolution w " +
            "WHERE w.status = :openedStatus " +
            "AND w.exercise NOT IN (" +
            "  SELECT wClosed.exercise FROM WorkloadEvolution wClosed " +
            "  WHERE wClosed.status = :closedStatus" +
            ")")
    List<WorkloadEvolution> findByStatusAndExerciseNotInClosed(
            @Param("openedStatus") String openedStatus,
            @Param("closedStatus") String closedStatus
    );

    @Query("SELECT w FROM WorkloadEvolution w " +
            "WHERE w.status = :openedStatus " +
            "AND w.exercise NOT IN (" +
            "  SELECT wClosed.exercise FROM WorkloadEvolution wClosed " +
            "  WHERE wClosed.status = :closedStatus" +
            "  AND YEAR(w.submitDate) = :year " +
            ") " +
            "AND YEAR(w.submitDate) = :year " +
            "ORDER BY YEAR(w.submitDate) DESC")
    List<WorkloadEvolution> findByStatusAndExerciseNotInClosedByYear(
            @Param("openedStatus") String openedStatus,
            @Param("closedStatus") String closedStatus,
            @Param("year") int year
    );

    @Query("SELECT w FROM WorkloadEvolution w " +
            "WHERE w.status = :openedStatus " +
            "AND YEAR(w.submitDate) = :year " +
            "ORDER BY w.submitDate DESC")
    List<WorkloadEvolution> findByStatusAndSiglumByYear(
            @Param("openedStatus") String openedStatus,
            @Param("year") int year
    );

    @Query("SELECT w FROM WorkloadEvolution w " +
            "WHERE w.status = :openedStatus " +
            "AND YEAR(w.submitDate) = :year " +
            "AND w.exercise <> :excludedExercise " +
            "ORDER BY w.submitDate DESC")
    List<WorkloadEvolution> findPreviowByStatusAndSiglumByYear(
            @Param("openedStatus") String openedStatus,
            @Param("year") int year,
            @Param("excludedExercise") String excludedExercise
    );

    @Query("""
    SELECT we.exercise
    FROM WorkloadEvolution we
    WHERE LOWER(we.status) = LOWER('CLOSED')
      AND (
          (LOWER(we.exercise) LIKE LOWER('OP%') AND we.submitDate = (
              SELECT MAX(we2.submitDate)
              FROM WorkloadEvolution we2
              WHERE LOWER(we2.status) = LOWER('CLOSED')
                AND LOWER(we2.exercise) LIKE LOWER('OP%')
          ))
          OR
          (LOWER(we.exercise) LIKE LOWER('FCII%') AND we.submitDate = (
              SELECT MAX(we2.submitDate)
              FROM WorkloadEvolution we2
              WHERE LOWER(we2.status) = LOWER('CLOSED')
                AND LOWER(we2.exercise) LIKE LOWER('FCII%')
          ))
      )
    ORDER BY we.submitDate DESC
    """)
    List<String> findLatestClosedExercisesByType();

    @Query("""
    SELECT we.exercise
    FROM WorkloadEvolution we
    WHERE LOWER(we.status) = LOWER('CLOSED')
    AND (we.submitDate = (
            SELECT MAX(we2.submitDate)
            FROM WorkloadEvolution we2
            WHERE LOWER(we2.status) = LOWER('CLOSED')
        ))
    ORDER BY we.submitDate DESC
    """)
    List<String> findLastClosedExercise();

    @Query("""
SELECT we.exercise
FROM WorkloadEvolution we
WHERE LOWER(we.status) = LOWER('OPENED')
AND YEAR(we.submitDate) = :year
ORDER BY we.submitDate DESC
""")
    List<String> findLastOpenedExercise(@Param("year") int year);

    @Query("""
SELECT we.exercise
FROM WorkloadEvolution we
WHERE LOWER(we.status) = LOWER('CLOSED')
AND YEAR(we.submitDate) = :year
ORDER BY we.submitDate DESC
""")
    List<String> findLastClosedExercise(@Param("year") int year);

    @Query("""
    SELECT we.exercise
    FROM WorkloadEvolution we
    WHERE LOWER(we.status) <> LOWER('CLOSED')
    AND we.siglum IN :siglumFiltered
    AND (we.submitDate = (
            SELECT MAX(we2.submitDate)
            FROM WorkloadEvolution we2
            WHERE LOWER(we2.status) <> LOWER('CLOSED')
        ))
    ORDER BY we.submitDate DESC
    """)
    List<String> findLastNoClosedExercise(List<Siglum> siglumFiltered);

    @Query("""
    SELECT we.exercise
    FROM WorkloadEvolution we
    WHERE LOWER(we.status) = LOWER('CLOSED')
    AND we.siglum IN :siglumFiltered
    AND EXTRACT(YEAR FROM we.submitDate) = :year
    AND we.submitDate = (
            SELECT MAX(we2.submitDate)
            FROM WorkloadEvolution we2
            WHERE LOWER(we2.status) = LOWER('CLOSED')
            AND EXTRACT(YEAR FROM we2.submitDate) = :year
        )
    ORDER BY we.submitDate DESC
    """)
    List<String> findLastClosedExerciseByYear(List<Siglum> siglumFiltered, @Param("year") int year);

    @Query("""
    SELECT DISTINCT we.exercise
    FROM WorkloadEvolution we
    WHERE LOWER(we.status) = LOWER('opened')
      AND we.exercise NOT IN (
          SELECT DISTINCT we2.exercise
          FROM WorkloadEvolution we2
          WHERE LOWER(we2.status) = LOWER('closed')
      )
    """)
    String findLatestOpenedExerciseWithoutClosedState();

    @Query("""
    SELECT we
    FROM WorkloadEvolution we
    WHERE we.exercise = :exercise
      AND we IN :filteredList
      AND we.siglum IN :siglums
      AND (
        we.status NOT LIKE 'first_submission%'
        OR we.submitDate = (
          SELECT MAX(w.submitDate)
          FROM WorkloadEvolution w
          WHERE w.siglum = we.siglum
            AND w.status LIKE 'first_submission%'
        )
      )
    ORDER BY we.siglum.id, we.submitDate ASC
    """)
    List<WorkloadEvolution> findAllWorkloadsInLatestStateBySiglum(@Param("exercise") String exercise,
                                                                  @Param("filteredList") List<WorkloadEvolution> filteredList,
                                                                  @Param("siglums") List<Siglum> siglums);

    @Query("SELECT we " +
            "FROM WorkloadEvolution we " +
            "WHERE we.siglum = :siglum " +
            "AND we.exercise = :exercise " +
            "AND we.status LIKE 'first_submission_approved%' " +
            "ORDER BY we.submitDate ASC LIMIT 1")
    Optional<WorkloadEvolution> findFirstBySiglumAndExerciseWithOlderSubmitDate(@Param("siglum") Siglum siglum,
                                                                                @Param("exercise") String exercise);
}