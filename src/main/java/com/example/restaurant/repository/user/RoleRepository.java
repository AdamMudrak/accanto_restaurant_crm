package com.example.restaurant.repository.user;

import com.example.restaurant.entity.user.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByName(Role.RoleType name);
}
