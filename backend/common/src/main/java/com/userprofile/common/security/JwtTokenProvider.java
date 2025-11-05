package com.userprofile.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT令牌提供者
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:userprofile-secret-key-change-in-production-min-256-bits-required}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")  // 默认24小时
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration:604800000}")  // 默认7天
    private long jwtRefreshExpirationMs;

    /**
     * 生成访问令牌
     */
    public String generateToken(String username) {
        return generateToken(username, new HashMap<>());
    }

    /**
     * 生成访问令牌（带额外声明）
     */
    public String generateToken(String username, Map<String, Object> claims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(username)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtRefreshExpirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从令牌中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }

    /**
     * 从令牌中获取所有声明
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证令牌
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException ex) {
            log.error("JWT签名无效: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("JWT格式错误: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("JWT已过期: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("不支持的JWT: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT声明为空: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * 检查令牌是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException ex) {
            return true;
        }
    }

    /**
     * 刷新令牌
     */
    public String refreshToken(String token) {
        Claims claims = getClaimsFromToken(token);
        String username = claims.getSubject();

        // 移除特殊声明
        claims.remove("type");
        claims.remove("iat");
        claims.remove("exp");

        return generateToken(username, claims);
    }

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 获取令牌过期时间
     */
    public Date getExpirationFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }

    /**
     * 获取令牌签发时间
     */
    public Date getIssuedAtFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getIssuedAt();
    }
}
