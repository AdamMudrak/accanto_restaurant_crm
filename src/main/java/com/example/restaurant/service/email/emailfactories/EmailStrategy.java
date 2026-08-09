package com.example.restaurant.service.email.emailfactories;

import com.example.restaurant.service.email.emailfactories.impl.ChangeEmailFactory;
import com.example.restaurant.service.email.emailfactories.impl.PasswordResetEmailFactory;
import com.example.restaurant.service.email.emailfactories.impl.RegistrationConfirmationEmailFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailStrategy {
    private final RegistrationConfirmationEmailFactory registrationConfirmationEmailFactory;
    private final PasswordResetEmailFactory passwordResetEmailFactory;
    private final ChangeEmailFactory changeEmailFactory;

    public enum EmailFactories {
        REGISTRATION_CONFIRMATION_EMAIL,
        RESET_PASSWORD_EMAIL,
        CHANGE_EMAIL_CONFIRMATION_EMAIL
    }

    public EmailFactory getEmailFactory(EmailFactories factories) {
        return switch (factories) {
            case REGISTRATION_CONFIRMATION_EMAIL -> registrationConfirmationEmailFactory;
            case RESET_PASSWORD_EMAIL -> passwordResetEmailFactory;
            case CHANGE_EMAIL_CONFIRMATION_EMAIL -> changeEmailFactory;
        };
    }
}
