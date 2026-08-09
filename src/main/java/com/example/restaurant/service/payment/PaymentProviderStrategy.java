package com.example.restaurant.service.payment;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentProviderStrategy {
    private final List<PaymentProvider> paymentProviders;

    @SuppressWarnings("unchecked")
    public <A, B, C, D> PaymentProvider<A, B, C, D> getPaymentProvider(
                    PaymentProviderType paymentProviderType) {
        return (PaymentProvider<A, B, C, D>) paymentProviders.stream()
                .filter(paymentProvider -> paymentProvider.getType() == paymentProviderType)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Payment provider " + paymentProviderType + " is not configured"));
    }
}
