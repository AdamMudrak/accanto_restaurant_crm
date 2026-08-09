package com.example.restaurant.service.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;

public class CookieExtractor {
    public static String extractCookieValueByName(HttpServletRequest request, String cookieName) {
        Cookie cookie = null;
        if (request != null
                && request.getCookies() != null
                && request.getCookies().length > 0) {
            cookie = Arrays.stream(request.getCookies())
                    .filter(cookieObject -> cookieObject.getName().equals(cookieName))
                    .findFirst().orElse(null);
        }
        return cookie != null ? cookie.getValue() : null;
    }
}
