package com.orangemantra.authservice.repository;
import com.orangemantra.authservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmpId(String empId);
    Optional<User> findByEmpId(String empId);
}