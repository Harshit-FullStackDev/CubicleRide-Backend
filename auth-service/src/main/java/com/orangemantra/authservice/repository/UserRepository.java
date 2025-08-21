package com.orangemantra.authservice.repository;
import com.orangemantra.authservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmpId(String empId);

    Optional<User> findByEmailHash(String emailHash);
    boolean existsByEmailHash(String emailHash);

    boolean existsByEmpId(String empId);
}