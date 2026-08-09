package com.example.restaurant.repository.management;

import com.example.restaurant.entity.management.EmployeeShift;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeShiftRepository extends JpaRepository<EmployeeShift, Long> {

    @Query("""
            SELECT employeeShift FROM EmployeeShift employeeShift
            WHERE (:employeeId IS NULL OR employeeShift.employee.id = :employeeId)
            AND (:from IS NULL OR employeeShift.workDate >= :from)
            AND (:to IS NULL OR employeeShift.workDate <= :to)
            """)
    Page<EmployeeShift> findSchedule(
            @Param("employeeId") Long employeeId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    boolean existsByEmployeeIdAndWorkDateAndStartsAtLessThanAndEndsAtGreaterThan(
            Long employeeId,
            LocalDate workDate,
            LocalTime endsAt,
            LocalTime startsAt);

    @Query("""
            SELECT COUNT(employeeShift) > 0 FROM EmployeeShift employeeShift
            WHERE employeeShift.id <> :id
            AND employeeShift.employee.id = :employeeId
            AND employeeShift.workDate = :workDate
            AND employeeShift.startsAt < :endsAt
            AND employeeShift.endsAt > :startsAt
            """)
    boolean existsOverlappingShiftExceptId(
            @Param("id") Long id,
            @Param("employeeId") Long employeeId,
            @Param("workDate") LocalDate workDate,
            @Param("startsAt") LocalTime startsAt,
            @Param("endsAt") LocalTime endsAt);
}
