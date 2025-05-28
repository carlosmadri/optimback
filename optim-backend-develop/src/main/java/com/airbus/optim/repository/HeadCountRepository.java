package com.airbus.optim.repository;

import com.airbus.optim.entity.HeadCount;
import com.airbus.optim.entity.Siglum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data JPA repository for the HeadCount entity, exposed as a REST resource.
 */
@Repository
public interface HeadCountRepository extends JpaRepository<HeadCount, Long> {
    @Query("SELECT SUM(h.fTE) FROM HeadCount h " +
            "WHERE h.year = :currentYear")
    Float sumTotalFTEForCurrentYear(@Param("currentYear") String currentYear);

    @Query("""
    SELECT SUM(h.fTE)
    FROM HeadCount h
    WHERE h.year = :currentYear
        AND LOWER(h.refCount) = LOWER(:refCount)
        AND h.siglum IN :siglumFiltered
    """)
    Float sumTotalFTEForCurrentYearExercise(
            @Param("currentYear") String currentYear,
            @Param("refCount") String refCount,
            @Param("siglumFiltered") List<Siglum> siglumFiltered
    );

}
