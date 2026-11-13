package com.example.restaurant.mapper.payment;

import com.example.restaurant.dto.response.analytics.CashlessTipPaymentResponse;
import com.example.restaurant.entity.order.Order;
import com.example.restaurant.entity.payment.Payment;
import com.example.restaurant.entity.user.Employee;
import org.springframework.stereotype.Component;

@Component
public class CashlessTipPaymentMapper {

    public CashlessTipPaymentResponse toResponse(Payment payment) {
        Order order = payment.getOrder();
        Employee employee = payment.getTipEmployee();

        return new CashlessTipPaymentResponse(
                payment.getId(),
                order.getId(),
                getEmployeeId(employee),
                getEmployeeFirstName(employee),
                getEmployeeLastName(employee),
                payment.getPayerGuest().getId(),
                payment.getProviderType(),
                payment.getTipAmount(),
                payment.isTipPaid(),
                payment.getTipPaidAt(),
                payment.getCompletedAt(), payment.getCurrency());
    }

    private Long getEmployeeId(Employee employee) {
        return employee == null ? null : employee.getId();
    }

    private String getEmployeeFirstName(Employee employee) {
        return employee == null ? null : employee.getFirstName();
    }

    private String getEmployeeLastName(Employee employee) {
        return employee == null ? null : employee.getLastName();
    }
}
