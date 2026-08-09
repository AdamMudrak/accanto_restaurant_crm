package com.example.restaurant.service.email;

import com.example.restaurant.entity.management.Restaurant.Language;
import com.example.restaurant.exception.types.notfound.RestaurantNotFoundException;
import com.example.restaurant.repository.management.RestaurantRepository;
import com.example.restaurant.service.email.dtos.EmailTexts;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class EmailTextProvider {
    private static final String ACCOUNT_CONFIRMATION = "account-confirmation";
    private static final String PASSWORD_RESET = "password-reset";
    private static final String CHANGE_EMAIL_CONFIRMATION = "change-email-confirmation";

    private final RestaurantRepository restaurantRepository;
    private final MessageSource emailMessageSource;

    public EmailTextProvider(
            RestaurantRepository restaurantRepository,
            @Qualifier("emailMessageSource") MessageSource emailMessageSource
    ) {
        this.restaurantRepository = restaurantRepository;
        this.emailMessageSource = emailMessageSource;
    }

    public EmailTexts accountConfirmation(
            String firstName,
            String lastName
    ) {
        String restaurantName = getRestaurantName();

        return getEmailTexts(
                ACCOUNT_CONFIRMATION,
                restaurantName,
                firstName,
                lastName
        );
    }

    public EmailTexts passwordReset(
            String firstName,
            String lastName
    ) {
        return getEmailTexts(
                PASSWORD_RESET,
                firstName,
                lastName
        );
    }

    public EmailTexts changeEmailConfirmation(
            String firstName,
            String lastName
    ) {
        String restaurantName = getRestaurantName();

        return getEmailTexts(
                CHANGE_EMAIL_CONFIRMATION,
                restaurantName,
                firstName,
                lastName
        );
    }

    private EmailTexts getEmailTexts(String emailType, Object... args) {
        Locale locale = getLocale();

        return new EmailTexts(
                getMessage(emailType, "subject", locale, args),
                getMessage(emailType, "title", locale, args),
                getMessage(emailType, "greeting", locale, args),
                getMessage(emailType, "description", locale, args),
                getMessage(emailType, "button", locale, args),
                getMessage(emailType, "security-note", locale, args)
        );
    }

    private String getMessage(
            String emailType,
            String field,
            Locale locale,
            Object... args
    ) {
        return emailMessageSource.getMessage(emailType + "." + field, args, locale);
    }

    private Locale getLocale() {
        Language language = restaurantRepository.findSingletonLanguage()
                .orElseThrow(() -> new RestaurantNotFoundException("Couldn't obtain restaurant"))
                .getLanguage();

        if (language == Language.UA) {
            return Locale.forLanguageTag("uk");
        }

        return Locale.ENGLISH;
    }

    private String getRestaurantName() {
        return restaurantRepository.findSingletonName()
                .orElseThrow(() -> new RestaurantNotFoundException("Couldn't obtain restaurant"))
                .getName();
    }
}
