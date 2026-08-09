package com.example.restaurant.service.security;

import static com.example.restaurant.service.utils.PropertyReader.getTimeFromPropertiesMillis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtAccessUtil extends JwtAbstractUtil {
    public JwtAccessUtil(@Value("${security.jwt.access.secret.key}") String secretString,
                         @Value("${security.jwt.access.expiration}") String expiration) {
        super(secretString, getTimeFromPropertiesMillis(expiration), TokenType.ACCESS_TOKEN);
    }
}
