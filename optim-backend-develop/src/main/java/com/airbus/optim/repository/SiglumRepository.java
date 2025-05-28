package com.airbus.optim.repository;

import com.airbus.optim.entity.Siglum;
import com.airbus.optim.entity.Workload;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiglumRepository extends JpaRepository<Siglum, Long> {
    Page<Siglum> findAll(Pageable pageable);

    @Query("SELECT s FROM Siglum s")
    List<Siglum> findAllSiglums();

    Optional<Siglum> findBySiglumHR(String siglumHR);

    List<Siglum> findBySiglumHRIn(List<String> siglumsHR);

    List<Workload>  findAll(Specification<Workload> spec);

    @Query("SELECT s FROM Siglum s " +
            "WHERE s.siglumHR LIKE :prefix" + "%")
    List<Siglum> findBySiglumHRStartingWith(@Param("prefix") String prefix);
}
