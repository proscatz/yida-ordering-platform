package com.yida.security;

import com.yida.constant.JwtClaimsConstant;
import com.yida.context.AuthenticatedPrincipal;
import com.yida.properties.JwtProperties;
import com.yida.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class JwtTokenService implements TokenService {
    private static final String REVOCATION_PREFIX = "auth:revoked:";
    private final JwtProperties properties;
    private final StringRedisTemplate redisTemplate;

    public JwtTokenService(JwtProperties properties, StringRedisTemplate redisTemplate) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
    }

    @Override public String issueAdmin(Long id) {
        return issue(id, AuthenticatedPrincipal.ADMIN, JwtClaimsConstant.EMP_ID,
                properties.getAdminSecretKey(), properties.getAdminTtl());
    }
    @Override public String issueUser(Long id) {
        return issue(id, AuthenticatedPrincipal.USER, JwtClaimsConstant.USER_ID,
                properties.getUserSecretKey(), properties.getUserTtl());
    }
    @Override public AuthenticatedPrincipal authenticateAdmin(String token) {
        return authenticate(token, AuthenticatedPrincipal.ADMIN, JwtClaimsConstant.EMP_ID,
                properties.getAdminSecretKey());
    }
    @Override public AuthenticatedPrincipal authenticateUser(String token) {
        return authenticate(token, AuthenticatedPrincipal.USER, JwtClaimsConstant.USER_ID,
                properties.getUserSecretKey());
    }
    @Override public void revokeAdmin(String token) { revoke(token, properties.getAdminSecretKey()); }
    @Override public void revokeUser(String token) { revoke(token, properties.getUserSecretKey()); }

    private String issue(Long id, String type, String idClaim, String secret, long ttl) {
        String tokenId = UUID.randomUUID().toString();
        Map<String, Object> claims = new HashMap<>();
        claims.put(idClaim, id);
        claims.put(JwtClaimsConstant.PRINCIPAL_TYPE, type);
        claims.put(JwtClaimsConstant.TOKEN_ID, tokenId);
        return JwtUtil.createJWT(secret, ttl, claims);
    }

    private AuthenticatedPrincipal authenticate(String token, String expectedType,
                                                  String idClaim, String secret) {
        if (!StringUtils.hasText(token)) { throw new IllegalArgumentException("token required"); }
        Claims claims = JwtUtil.parseJWT(secret, token);
        String type = claims.get(JwtClaimsConstant.PRINCIPAL_TYPE, String.class);
        String tokenId = claims.get(JwtClaimsConstant.TOKEN_ID, String.class);
        Object id = claims.get(idClaim);
        if (!expectedType.equals(type) || !StringUtils.hasText(tokenId) || id == null
                || Boolean.TRUE.equals(redisTemplate.hasKey(revocationKey(token)))) {
            throw new IllegalArgumentException("invalid token");
        }
        return new AuthenticatedPrincipal(Long.valueOf(id.toString()), type, tokenId);
    }

    private void revoke(String token, String secret) {
        if (!StringUtils.hasText(token)) { return; }
        Claims claims = JwtUtil.parseJWT(secret, token);
        Date expiration = claims.getExpiration();
        long ttl = expiration == null ? 0 : expiration.getTime() - System.currentTimeMillis();
        if (ttl > 0) {
            redisTemplate.opsForValue().set(revocationKey(token), "1", ttl, TimeUnit.MILLISECONDS);
        }
    }

    private String revocationKey(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return REVOCATION_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash token", ex);
        }
    }
}