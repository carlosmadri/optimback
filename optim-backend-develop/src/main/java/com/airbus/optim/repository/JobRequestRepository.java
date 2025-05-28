package com.airbus.optim.repository;

import com.airbus.optim.dto.JobRequestTypeCountDTO;
import com.airbus.optim.entity.JobRequest;
import com.airbus.optim.entity.Siglum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data JPA repository for the JobRequest entity, exposed as a REST resource.
 */
@Repository
public interface JobRequestRepository extends JpaRepository<JobRequest, Long>, JpaSpecificationExecutor<JobRequest> {

    @Query("SELECT COUNT(jr) FROM JobRequest jr WHERE jr IN :jobRequests AND LOWER(jr.status) = LOWER('Filled') " +
            "AND jr.startDate > :currentDate AND (jr.releaseDate IS NULL OR jr.releaseDate >= :endOfYear)")
    Float countFilledJobRequestsAfterStartDate(@Param("jobRequests") List<JobRequest> jobRequests,
                                               @Param("currentDate") Instant currentDate,
                                               @Param("endOfYear") Instant endOfYear);

    @Query("SELECT COUNT(jr) FROM JobRequest jr WHERE jr IN :jobRequests AND LOWER(jr.status) = LOWER('Opened') " +
            "AND jr.startDate > :currentDate AND (jr.releaseDate IS NULL OR jr.releaseDate >= :endOfYear)")
    Float countOpenedJobRequestsAfterStartDate(@Param("jobRequests") List<JobRequest> jobRequests,
                                               @Param("currentDate") Instant currentDate,
                                               @Param("endOfYear") Instant endOfYear);

    @Query("SELECT COUNT(jr) FROM JobRequest jr WHERE jr IN :jobRequests AND LOWER(jr.status) IN " +
            "(LOWER('Validation Required'), LOWER('QMC Approved'), LOWER('SHRBP/HO T1Q Approved'), LOWER('COO Approved')) " +
            "AND jr.startDate > :currentDate AND (jr.releaseDate IS NULL OR jr.releaseDate >= :endOfYear)")
    Float countValidationProcessJobRequests(@Param("jobRequests") List<JobRequest> jobRequests,
                                            @Param("currentDate") Instant currentDate,
                                            @Param("endOfYear") Instant endOfYear);

    @Query("SELECT COUNT(jr) FROM JobRequest jr WHERE jr IN :jobRequests AND LOWER(jr.status) = LOWER('On Hold') " +
            "AND jr.startDate > :currentDate AND (jr.releaseDate IS NULL OR jr.releaseDate >= :endOfYear)")
    Float countOnHoldJobRequestsAfterStartDate(@Param("jobRequests") List<JobRequest> jobRequests,
                                               @Param("currentDate") Instant currentDate,
                                               @Param("endOfYear") Instant endOfYear);

    @Query("SELECT new com.airbus.optim.dto.JobRequestTypeCountDTO(jr.type, COUNT(jr)) " +
            "FROM JobRequest jr " +
            "WHERE jr IN :jobRequests " +
            "AND LOWER(jr.status) NOT IN (LOWER('Cancelled'), LOWER('Closed')) " +
            "GROUP BY jr.type")
    List<JobRequestTypeCountDTO> countJobRequestsByType(@Param("jobRequests") List<JobRequest> jobRequests);

    @Query("SELECT COUNT(j) FROM JobRequest j " +
            "WHERE (LOWER(j.status) = LOWER('FILLED') OR LOWER(j.status) = LOWER('OPENED')) " +
            "AND j.startDate <= :currentDate " +
            "AND (j.releaseDate IS NULL OR j.releaseDate >= :endOfYear) " +
            "AND j IN :jobRequests")
    Long countJobRequestsWithFilledOrOpenedStatus(@Param("currentDate") Instant currentDate,
                                                  @Param("endOfYear") Instant endOfYear,
                                                  @Param("jobRequests") List<JobRequest> jobRequests);


    @Query("SELECT COUNT(j) FROM JobRequest j " +
            "WHERE (LOWER(j.status) IN (LOWER('Validation Required'), LOWER('QMC Approved'), LOWER('SHRBP/HO T1Q Approved'), LOWER('COO Approved'))) " +
            "AND j.startDate <= :currentDate " +
            "AND (j.releaseDate IS NULL OR j.releaseDate >= :endOfYear) " +
            "AND j IN :jobRequests")
    Long countJobRequestsWithValidationOrProgressStatus(@Param("currentDate") Instant currentDate,
                                                        @Param("endOfYear") Instant endOfYear,
                                                        @Param("jobRequests") List<JobRequest> jobRequests);

    @Query("SELECT COUNT(j) FROM JobRequest j " +
            "WHERE LOWER(j.status) = LOWER('ON HOLD') " +
            "AND j.startDate <= :currentDate " +
            "AND (j.releaseDate IS NULL OR j.releaseDate >= :endOfYear) " +
            "AND j IN :jobRequests")
    Long countJobRequestsWithOnHoldStatus(@Param("currentDate") Instant currentDate,
                                          @Param("endOfYear") Instant endOfYear,
                                          @Param("jobRequests") List<JobRequest> jobRequests);

    Page<JobRequest> findAll(Specification<JobRequest> spec, Pageable pageable);

    @Query("""
    	       SELECT jr
    	       FROM JobRequest jr
    	       LEFT JOIN FETCH jr.siglum
    	       LEFT JOIN FETCH jr.costCenter
    	       WHERE jr.status IN :statusList
    	       AND jr IN :jobRequestList
    	       AND jr.siglum IN :siglumVisibilityList
    	       """)
    	List<JobRequest> findJobsByStatus(
    	        @Param("statusList") List<String> statusList,
    	        @Param("jobRequestList") List<JobRequest> jobRequestList,
    	        @Param("siglumVisibilityList") List<Siglum> siglumVisibilityList);
}


