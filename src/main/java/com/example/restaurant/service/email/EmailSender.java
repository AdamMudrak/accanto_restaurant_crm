package com.example.restaurant.service.email;

import com.example.restaurant.exception.types.serviceunavailable.EmailException;
import com.example.restaurant.service.email.dtos.EmailContent;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EmailSender {
    private final String resendApiKey;
    private final String from;

    public EmailSender(@Value("${resend.api.key}") String resendApiKey,
                       @Value("${resend.domain.email}") String from) {
        this.resendApiKey = resendApiKey;
        this.from = from;
    }

    public void send(String to, EmailContent content) {
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(from)
                .to(to)
                .subject(content.subject())
                .html(content.html())
                .build();

        try {
            new Resend(resendApiKey).emails().send(params);
        } catch (ResendException e) {
            throw new EmailException("Couldn't serve email.", e);
        }
    }
}
