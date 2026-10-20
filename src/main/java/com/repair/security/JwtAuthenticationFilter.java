package com.repair.security;

import com.repair.entity.User;
import com.repair.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器：
 * 1. 从 Authorization: Bearer &lt;token&gt; 取 token 并验签；
 * 2. 按 userId 查库，确认用户存在、未逻辑删除且启用（保证「禁用即时生效」）；
 * 3. 通过后写入 SecurityContext（角色映射为 ROLE_XXX 权限）。
 * 任何失败都保持未认证状态，由 EntryPoint 统一返回 401，不在此处输出响应。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    private final UserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith(BEARER_PREFIX)) {
                authenticate(header.substring(BEARER_PREFIX.length()), request);
            }
            chain.doFilter(request, response);
        } catch (Exception e) {
            // 放行链路前清理上下文：SecurityException 等极少见，统一当未认证处理
            SecurityContextHolder.clearContext();
            log.debug("JWT 校验失败: {}", e.getMessage());
            chain.doFilter(request, response);
        } finally {
            // 无状态会话：清理线程上下文，避免 Tomcat 复用线程时串用户
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticate(String token, HttpServletRequest request) {
        Claims claims = jwtUtil.parseToken(token);
        Long userId = Long.valueOf(claims.getSubject());
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            // 用户不存在 / 已逻辑删除 / 已禁用：不写入认证信息
            return;
        }
        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), user.getRole());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                loginUser,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
