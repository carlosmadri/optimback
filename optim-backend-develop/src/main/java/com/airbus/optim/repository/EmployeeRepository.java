package com.airbus.optim.repository;

import com.airbus.optim.dto.ReportEndOfYear.ActiveWorkforceReportDTO;
import com.airbus.optim.dto.ReportEndOfYear.ActiveWorkforceAndSiteDTO;
import com.airbus.optim.entity.Employee;
import com.airbus.optim.entity.Siglum;
import com.airbus.optim.utils.IdentifiableRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee>, IdentifiableRepository<Employee> {
    Optional<Employee> findByEmployeeId(Long employeeId);

    Page<Employee> findAll(Specification<Employee> spec, Pageable pageable);

    Optional<Employee> findOne(Specification<Employee> spec);

    @Query(value = "SELECT COALESCE(MAX(e.id), 0) + 1 FROM employee e", nativeQuery = true)
    Long findNextAvailableId();

    @Query("SELECT SUM(e.fTE) FROM Employee e WHERE e.activeWorkforce = :activeWorkforce AND e IN :employees AND e.impersonal = false")
    Float sumFTEsByActiveWorkforce(@Param("activeWorkforce") String activeWorkforce, @Param("employees") List<Employee> employees);

    @Query("""
                SELECT new com.airbus.optim.dto.ReportEndOfYear.ActiveWorkforceReportDTO(e.siglum.siglum4, e.activeWorkforce, COALESCE(SUM(e.fTE), 0))
                FROM Employee e
                WHERE e.activeWorkforce = :activeWorkforce
                AND e IN :employees
                AND e.siglum IN :siglumVisibilityList
                AND e.impersonal = false
                GROUP BY e.siglum.siglum4, e.activeWorkforce
                ORDER bY e.siglum.siglum4 ASC
            """)
    List<ActiveWorkforceReportDTO> sumFTEsByActiveWorkforceAndSiglum4(
            @Param("activeWorkforce") String activeWorkforce,
            @Param("employees") List<Employee> employees,
            @Param("siglumVisibilityList") List<Siglum> siglumVisibilityList);

    @Query("""
                SELECT new com.airbus.optim.dto.ReportEndOfYear.ActiveWorkforceReportDTO(e.siglum.siglum5, e.activeWorkforce, COALESCE(SUM(e.fTE), 0))
                FROM Employee e
                WHERE e.activeWorkforce = :activeWorkforce
                AND e IN :employees
                AND e.siglum IN :siglumVisibilityList
                AND e.impersonal = false
                GROUP BY e.siglum.siglum5, e.activeWorkforce
                ORDER bY e.siglum.siglum5 ASC
            """)
    List<ActiveWorkforceReportDTO> sumFTEsByActiveWorkforceAndSiglum5(
            @Param("activeWorkforce") String activeWorkforce,
            @Param("employees") List<Employee> employees,
            @Param("siglumVisibilityList") List<Siglum> siglumVisibilityList);

    @Query("""
    	    SELECT new com.airbus.optim.dto.ReportEndOfYear.ActiveWorkforceReportDTO(
    	           e.costCenter.location.site, 
    	           e.activeWorkforce, 
    	           COALESCE(SUM(e.fTE), 0))
    	    FROM Employee e
    	    WHERE e.activeWorkforce = :activeWorkforce
    	      AND e IN :employees
    	      AND e.siglum IN :siglumVisibilityList
    	      AND e.impersonal = false
    	    GROUP BY e.costCenter.location.site, e.activeWorkforce
    	    ORDER BY e.costCenter.location.site ASC
    	""")
    	List<ActiveWorkforceReportDTO> sumFTEsByActiveWorkforceAndSite(
    	        @Param("activeWorkforce") String activeWorkforce,
    	        @Param("employees") List<Employee> employees,
    	        @Param("siglumVisibilityList") List<Siglum> siglumVisibilityList);

    @Query("""
                SELECT e
                FROM Employee e
                WHERE e IN :employees
                AND e.siglum IN :siglumVisibilityList
            """)
    List<Employee> getEmployeesFilteredBySiglumVisibility(
            @Param("employees") List<Employee> employees,
            @Param("siglumVisibilityList") List<Siglum> siglumVisibilityList);

    @Query("""
                SELECT e FROM Employee e
                WHERE e IN :employees
                AND e.siglum IN :siglumVisibilityList
            """)
    List<Employee> getEmployeesWorkforceBySiglumVisibility(
            @Param("employees") List<Employee> employees,
            @Param("siglumVisibilityList") List<Siglum> siglumVisibilityList);

    /*@Query("""
                SELECT e
                FROM Employee e
                WHERE e IN :employees
                AND e.siglum IN :siglumVisibilityList
                GROUP BY e.
            """)
    List<Employee> getMontly(
            @Param("employees") List<Employee> employees,
            @Param("siglumVisibilityList") List<Siglum> siglumVisibilityList);*/

}



