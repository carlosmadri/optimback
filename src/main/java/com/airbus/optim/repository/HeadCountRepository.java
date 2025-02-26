package com.airbus.optim.repository;

import com.airbus.optim.entity.HeadCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

/**
 * Spring Data JPA repository for the HeadCount entity, exposed as a REST resource.
 */
@Repository
public interface HeadCountRepository extends JpaRepository<HeadCount, Long> {
    @Query("SELECT SUM(h.fTE) FROM HeadCount h " +
            "WHERE h.year = :currentYear")
    Float sumTotalFTEForCurrentYear(@Param("currentYear") String currentYear);

}
