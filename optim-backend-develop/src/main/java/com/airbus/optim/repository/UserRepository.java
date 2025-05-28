package com.airbus.optim.repository;

import com.airbus.optim.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the {@link User} entity, exposed as a REST resource.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findOneByEmailIgnoreCase(String email);
}
