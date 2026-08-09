package com.example.restaurant.service.payment;

import com.example.restaurant.entity.order.OrderItem;
import com.example.restaurant.entity.payment.Payment;
import org.springframework.stereotype.Service;

@Service
public class PaymentOrderItemService {
    public void markItemsAsPaid(Payment payment) {
        payment.getOrderItems().forEach(orderItem ->
                orderItem.setStatus(OrderItem.OrderItemStatus.PAID_CARD));
    }
}
