package com.example.restaurant.service.payment;

import com.example.restaurant.entity.order.OrderItem;
import com.example.restaurant.entity.payment.Payment;
import com.example.restaurant.entity.user.Employee;
import com.example.restaurant.exception.types.conflict.ItemIllegalStateException;
import com.example.restaurant.repository.payment.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class CashPaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentSettlementService settlementService;

    public void record(List<OrderItem> items, Employee employee) {
        List<OrderItem> unpaid = items.stream()
                .filter(item -> item.getStatus() != OrderItem.OrderItemStatus.PAID_CASH
                        && item.getStatus() != OrderItem.OrderItemStatus.PAID_CARD
                        && item.getStatus() != OrderItem.OrderItemStatus.CANCELED).toList();
        if (unpaid.isEmpty()) {
            return;
        }
        unpaid.forEach(settlementService::assertNoPendingPayment);
        if (unpaid.getFirst().getOrder().getCurrency() == null) {
            throw new ItemIllegalStateException(
                    "Order currency requires historical reconciliation.");
        }
        Payment payment = new Payment();
        payment.setOrder(unpaid.getFirst().getOrder());
        payment.setPayerGuest(unpaid.getFirst().getGuest());
        payment.setCurrency(payment.getOrder().getCurrency());
        payment.setMethod(Payment.PaymentMethod.CASH);
        payment.setRecordedBy(employee);
        payment.setTipEmployee(payment.getOrder().getEmployee());
        payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
        payment.setAmount(unpaid.stream().map(item -> item.getItemPriceSnapshot()
                .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        payment.setTipAmount(BigDecimal.ZERO);
        payment.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        payment.setCompletedAt(payment.getCreatedAt());
        payment.setReviewToken(UUID.randomUUID().toString());
        payment.getOrderItems().addAll(unpaid);
        paymentRepository.save(payment);
        unpaid.forEach(item -> {
            item.setPayment(payment);
            item.setStatus(OrderItem.OrderItemStatus.PAID_CASH);
        });
    }
}
