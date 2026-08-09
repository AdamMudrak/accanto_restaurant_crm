package com.example.restaurant.service.utils;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CookieRemover {
    @Value("${security.cookie.local}")
    private boolean localCookie;

    public void removeAllTokenCookies(HttpServletResponse response) {
        removeAccessTokenCookie(response);
        removeRefreshTokenCookie(response);
    }

    public void removeAccessTokenCookie(HttpServletResponse response) {
        String accessCookie = "accessToken="
                + "; Path=/"
                + "; HttpOnly"
                + getSecureCookieAttribute()
                + getSameSiteCookieAttribute()
                + "; Max-Age=0";

        response.addHeader("Set-Cookie", accessCookie);
    }

    public void removeRefreshTokenCookie(HttpServletResponse response) {
        String refreshCookie = "refreshToken="
                + "; Path=/"
                + "; HttpOnly"
                + getSecureCookieAttribute()
                + getSameSiteCookieAttribute()
                + "; Max-Age=0";

        response.addHeader("Set-Cookie", refreshCookie);
    }

    private String getSecureCookieAttribute() {
        return localCookie ? "" : "; Secure";
    }

    private String getSameSiteCookieAttribute() {
        return localCookie ? "" : "; SameSite=Strict";
    }
}
