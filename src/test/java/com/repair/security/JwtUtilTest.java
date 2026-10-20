package com.repair.security;

import com.repair.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * JWT 工具单元测试（无需 Spring 上下文与数据库）。
 */
class JwtUtilTest {

    private static final String SECRET = "unit-test-secret-key-0123456789-0123456789-0123456789";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setExpireSeconds(3600L);
        jwtUtil = new JwtUtil(properties);
    }

    @Test
    void generateAndParse() {
        String token = jwtUtil.generateToken(1L, "zhangsan", "REPORTER");
        Claims claims = jwtUtil.parseToken(token);
        assertEquals("1", claims.getSubject());
        assertEquals("zhangsan", claims.get("username", String.class));
        assertEquals("REPORTER", claims.get("role", String.class));
    }

    @Test
    void expiredTokenThrows() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setExpireSeconds(-10L); // 立即过期
        JwtUtil expiredUtil = new JwtUtil(properties);
        String token = expiredUtil.generateToken(1L, "zhangsan", "REPORTER");
        assertThrows(JwtException.class, () -> jwtUtil.parseToken(token));
    }

    @Test
    void tamperedTokenThrows() {
        String token = jwtUtil.generateToken(1L, "zhangsan", "REPORTER");
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");
        assertThrows(JwtException.class, () -> jwtUtil.parseToken(tampered));
    }
}
