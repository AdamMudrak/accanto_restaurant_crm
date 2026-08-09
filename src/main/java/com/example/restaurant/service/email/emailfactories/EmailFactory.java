package com.example.restaurant.service.email.emailfactories;

import com.example.restaurant.service.email.dtos.EmailContent;
import java.util.Map;

public interface EmailFactory {
    EmailContent build(Map<String, String> emailParams);
}
