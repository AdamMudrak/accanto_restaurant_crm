package com.example.restaurant.service.payment;

import com.example.restaurant.entity.order.Order;
import com.example.restaurant.entity.order.OrderItem;
import com.example.restaurant.entity.payment.Payment;
import com.example.restaurant.entity.payment.PaymentRefund;
import com.example.restaurant.exception.types.conflict.ItemIllegalStateException;
import com.example.restaurant.repository.order.OrderRepository;
import com.example.restaurant.repository.payment.PaymentRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class PaymentSettlementService {
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public Payment lockByProviderId(String providerId) {
        Long orderId = paymentRepository.findOrderIdByProviderId(providerId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found"));
        orderRepository.lockById(orderId).orElseThrow();
        return paymentRepository.findByProviderPaymentId(providerId).orElseThrow();
    }

    public Payment lockById(Long id) {
        Long orderId = paymentRepository.findOrderIdByPaymentId(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found"));
        orderRepository.lockById(orderId).orElseThrow();
        return paymentRepository.findById(id).orElseThrow();
    }

    public void assertNoPendingPayment(OrderItem item) {
        Payment payment = item.getPayment();
        if (payment != null && payment.getStatus() == Payment.PaymentStatus.PENDING) {
            throw new ItemIllegalStateException("Online payment " + payment.getId()
                    + " is unresolved. Confirm its final provider status before changing payment.");
        }
    }

    public void assertNoPendingPayments(Order order) {
        if (!paymentRepository.findAllByOrderIdAndStatus(
                order.getId(), Payment.PaymentStatus.PENDING).isEmpty()) {
            throw new ItemIllegalStateException("Order has an unresolved online payment.");
        }
    }

    public void applyStatus(Payment payment, Payment.PaymentStatus status,
                            OffsetDateTime providerTime) {
        if (providerTime != null && payment.getProviderModifiedAt() != null
                && providerTime.isBefore(payment.getProviderModifiedAt())) {
            return;
        }
        if (payment.getStatus() == Payment.PaymentStatus.SUCCEEDED) {
            if ((status == Payment.PaymentStatus.FAILED
                    || status == Payment.PaymentStatus.EXPIRED)
                    && (payment.getRefund() == null || payment.getRefund().getStatus()
                    == PaymentRefund.Status.FAILED)) {
                payment.setReconciliationIssue("Provider reported " + status
                        + " after a successful payment; manual reconciliation required.");
            }
            return;
        }
        if (payment.getStatus() != Payment.PaymentStatus.PENDING
                && status == Payment.PaymentStatus.PENDING) {
            return;
        }
        payment.setStatus(status);
        if (providerTime != null) {
            payment.setProviderModifiedAt(providerTime);
        }
        if (status != Payment.PaymentStatus.SUCCEEDED) {
            return;
        }
        payment.setCompletedAt(providerTime == null ? LocalDateTime.now(ZoneOffset.UTC)
                : providerTime.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime());
        if (payment.getOrderItems().isEmpty()) {
            payment.setReconciliationIssue("Successful payment has no associated items.");
            return;
        }
        for (OrderItem item : payment.getOrderItems()) {
            if (item.getPayment() == null
                    || !Objects.equals(item.getPayment().getId(), payment.getId())
                    || (item.getStatus() != OrderItem.OrderItemStatus.CONFIRMED
                    && item.getStatus() != OrderItem.OrderItemStatus.SERVED)) {
                payment.setReconciliationIssue(
                        "Successful provider payment conflicts with order items; review required.");
                return;
            }
        }
        payment.getOrderItems().forEach(item ->
                item.setStatus(OrderItem.OrderItemStatus.PAID_CARD));
    }
}
