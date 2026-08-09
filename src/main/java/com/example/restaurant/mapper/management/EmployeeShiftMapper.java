package com.example.restaurant.mapper.management;

import com.example.restaurant.dto.response.management.EmployeeShiftResponse;
import com.example.restaurant.entity.management.EmployeeShift;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EmployeeShiftMapper {

    public EmployeeShiftResponse toResponse(EmployeeShift employeeShift) {
        return new EmployeeShiftResponse(
                employeeShift.getId(),
                employeeShift.getEmployee().getId(),
                employeeShift.getEmployee().getFirstName(),
                employeeShift.getEmployee().getLastName(),
                employeeShift.getWorkDate(),
                employeeShift.getStartsAt(),
                employeeShift.getEndsAt(),
                employeeShift.getNote(),
                employeeShift.getCreatedAt(),
                employeeShift.getUpdatedAt());
    }

    public List<EmployeeShiftResponse> toResponse(List<EmployeeShift> employeeShifts) {
        List<EmployeeShiftResponse> responses = new ArrayList<>();
        for (EmployeeShift employeeShift : employeeShifts) {
            responses.add(toResponse(employeeShift));
        }

        return responses;
    }
}
