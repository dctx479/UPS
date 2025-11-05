package com.userprofile.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT认证过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 从请求中提取JWT令牌
            String token = extractTokenFromRequest(request);

            // 验证令牌并设置认证信息
            if (token != null && jwtTokenProvider.validateToken(token)) {
                String username = jwtTokenProvider.getUsernameFromToken(token);

                // 创建认证对象
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                        );

                // 设置到安全上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("已为用户 '{}' 设置认证信息", username);
            }
        } catch (Exception ex) {
            log.error("无法设置用户认证信息", ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求中提取JWT令牌
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        // 从Authorization header中提取
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 从Cookie中提取（可选）
        // Cookie[] cookies = request.getCookies();
        // if (cookies != null) {
        //     for (Cookie cookie : cookies) {
        //         if ("access_token".equals(cookie.getName())) {
        //             return cookie.getValue();
        //         }
        //     }
        // }

        return null;
    }

    /**
     * 判断是否需要过滤该请求
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // 对于公开端点不进行JWT验证
        return path.startsWith("/api/auth/") ||
               path.startsWith("/actuator/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/api-docs/") ||
               path.equals("/error");
    }
}
