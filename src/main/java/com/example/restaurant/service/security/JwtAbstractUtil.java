package com.example.restaurant.service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;
import javax.crypto.SecretKey;

public abstract class JwtAbstractUtil {
    protected final SecretKey secret;
    protected final long expiration;
    protected final TokenType tokenType;

    public JwtAbstractUtil(String secretString, long expiration, TokenType tokenType) {
        secret = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
        this.tokenType = tokenType;
    }

    public String generateToken(String username) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .subject(username)
                .claim("tokenType", tokenType.name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiration))
                .signWith(secret)
                .compact();
    }

    public boolean isValidToken(String token) {
        if (token == null) {
            return false;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secret)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String actualTokenType = claims.get("tokenType", String.class);

            return tokenType.name().equals(actualTokenType);

        } catch (ExpiredJwtException e) {
            return false;

        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtException("Invalid JWT token", e);
        }
    }

    public String getUsername(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    private <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parser()
                .verifyWith(secret)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }

    public enum TokenType {
        ACCESS_TOKEN,
        REFRESH_TOKEN
    }
}
