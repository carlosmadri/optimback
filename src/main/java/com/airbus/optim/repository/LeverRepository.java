package com.airbus.optim.repository;

import com.airbus.optim.entity.Employee;
import com.airbus.optim.entity.Lever;
import com.airbus.optim.repository.projections.LeverTypeFteSum;
import com.airbus.optim.utils.IdentifiableRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the Lever entity, exposed as a REST resource.
 */
@Repository
public interface LeverRepository extends JpaRepository<Lever, Long>, JpaSpecificationExecutor<Lever>, IdentifiableRepository<Lever> {

    Optional<Lever> findByEmployee(Employee employee);

    @Query("SELECT SUM(l.fTE) FROM Lever l " +
            "WHERE l.employee IN :employees " +
            "AND l.startDate > :currentDate " +
            "AND (l.endDate IS NULL OR l.endDate >= :endOfYear) " +
            "AND l.employee.impersonal = false " +
            "AND LOWER(l.leverType) NOT IN (LOWER('Borrowed'), LOWER('Leased'))")
    Float sumFTEsLeaversAfterStartDate(@Param("currentDate") Instant currentDate,
                                       @Param("endOfYear") Instant endOfYear,
                                       @Param("employees") List<Employee> employees);

    @Query("SELECT ABS(SUM(l.fTE)) " +
            "FROM Lever l " +
            "WHERE l.employee IN :employees " +
            "AND l.startDate <= :currentDate " +
            "AND (l.endDate IS NULL OR l.endDate <= :endOfYear) " +
            "AND l.employee.impersonal = false " +
            "AND LOWER(l.leverType) NOT IN (LOWER('Borrowed'), LOWER('Leased'))")
    Float sumFTEsRecoveriesBeforeEndOfYear(@Param("currentDate") Instant currentDate,
                                           @Param("endOfYear") Instant endOfYear,
                                           @Param("employees") List<Employee> employees);

    @Query("SELECT SUM(l.fTE) FROM Lever l " +
            "WHERE l.employee IN :employees " +
            "AND l.startDate > :currentDate " +
            "AND l.employee.impersonal = true " +
            "AND LOWER(l.leverType) IN (LOWER('Redeployment'))")
    Float sumFTEsForRedeployment(@Param("currentDate") Instant currentDate,
                                 @Param("employees") List<Employee> employees);

    @Query("SELECT SUM(l.fTE) FROM Lever l " +
            "WHERE l.employee IN :employees " +
            "AND l.startDate > :currentDate " +
            "AND l.employee.impersonal = true " +
            "AND LOWER(l.leverType) IN (LOWER('Perimeter Change'))")
    Float sumFTEsForPerimeterChanges(@Param("currentDate") Instant currentDate,
                                     @Param("employees") List<Employee> employees);

    @Query("SELECT l.employee FROM Lever l " +
            "WHERE l.employee IN :employees " +
            "AND l.startDate <= :currentDate " +
            "AND l.endDate <= :endOfYear " +
            "AND LOWER(l.leverType) = LOWER('Borrowed')")
    List<Employee> findEmployeesWithBorrowedLever(@Param("currentDate") Instant currentDate,
                                                  @Param("endOfYear") Instant endOfYear,
                                                  @Param("employees") List<Employee> employees);

    @Query("SELECT l.employee FROM Lever l " +
            "WHERE l.employee IN :employees " +
            "AND l.startDate <= :currentDate " +
            "AND l.endDate <= :endOfYear " +
            "AND LOWER(l.leverType) = LOWER('Leased')")
    List<Employee> findEmployeesWithLeasedLever(@Param("currentDate") Instant currentDate,
                                                @Param("endOfYear") Instant endOfYear,
                                                @Param("employees") List<Employee> employees);

    @Query("SELECT SUM(l.fTE) FROM Lever l " +
            "WHERE l.employee IN :employees " +
            "AND l.startDate > :currentDate " +
            "AND l.endDate >= :endOfYear " +
            "AND LOWER(l.leverType) = LOWER('Internal Mobility') " +
            "AND l.employee.impersonal = false")
    Float sumFTEsForInternalMobility(@Param("currentDate") Instant currentDate,
                                     @Param("endOfYear") Instant endOfYear,
                                     @Param("employees") List<Employee> employees);

    @Query(value = "SELECT COALESCE(MAX(l.id), 0) + 1 FROM lever l", nativeQuery = true)
    Long findNextAvailableId();

    @Query("SELECT l.leverType AS leverType, SUM(l.fTE) AS fteSum " +
            "FROM Lever l " +
            "WHERE l.employee IN :employees " +
            "AND l.startDate > :currentDate " +
            "AND (l.endDate IS NULL OR l.endDate > :endOfYear) " +
            "GROUP BY l.leverType")
    List<LeverTypeFteSum> sumLeaversGroupedByType(@Param("currentDate") Instant currentDate,
                                                  @Param("endOfYear") Instant endOfYear,
                                                  @Param("employees") List<Employee> employees);

    @Query("SELECT l.leverType AS leverType, SUM(ABS(l.fTE)) AS fteSum " +
            "FROM Lever l " +
            "WHERE l.employee IN :employees " +
            "AND l.startDate < :currentDate " +
            "AND (l.endDate IS NULL OR l.endDate < :endOfYear) " +
            "GROUP BY l.leverType")
    List<LeverTypeFteSum> sumRecoveriesGroupedByType(@Param("currentDate") Instant currentDate,
                                                     @Param("endOfYear") Instant endOfYear,
                                                     @Param("employees") List<Employee> employees);

}

