package com.airbus.optim.repository;

import com.airbus.optim.entity.PurchaseOrders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the PurchaseOrders entity, exposed as a REST resource.
 */
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface PurchaseOrdersRepository extends JpaRepository<PurchaseOrders, Long> {

    Page<PurchaseOrders> findAll(Pageable pageable);
}

