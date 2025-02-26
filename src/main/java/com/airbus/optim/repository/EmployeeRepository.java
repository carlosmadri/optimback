package com.airbus.optim.repository;

import com.airbus.optim.entity.Employee;
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

}


