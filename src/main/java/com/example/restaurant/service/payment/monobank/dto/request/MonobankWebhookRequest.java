package com.example.restaurant.service.payment.monobank.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MonobankWebhookRequest(
        String invoiceId,
        Status status,
        String failureReason,
        String errCode,
        String errText,
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
