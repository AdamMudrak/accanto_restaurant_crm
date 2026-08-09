package com.example.restaurant.service.utils;

import com.example.restaurant.service.security.JwtAbstractUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenGenerator {
    private final JwtAbstractUtil jwtAccessUtil;
    private final JwtAbstractUtil jwtRefreshUtil;

    public String generateAccessToken(String email) {
        return jwtAccessUtil.generateToken(email);
    }

    public String generateRefreshToken(String email) {
        return jwtRefreshUtil.generateToken(email);
    }
}
