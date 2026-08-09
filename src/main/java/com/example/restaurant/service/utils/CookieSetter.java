package com.example.restaurant.service.utils;

import static com.example.restaurant.service.utils.PropertyReader.getTimeFromPropertiesSeconds;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CookieSetter {
    @Value("${security.cookie.local}")
    private boolean localCookie;

    public void addCookie(String cookieName, String cookieValue,
                          String expiration, HttpServletResponse response) {
        String cookie = cookieName + "=" + cookieValue
                + "; Path=/"
                + "; HttpOnly"
                + getSecureCookieAttribute()
                + getSameSiteCookieAttribute()
                + "; Max-Age=" + getTimeFromPropertiesSeconds(expiration);
        response.addHeader("Set-Cookie", cookie);
    }

    private String getSecureCookieAttribute() {
        return localCookie ? "" : "; Secure";
    }

    private String getSameSiteCookieAttribute() {
        return localCookie ? "" : "; SameSite=Strict";
    }
}
