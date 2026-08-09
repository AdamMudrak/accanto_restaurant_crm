package com.example.restaurant.mapper.payment;

import com.example.restaurant.entity.order.Order;
import com.example.restaurant.entity.payment.Payment;
import com.example.restaurant.service.payment.monobank.dto.request.MonobankInvoiceRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentToMonobankMapper {
    private static final BigDecimal COINS_MULTIPLICAND = BigDecimal.valueOf(100);
    private static final Integer UAH_CCY = 980;
    private static final int COINS_SCALE = 0;
    private static final Long PAYMENT_VALIDITY = 900L;
    @Value("${payment.monobank.redirect-url}")
    private String redirectUrl;
    @Value("${payment.monobank.webhook-url}")
    private String webhookUrl;

    public MonobankInvoiceRequest toMonobankInvoiceRequest(Payment payment, Order order) {
        return new MonobankInvoiceRequest(
                toCoinsTotalAmount(payment)
                        .setScale(COINS_SCALE, RoundingMode.UNNECESSARY)
                        .longValueExact(),
                UAH_CCY,
                new MonobankInvoiceRequest.MerchantPaymInfo(
                        String.valueOf(payment.getId()),
                        destinationBuilder(payment, order)
                ),
                redirectUrl,
                webhookUrl,
                PAYMENT_VALIDITY,
                null,//todo
                null,//todo
                MonobankInvoiceRequest.PaymentType.debit
        );
    }

    private BigDecimal toCoinsTotalAmount(Payment payment) {
        return payment.getAmount().add(payment.getTipAmount()).multiply(COINS_MULTIPLICAND);
    }

    private String destinationBuilder(Payment payment, Order order) {
        StringBuilder builder = new StringBuilder();
        builder.append("Оплата за замовлення ")
                .append(order.getId())
                .append(".");

        if (payment.getTipAmount() != null
                && payment.getTipAmount().compareTo(BigDecimal.ZERO) > 0) {
            builder.append(" З них чайові: ")
                    .append(payment.getTipAmount())
                    .append(" для співробітника за id: ")
                    .append(order.getEmployee().getId());
        }

        return builder.toString();
    }
}
