package com.example.restaurant.service.security;

import com.example.restaurant.repository.user.EmployeeRepository;
import com.example.restaurant.service.utils.CookieExtractor;
import com.example.restaurant.service.utils.CookieRemover;
import com.example.restaurant.service.utils.CookieSetter;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final UserDetailsService userDetailsService;
    private final JwtAbstractUtil jwtAccessUtil;
    private final JwtAbstractUtil jwtRefreshUtil;
    private final EmployeeRepository employeeRepository;
    private final CookieRemover cookieRemover;
    private final CookieSetter cookieSetter;
    @Value("${security.jwt.access.expiration}")
    private String accessExpiration;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String accessToken = CookieExtractor.extractCookieValueByName(request, "accessToken");
        String refreshToken = CookieExtractor.extractCookieValueByName(request, "refreshToken");

        boolean isAccessValid = isAccessValid(accessToken, response);
        boolean isRefreshValid = isRefreshValid(refreshToken, response);

        try {
            if (!isAccessValid && isRefreshValid) {
                accessToken = refreshAccessToken(response, refreshToken);
            }
            if (isAccessValid(accessToken, response)) {
                setAuthentication(accessToken, response);
            }

            filterChain.doFilter(request, response);

        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean isAccessValid(String accessToken, HttpServletResponse response) {
        try {
            return jwtAccessUtil.isValidToken(accessToken);
        } catch (JwtException e) {
            cookieRemover.removeAccessTokenCookie(response);
            return false;
        }
    }

    private boolean isRefreshValid(String refreshToken, HttpServletResponse response) {
        try {
            return jwtRefreshUtil.isValidToken(refreshToken);
        } catch (JwtException e) {
            cookieRemover.removeRefreshTokenCookie(response);
            return false;
        }
    }

    private String refreshAccessToken(HttpServletResponse response,
                                      String refreshToken) {
        String username = jwtRefreshUtil.getUsername(refreshToken);
        String accessToken = jwtAccessUtil.generateToken(username);
        cookieSetter.addCookie("accessToken", accessToken, accessExpiration, response);
        return accessToken;
    }

    private void setAuthentication(String accessToken, HttpServletResponse response) {
        String username = jwtAccessUtil.getUsername(accessToken);
        if (employeeRepository.existsByEmailAndIsDeletedTrue(username)) {
            cookieRemover.removeAllTokenCookies(response);
            SecurityContextHolder.clearContext();
        } else {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }
}
