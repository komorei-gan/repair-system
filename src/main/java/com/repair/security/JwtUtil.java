package com.repair.security;

import com.repair.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 工具：HS256 签发与解析（jjwt 0.12.x API）。
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(jwtProperties.secretBytes());
    }

    /**
     * 签发 token：subject 存 userId，claims 存 username / role。
     */
    public String generateToken(Long userId, String username, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtProperties.getExpireSeconds() * 1000))
                .signWith(key())
                .compact();
    }

    /**
     * 解析并校验 token，非法/过期/被篡改时抛 io.jsonwebtoken.JwtException。
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
