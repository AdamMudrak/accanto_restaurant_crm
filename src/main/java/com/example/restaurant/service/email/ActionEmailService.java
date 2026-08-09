package com.example.restaurant.service.email;

import com.example.restaurant.entity.token.UuidToken;
import com.example.restaurant.repository.token.ConfirmationTokenRepository;
import com.example.restaurant.service.email.dtos.EmailContent;
import com.example.restaurant.service.email.emailfactories.EmailFactory;
import com.example.restaurant.service.utils.PropertyReader;
import com.example.restaurant.service.utils.UrlBuilder;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Component
@RequiredArgsConstructor
public class ActionEmailService {
    @Value("${front.base.url}")
    private String frontBaseUrl;
    private final EmailSender emailSender;
    private final ConfirmationTokenRepository confirmationTokenRepository;

    public void sendConfirmationEmail(String toWhom,
                               String firstName,
                               String lastName,
                               Long employeeId,
                               String expiration,
                               String confirmationPath,
                               EmailFactory emailFactory,
                               UuidToken.TokenType tokenType) {
        String uuid = saveAndGetUuidToken(expiration, employeeId, tokenType);

        String fullConfirmationUrl =
                UrlBuilder.buildUrl(frontBaseUrl, confirmationPath, getMapForUrl(employeeId, uuid));

        EmailContent emailContent = emailFactory.build(
                getMapForConfirmationEmail(
                        firstName,
                        lastName,
                        fullConfirmationUrl));
        emailSender.send(toWhom, emailContent);
    }

    private MultiValueMap<String, String> getMapForUrl(Long employeeId, String uuid) {
        MultiValueMap<String, String> urlParams = new LinkedMultiValueMap<>();
        urlParams.add("employee_id", String.valueOf(employeeId));
        urlParams.add("uuid", uuid);

        return urlParams;
    }

    private Map<String, String> getMapForConfirmationEmail(String firstName,
                                                           String lastName,
                                                           String confirmationPath) {
        return Map.of(
                "firstName", firstName,
                "lastName", lastName,
                "confirmationPath", confirmationPath
        );
    }

    private String saveAndGetUuidToken(String expiration, Long employeeId,
                                       UuidToken.TokenType tokenType) {
        UuidToken uuidToken = new UuidToken();
        uuidToken.setTokenType(tokenType);
        uuidToken.setUuid(UUID.randomUUID().toString());
        uuidToken.setEmployeeId(employeeId);
        LocalDateTime now = LocalDateTime.now();
        uuidToken.setCreatedAt(now);
        uuidToken.setExpiresAt(now.plusSeconds(
                PropertyReader.getTimeFromPropertiesSeconds(expiration)));

        confirmationTokenRepository.save(uuidToken);

        return uuidToken.getUuid();
    }
}
