package com.example.restaurant.service.payment;

public interface PaymentProvider<PayRequest, PayResponse, GetRequest, GetResponse> {
    PaymentProviderType getType();

    PayResponse pay(PayRequest request);

    GetResponse getStatus(GetRequest request);
}
