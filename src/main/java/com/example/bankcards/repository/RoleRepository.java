package com.example.bankcards.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.RoleName;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
