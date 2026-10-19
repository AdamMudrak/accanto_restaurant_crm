package com.example.restaurant.mapper.payment;

import com.example.restaurant.entity.order.Order;
import com.example.restaurant.entity.payment.Payment;
import com.example.restaurant.service.payment.stripe.dto.request.StripeCheckoutSessionRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentToStripeMapper {
    private static final BigDecimal CENTS_MULTIPLICAND = BigDecimal.valueOf(100);
    private static final int CENTS_SCALE = 0;
    @Value("${payment.stripe.success-url}")
    private String successUrl;
    @Value("${payment.stripe.cancel-url}")
    private String cancelUrl;

    public StripeCheckoutSessionRequest toStripeCheckoutSessionRequest(
            Payment payment,
            Order order) {
        return new StripeCheckoutSessionRequest(
                payment.getId(),
                toCentsTotalAmount(payment)
                        .setScale(CENTS_SCALE, RoundingMode.UNNECESSARY)
                        .longValueExact(),
                payment.getCurrency().name().toLowerCase(Locale.ROOT),
                "Order payment " + order.getId(),
                successUrl,
                cancelUrl
        );
    }

    private BigDecimal toCentsTotalAmount(Payment payment) {
        return payment.getAmount().add(payment.getTipAmount()).multiply(CENTS_MULTIPLICAND);
    }
}
