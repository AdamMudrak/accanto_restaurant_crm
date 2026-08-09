package com.example.restaurant.repository.user;

import com.example.restaurant.entity.user.Employee;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    boolean existsByEmail(String email);

    Optional<Employee> findByEmail(String email);

    boolean existsByEmailAndIsDeletedTrue(String email);

    boolean existsByPendingEmail(String pendingEmail);
}
