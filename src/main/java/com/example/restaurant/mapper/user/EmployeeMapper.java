package com.example.restaurant.mapper.user;

import com.example.restaurant.dto.request.security.RegisterEmployeeRequest;
import com.example.restaurant.dto.response.user.EmployeeResponse;
import com.example.restaurant.entity.user.Employee;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    public Employee toEmployee(RegisterEmployeeRequest request) {
        Employee employee = new Employee();
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setEmail(request.email());
        employee.setPhone(request.phone());
        LocalDateTime now = LocalDateTime.now();
        employee.setCreatedAt(now);
        employee.setUpdatedAt(now);
        return employee;
    }

    public EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.isEmailConfirmed(),
                getProfilePictureIdSafely(employee),
                employee.getPhone(),
                employee.isDeleted()
        );
    }

    public List<EmployeeResponse> toResponse(List<Employee> employees) {
        List<EmployeeResponse> responses = new ArrayList<>();
        for (Employee employee : employees) {
            responses.add(toResponse(employee));
        }

        return responses;
    }

    private Long getProfilePictureIdSafely(Employee employee) {
        if (employee.getProfilePicture() != null) {
            return employee.getProfilePicture().getId();
        }
        return null;
    }
}
