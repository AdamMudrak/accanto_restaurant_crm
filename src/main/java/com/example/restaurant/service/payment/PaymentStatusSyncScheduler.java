package com.example.restaurant.service.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentStatusSyncScheduler {
    private final PaymentService paymentService;
    private final PaymentOperationsService operations;

    @Scheduled(
            fixedDelayString = "${payment.sync.delay-ms:300000}",
            initialDelayString = "${payment.sync.initial-delay-ms:300000}")
    public void syncPendingPayments() {
        paymentService.syncPendingPayments();
        operations.syncPendingRefunds();
    }
}
