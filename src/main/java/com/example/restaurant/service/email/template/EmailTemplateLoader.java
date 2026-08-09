package com.example.restaurant.service.email.template;

import com.example.restaurant.exception.types.internalserver.EmailTemplateException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class EmailTemplateLoader {

    public String load(String templatePath) {
        ClassPathResource resource =
                new ClassPathResource(templatePath);

        try (InputStream inputStream = resource.getInputStream()) {
            return new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        } catch (IOException exception) {
            throw new EmailTemplateException(
                    "Failed to load email template: " + templatePath,
                    exception
            );
        }
    }
}
