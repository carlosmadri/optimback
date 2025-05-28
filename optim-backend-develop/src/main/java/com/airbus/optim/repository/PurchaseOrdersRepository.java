package com.airbus.optim.repository;

import com.airbus.optim.dto.subcontractingDto.QuartersDTO;
import com.airbus.optim.dto.subcontractingDto.SubcontractingDataDTO;
import com.airbus.optim.entity.PurchaseOrders;
import com.airbus.optim.entity.Siglum;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the PurchaseOrders entity, exposed as a REST resource.
 */
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface PurchaseOrdersRepository extends JpaRepository<PurchaseOrders, Long> {

    List<PurchaseOrders>  findAll(Specification<PurchaseOrders> spec);

    Page<PurchaseOrders> findAll(Pageable pageable);

    @Query("SELECT p " +
            "FROM PurchaseOrders p " +
            "WHERE p IN :purchaseOrderList " +
            "AND p.siglum IN :siglumVisibleList")
    List<PurchaseOrders> findAllPurchaseOrdersFiltered(List<PurchaseOrders> purchaseOrderList, List<Siglum> siglumVisibleList);

    @Query("SELECT new com.airbus.optim.dto.subcontractingDto.SubcontractingDataDTO(" +
                "p.siglum.siglumHR, l.site, p.description, p.provider, p.approved, p.quarter, p.year, p.kEur, p.orderRequest, p.id, p.hmg, p.pep) " +
            "FROM PurchaseOrders p INNER JOIN Location l ON l.id = p.locationId " +
            "WHERE p IN :purchaseOrderList " +
            "AND p.siglum IN :siglumVisibleList")
    Page<SubcontractingDataDTO> findAllSelectiveTableElements (
            Pageable pageable, List<PurchaseOrders> purchaseOrderList, List<Siglum> siglumVisibleList);

    @Query("SELECT new com.airbus.optim.dto.subcontractingDto.QuartersDTO(quarter, COALESCE(SUM(kEur), 0)) " +
            "FROM PurchaseOrders p " +
            "WHERE year = :currentYear " +
            "AND approved = :approved " +
            "AND p IN :purchaseOrderList " +
            "GROUP BY quarter " +
            "ORDER BY quarter ASC")
    List<QuartersDTO> groupPerQuarter(List<PurchaseOrders> purchaseOrderList, String approved, int currentYear);
}

