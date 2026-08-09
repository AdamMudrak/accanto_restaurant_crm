package com.example.restaurant.service.security;

import static com.example.restaurant.service.utils.PropertyReader.getTimeFromPropertiesMillis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtRefreshUtil extends JwtAbstractUtil {
    public JwtRefreshUtil(@Value("${security.jwt.refresh.secret.key}") String secretString,
                          @Value("${security.jwt.refresh.expiration}") String expiration) {
        super(secretString, getTimeFromPropertiesMillis(expiration), TokenType.REFRESH_TOKEN);
    }
}
