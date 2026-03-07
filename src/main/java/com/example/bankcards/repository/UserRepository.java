package com.example.bankcards.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.bankcards.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
