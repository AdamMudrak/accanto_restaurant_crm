package com.example.restaurant.service.payment;

public interface PaymentProvider<PayRequest, PayResponse, GetRequest, GetResponse> {
    PaymentProviderType getType();

    PayResponse pay(PayRequest request);

    GetResponse getStatus(GetRequest request);

    ProviderInvoice inspect(String invoiceId);

    ProviderRefund refund(String invoiceId, String reference, long amount, String currency);

    ProviderRefund inspectRefund(String invoiceId, String reference,
                                 String refundId, long amount, String currency);
}
