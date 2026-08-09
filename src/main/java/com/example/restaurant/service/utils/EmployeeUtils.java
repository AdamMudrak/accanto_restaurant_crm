package com.example.restaurant.service.utils;

import com.example.restaurant.entity.token.UuidToken;
import com.example.restaurant.entity.user.Employee;
import com.example.restaurant.entity.user.Role;
import com.example.restaurant.exception.types.conflict.EmailAlreadyConfirmedException;
import com.example.restaurant.exception.types.forbidden.DeletedEmployeeException;
import com.example.restaurant.exception.types.gone.ExpiredTokenException;
import com.example.restaurant.exception.types.unauthorized.InvalidTokenException;
import com.example.restaurant.repository.token.ConfirmationTokenRepository;
import com.example.restaurant.repository.user.EmployeeRepository;
import com.example.restaurant.repository.user.RoleRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmployeeUtils {
    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final ConfirmationTokenRepository confirmationTokenRepository;

    public void throwIfEmailConfirmed(Employee employee) {
        if (employee.isEmailConfirmed()) {
            throw new EmailAlreadyConfirmedException("Email already confirmed.");
        }
    }

    public void throwIfEmployeeDeleted(Employee employee) {
        if (employee.isDeleted()) {
            throw new DeletedEmployeeException(
                    "Employee with email " + employee.getEmail() + " has been deleted.");
        }
    }

    public Employee getEmployeeByEmail(String email) {
        return employeeRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Could not find employee for email: " + email + "."));
    }

    public Employee getEmployeeById(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Could not find employee for id: " + employeeId + "."));
    }

    public Role getRoleByBoolean(Boolean isAdmin) {
        if (isAdmin) {
            return roleRepository.findByName(Role.RoleType.ROLE_ADMIN_EMPLOYEE);
        } else {
            return roleRepository.findByName(Role.RoleType.ROLE_EMPLOYEE);
        }
    }

    public void validateAndDeleteUuidToken(String uuid, Long employeeId,
                                           UuidToken.TokenType tokenType) {
        UuidToken uuidToken = confirmationTokenRepository.findByUuidAndEmployeeId(
                        uuid, employeeId)
                .orElseThrow(() -> new InvalidTokenException(
                        "Could not find confirmation token for uuid: " + uuid
                                + " and employeeId: " + employeeId + "."));

        if (uuidToken.getTokenType() != tokenType) {
            throw new InvalidTokenException("This token is not of the type expected.");
        }

        if (uuidToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ExpiredTokenException("This token is already expired.");
        }

        confirmationTokenRepository.deleteAllByEmployeeId(employeeId);
    }
}
