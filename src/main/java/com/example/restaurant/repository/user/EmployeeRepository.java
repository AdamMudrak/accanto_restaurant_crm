package com.example.restaurant.repository.user;

import com.example.restaurant.entity.user.Employee;
import com.example.restaurant.entity.user.Role;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Page<Employee> findAllByIsDeletedFalse(Pageable pageable);

    long countByIsDeletedFalseAndEmailConfirmedTrueAndRole_Name(Role.RoleType role);

    boolean existsByEmail(String email);

    Optional<Employee> findByEmail(String email);

    boolean existsByPendingEmail(String pendingEmail);
}
