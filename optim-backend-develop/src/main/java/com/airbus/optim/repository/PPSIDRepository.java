package com.airbus.optim.repository;

import com.airbus.optim.entity.PPSID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the PPSID entity, exposed as a REST resource.
 */
@Repository
public interface PPSIDRepository extends JpaRepository<PPSID, Long> {

    @Query("SELECT p.ppsid FROM PPSID p where p.ppsid is not null")
    List<String> findAllPPSID();

    Optional<PPSID> findByPpsid(String ppsid);

}
