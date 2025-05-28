package com.airbus.optim.repository;

import com.airbus.optim.entity.CostCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the CostCenter entity, exposed as a REST resource.
 */
@Repository
public interface CostCenterRepository extends JpaRepository<CostCenter, Long>{
    Optional<CostCenter> findByCostCenterCode(String costCenterCode);
}
