package com.example.restaurant.service.payment.monobank.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MonobankStatusResponse(
        String invoiceId,
        Status status,
        String failureReason,
        String errCode,
        String errText,
        Long amount,
        Integer ccy,
        Long finalAmount,
        String reference,
        String destination,
        OffsetDateTime createdDate,
        OffsetDateTime modifiedDate
) {
    public enum Status {
        created,
        processing,
        hold,
        success,
        failure,
        reversed,
        expired
    }
}
