package com.yida.websocket;

import com.yida.constant.StatusConstant;
import com.yida.context.AuthenticatedPrincipal;
import com.yida.entity.Employee;
import com.yida.mapper.EmployeeMapper;
import com.yida.security.TokenService;
import io.jsonwebtoken.JwtException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 在 Spring WebSocket 握手阶段完成管理员身份校验，并把脱敏后的校验结果传给连接处理器。
 *
 * <p>校验失败时仍允许完成协议升级，由 {@link WebSocketServer} 发送标准关闭帧，
 * 使浏览器能够收到 4001 或 1011，而不是不可判定的 1006。</p>
 */
@Component
@RequiredArgsConstructor
public class WebSocketAuthenticationInterceptor implements HandshakeInterceptor {
    static final String AUTHENTICATION_ATTRIBUTE = "yida.websocket.authentication";

    private final TokenService tokenService;
    private final EmployeeMapper employeeMapper;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String sid = extractSid(request.getURI());
        attributes.put(AUTHENTICATION_ATTRIBUTE, authenticate(request, sid));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // 连接建立、拒绝和异常日志由 WebSocketServer 统一记录，避免在此输出 URL 或 Token。
    }

    private AuthenticationDecision authenticate(ServerHttpRequest request, String sid) {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            return AuthenticationDecision.rejected(sid, "missing_token", null);
        }

        final AuthenticatedPrincipal principal;
        try {
            principal = tokenService.authenticateAdmin(token);
        } catch (JwtException | IllegalArgumentException | SecurityException ex) {
            return AuthenticationDecision.rejected(sid, "invalid_token", ex);
        } catch (RuntimeException ex) {
            return AuthenticationDecision.internalFailure(sid, "token_service_failure", ex);
        }

        if (principal == null) {
            return AuthenticationDecision.internalFailure(sid, "token_service_invalid_result", null);
        }
        if (!AuthenticatedPrincipal.ADMIN.equals(principal.getType())) {
            return AuthenticationDecision.rejected(sid, "invalid_principal_type", null);
        }
        if (!StringUtils.hasText(sid) || !String.valueOf(principal.getId()).equals(sid)) {
            return AuthenticationDecision.rejected(sid, "identity_mismatch", null);
        }

        final Employee employee;
        try {
            employee = employeeMapper.getById(principal.getId());
        } catch (RuntimeException ex) {
            return AuthenticationDecision.internalFailure(sid, "employee_lookup_failure", ex);
        }
        if (employee == null || !StatusConstant.ENABLE.equals(employee.getStatus())) {
            return AuthenticationDecision.rejected(sid, "employee_inactive", null);
        }
        return AuthenticationDecision.accepted(sid, principal.getId());
    }

    private String resolveToken(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        String token = normalizeToken(headers.getFirst(HttpHeaders.AUTHORIZATION));
        if (token == null) token = normalizeToken(headers.getFirst("token"));
        if (token == null) token = queryParameter(request.getURI(), "token");
        return token;
    }

    private String normalizeToken(String value) {
        if (!StringUtils.hasText(value)) return null;
        String trimmed = value.trim();
        if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            trimmed = trimmed.substring(7).trim();
        }
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }

    private String queryParameter(URI uri, String expectedName) {
        if (uri == null || uri.getRawQuery() == null) return null;
        for (String pair : uri.getRawQuery().split("&")) {
            int separator = pair.indexOf('=');
            String rawName = separator < 0 ? pair : pair.substring(0, separator);
            if (!expectedName.equals(decode(rawName))) continue;
            String rawValue = separator < 0 ? "" : pair.substring(separator + 1);
            return normalizeToken(decode(rawValue));
        }
        return null;
    }

    private String extractSid(URI uri) {
        if (uri == null || uri.getRawPath() == null) return null;
        String path = uri.getRawPath();
        int separator = path.lastIndexOf('/');
        if (separator < 0 || separator == path.length() - 1) return null;
        String sid = decode(path.substring(separator + 1));
        return sid != null && sid.matches("[0-9]{1,20}") ? sid : null;
    }

    private String decode(String value) {
        try {
            return UriUtils.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    enum Outcome {
        ACCEPTED,
        REJECTED,
        INTERNAL_FAILURE
    }

    @Getter
    static final class AuthenticationDecision {
        private final Outcome outcome;
        private final String sid;
        private final Long employeeId;
        private final String category;
        private final String exceptionType;

        private AuthenticationDecision(Outcome outcome,
                                       String sid,
                                       Long employeeId,
                                       String category,
                                       Throwable error) {
            this.outcome = outcome;
            this.sid = sid;
            this.employeeId = employeeId;
            this.category = category;
            this.exceptionType = error == null ? "none" : error.getClass().getSimpleName();
        }

        static AuthenticationDecision accepted(String sid, Long employeeId) {
            return new AuthenticationDecision(Outcome.ACCEPTED, sid, employeeId, "accepted", null);
        }

        static AuthenticationDecision rejected(String sid, String category, Throwable error) {
            return new AuthenticationDecision(Outcome.REJECTED, sid, null, category, error);
        }

        static AuthenticationDecision internalFailure(String sid, String category, Throwable error) {
            return new AuthenticationDecision(Outcome.INTERNAL_FAILURE, sid, null, category, error);
        }
    }
}

