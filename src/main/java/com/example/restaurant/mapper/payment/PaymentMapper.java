package com.example.restaurant.mapper.payment;

import com.example.restaurant.dto.response.payment.PaymentResponse;
import com.example.restaurant.entity.order.Guest;
import com.example.restaurant.entity.order.Order;
import com.example.restaurant.entity.payment.Payment;
import com.example.restaurant.service.payment.PaymentProviderType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public Payment toPayment(Payment payment, Order order, Guest guest,
                             BigDecimal amount, BigDecimal tipAmount,
                             PaymentProviderType providerType) {
        payment.setOrder(order);
        payment.setPayerGuest(guest);
        payment.setAmount(amount);
        payment.setTipAmount(tipAmount);
        payment.setProviderType(providerType);
        payment.setCurrency(order.getCurrency());
        payment.setTipEmployee(order.getEmployee());
        payment.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));

        return payment;
    }

    public PaymentResponse toPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getPayerGuest().getId(),
                payment.getProviderType(),
                payment.getProviderPaymentId(),
                payment.getPaymentPageUrl(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getTipAmount(),
                payment.getCreatedAt(),
                payment.getCompletedAt(),
                payment.getCurrency(),
                payment.getMethod(),
                payment.isReconstructed(),
                payment.getReconciliationIssue(),
                payment.getLastSyncError(),
                payment.getResolutionNote(),
                payment.getRefund() == null ? null : payment.getRefund().getStatus()
        );
    }
}
