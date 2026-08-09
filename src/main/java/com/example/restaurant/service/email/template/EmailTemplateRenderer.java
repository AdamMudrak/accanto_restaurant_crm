package com.example.restaurant.service.email.template;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class EmailTemplateRenderer {

    public String render(
            String template,
            Map<String, String> values
    ) {
        String result = template;

        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace(
                    "{{" + entry.getKey() + "}}",
                    entry.getValue()
            );
        }

        return result;
    }
}
