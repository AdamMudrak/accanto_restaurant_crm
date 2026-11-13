package com.example.restaurant.service.management;

import com.example.restaurant.dto.request.management.CreateEmployeeShiftRequest;
import com.example.restaurant.dto.request.management.UpdateEmployeeShiftRequest;
import com.example.restaurant.dto.response.list.management.EmployeeShiftListResponse;
import com.example.restaurant.dto.response.management.EmployeeShiftResponse;
import com.example.restaurant.entity.management.EmployeeShift;
import com.example.restaurant.entity.user.Employee;
import com.example.restaurant.exception.types.conflict.TimeConflictException;
import com.example.restaurant.exception.types.notfound.EmployeeShiftNotFoundException;
import com.example.restaurant.mapper.management.EmployeeShiftMapper;
import com.example.restaurant.repository.management.EmployeeShiftRepository;
import com.example.restaurant.repository.management.RestaurantRepository;
import com.example.restaurant.repository.user.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeShiftService {
    private final EmployeeShiftRepository employeeShiftRepository;
    private final EmployeeRepository employeeRepository;
    private final RestaurantRepository restaurantRepository;
    private final EmployeeShiftMapper employeeShiftMapper;

    @Transactional(readOnly = true)
    public EmployeeShiftListResponse getEmployeeShifts(
            Long employeeId,
            LocalDate from,
            LocalDate to,
            Pageable pageable) {
        Page<EmployeeShift> employeeShifts =
                employeeShiftRepository.findSchedule(employeeId, from, to, pageable);

        return new EmployeeShiftListResponse(
                employeeShiftMapper.toResponse(employeeShifts.getContent()),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                employeeShifts.getTotalElements());
    }

    @Transactional(readOnly = true)
    public EmployeeShiftResponse getEmployeeShiftById(Long id) {
        return employeeShiftMapper.toResponse(findEmployeeShiftById(id));
    }

    public EmployeeShiftResponse createEmployeeShift(CreateEmployeeShiftRequest request) {
        Employee employee = findEmployeeById(request.employeeId());
        validateTimeRange(request.startsAt(), request.endsAt());
        validateNoOverlap(
                employee.getId(), request.workDate(), request.startsAt(), request.endsAt());

        LocalDateTime now = LocalDateTime.now();
        EmployeeShift employeeShift = new EmployeeShift();
        employeeShift.setEmployee(employee);
        employeeShift.setWorkDate(request.workDate());
        employeeShift.setStartsAt(request.startsAt());
        employeeShift.setEndsAt(request.endsAt());
        employeeShift.setNote(request.note());
        employeeShift.setCreatedAt(now);
        employeeShift.setUpdatedAt(now);

        return employeeShiftMapper.toResponse(employeeShiftRepository.save(employeeShift));
    }

    public EmployeeShiftResponse updateEmployeeShiftById(
            Long id,
            UpdateEmployeeShiftRequest request) {
        EmployeeShift employeeShift = findEmployeeShiftById(id);

        if (request.employeeId() != null) {
            employeeShift.setEmployee(findEmployeeById(request.employeeId()));
        }
        if (request.workDate() != null) {
            employeeShift.setWorkDate(request.workDate());
        }
        if (request.startsAt() != null) {
            employeeShift.setStartsAt(request.startsAt());
        }
        if (request.endsAt() != null) {
            employeeShift.setEndsAt(request.endsAt());
        }
        if (request.note() != null) {
            employeeShift.setNote(request.note());
        }

        validateTimeRange(employeeShift.getStartsAt(), employeeShift.getEndsAt());
        validateNoOverlapExceptSelf(employeeShift);
        employeeShift.setUpdatedAt(LocalDateTime.now());

        return employeeShiftMapper.toResponse(employeeShiftRepository.save(employeeShift));
    }

    public void deleteEmployeeShiftById(Long id) {
        EmployeeShift employeeShift = findEmployeeShiftById(id);

        employeeShiftRepository.delete(employeeShift);
    }

    private EmployeeShift findEmployeeShiftById(Long id) {
        return employeeShiftRepository.findById(id)
                .orElseThrow(() -> new EmployeeShiftNotFoundException(
                        "Couldn't find employee shift with id: " + id));
    }

    private Employee findEmployeeById(Long employeeId) {
        restaurantRepository.lockSingleton().orElseThrow();
        return employeeRepository.findById(employeeId).filter(employee -> !employee.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Employee with id " + employeeId + " not found."));
    }

    private void validateTimeRange(LocalTime startsAt, LocalTime endsAt) {
        if (!startsAt.isBefore(endsAt)) {
            throw new TimeConflictException("startsAt must be before endsAt.");
        }
    }

    private void validateNoOverlap(
            Long employeeId,
            LocalDate workDate,
            LocalTime startsAt,
            LocalTime endsAt) {
        if (employeeShiftRepository
                .existsByEmployeeIdAndWorkDateAndStartsAtLessThanAndEndsAtGreaterThan(
                        employeeId, workDate, endsAt, startsAt)) {
            throw new TimeConflictException("Employee already has a shift in this time range.");
        }
    }

    private void validateNoOverlapExceptSelf(EmployeeShift employeeShift) {
        if (employeeShiftRepository.existsOverlappingShiftExceptId(
                employeeShift.getId(),
                employeeShift.getEmployee().getId(),
                employeeShift.getWorkDate(),
                employeeShift.getStartsAt(),
                employeeShift.getEndsAt())) {
            throw new TimeConflictException("Employee already has a shift in this time range.");
        }
    }
}
