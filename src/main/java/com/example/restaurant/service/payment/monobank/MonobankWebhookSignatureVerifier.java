package com.example.restaurant.service.payment.monobank;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class MonobankWebhookSignatureVerifier {
    private static final String SIGNATURE_ALGORITHM = "SHA256withECDSA";
    private static final String KEY_ALGORITHM = "EC";
    private static final String PUBLIC_KEY_BEGIN = "-----BEGIN PUBLIC KEY-----";
    private static final String PUBLIC_KEY_END = "-----END PUBLIC KEY-----";

    public boolean verify(String rawBody, String signBase64, String publicKeyBase64) {
        try {
            byte[] signatureBytes = Base64.getDecoder().decode(signBase64);
            PublicKey publicKey = parsePublicKey(publicKeyBase64);

            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(rawBody.getBytes(StandardCharsets.UTF_8));

            return signature.verify(signatureBytes);
        } catch (Exception exception) {
            return false;
        }
    }

    private PublicKey parsePublicKey(String publicKeyBase64) throws Exception {
        String publicKeyPem = new String(
                Base64.getDecoder().decode(publicKeyBase64),
                StandardCharsets.UTF_8);
        String publicKeyContent = publicKeyPem
                .replace(PUBLIC_KEY_BEGIN, "")
                .replace(PUBLIC_KEY_END, "")
                .replaceAll("\\s", "");

        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyContent);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);

        return KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(keySpec);
    }
}
