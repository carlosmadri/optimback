package com.airbus.optim.repository;

import com.airbus.optim.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the Location entity, exposed as a REST resource.
 */
@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    Optional<Location> findByCountryAndSite(String country, String site);

}

