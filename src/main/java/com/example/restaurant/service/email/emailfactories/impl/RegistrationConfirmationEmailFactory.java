package com.example.restaurant.service.email.emailfactories.impl;

import com.example.restaurant.service.email.EmailTextProvider;
import com.example.restaurant.service.email.dtos.EmailContent;
import com.example.restaurant.service.email.dtos.EmailTexts;
import com.example.restaurant.service.email.emailfactories.EmailFactory;
import com.example.restaurant.service.email.template.EmailTemplateLoader;
import com.example.restaurant.service.email.template.EmailTemplateRenderer;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RegistrationConfirmationEmailFactory implements EmailFactory {

    private static final String TEMPLATE_PATH =
            "templates/email-action.html";

    private final EmailTemplateLoader templateLoader;
    private final EmailTemplateRenderer templateRenderer;
    private final EmailTextProvider textProvider;

    public RegistrationConfirmationEmailFactory(
            EmailTemplateLoader templateLoader,
            EmailTemplateRenderer templateRenderer,
            EmailTextProvider textProvider
    ) {
        this.templateLoader = templateLoader;
        this.templateRenderer = templateRenderer;
        this.textProvider = textProvider;
    }

    @Override
    public EmailContent build(Map<String, String> emailParams) {
        EmailTexts texts = textProvider.accountConfirmation(
                emailParams.get("firstName"),
                emailParams.get("lastName")
        );

        String html = templateRenderer.render(
                templateLoader.load(TEMPLATE_PATH),
                Map.of(
                        "title", texts.title(),
                        "greeting", texts.greeting(),
                        "description", texts.description(),
                        "button", texts.button(),
                        "securityNote", texts.securityNote(),
                        "actionUrl", emailParams.get("confirmationPath")
                )
        );

        return new EmailContent(
                texts.subject(),
                html
        );
    }
}
