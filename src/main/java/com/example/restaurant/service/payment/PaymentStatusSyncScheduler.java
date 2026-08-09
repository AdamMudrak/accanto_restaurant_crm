package com.example.restaurant.service.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentStatusSyncScheduler {
    private static final long PAYMENT_STATUS_SYNC_DELAY_MILLIS = 300000L;
    private final PaymentService paymentService;

    @Scheduled(
            fixedDelay = PAYMENT_STATUS_SYNC_DELAY_MILLIS,
            initialDelay = PAYMENT_STATUS_SYNC_DELAY_MILLIS)
    public void syncPendingPayments() {
        paymentService.syncPendingPayments();
    }
}
