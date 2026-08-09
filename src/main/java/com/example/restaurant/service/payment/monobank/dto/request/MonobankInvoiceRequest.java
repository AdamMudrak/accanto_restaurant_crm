package com.example.restaurant.service.payment.monobank.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MonobankInvoiceRequest(
        Long amount,
        Integer ccy,
        MerchantPaymInfo merchantPaymInfo,
        String redirectUrl,
        String webHookUrl,
        Long validity,
        DisplayType displayType,
        Boolean withAppUrl,
        PaymentType paymentType
) {
    public record MerchantPaymInfo(
            String reference,
            String destination
    ) {}

    public enum PaymentType {
        debit
    }

    public enum DisplayType {
        iframe
    }
}
