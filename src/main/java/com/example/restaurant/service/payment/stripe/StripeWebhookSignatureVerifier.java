package com.example.restaurant.service.payment.stripe;

import com.example.restaurant.exception.types.internalserver.StripePaymentProviderException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class StripeWebhookSignatureVerifier {
    private static final String SIGNATURE_PREFIX = "v1=";
    private static final String TIMESTAMP_PREFIX = "t=";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long TOLERANCE_SECONDS = 300L;

    public boolean verify(String rawBody, String signatureHeader, String endpointSecret) {
        if (rawBody == null
                || signatureHeader == null
                || endpointSecret == null
                || endpointSecret.isBlank()) {
            return false;
        }

        Long timestamp = extractTimestamp(signatureHeader);
        if (timestamp == null || isTimestampTooOld(timestamp)) {
            return false;
        }

        String expectedSignature = buildSignature(timestamp, rawBody, endpointSecret);
        for (String part : signatureHeader.split(",")) {
            String trimmedPart = part.trim();
            if (trimmedPart.startsWith(SIGNATURE_PREFIX)
                    && secureEquals(expectedSignature, trimmedPart.substring(3))) {
                return true;
            }
        }

        return false;
    }

    private Long extractTimestamp(String signatureHeader) {
        for (String part : signatureHeader.split(",")) {
            String trimmedPart = part.trim();
            if (trimmedPart.startsWith(TIMESTAMP_PREFIX)) {
                try {
                    return Long.parseLong(trimmedPart.substring(2));
                } catch (NumberFormatException exception) {
                    return null;
                }
            }
        }

        return null;
    }

    private boolean isTimestampTooOld(Long timestamp) {
        long now = Clock.systemUTC().instant().getEpochSecond();
        return Math.abs(now - timestamp) > TOLERANCE_SECONDS;
    }

    private String buildSignature(Long timestamp, String rawBody, String endpointSecret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(
                    endpointSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKey);
            byte[] result = mac.doFinal((timestamp + "." + rawBody)
                    .getBytes(StandardCharsets.UTF_8));

            return toHex(result);
        } catch (Exception exception) {
            throw new StripePaymentProviderException(
                    "Failed to verify stripe webhook signature: " + exception.getMessage());
        }
    }

    private boolean secureEquals(String expected, String actual) {
        return java.security.MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte currentByte : bytes) {
            builder.append(String.format("%02x", currentByte));
        }

        return builder.toString();
    }
}
