package com.yida.websocket;

import com.yida.constant.StatusConstant;
import com.yida.context.AuthenticatedPrincipal;
import com.yida.entity.Employee;
import com.yida.mapper.EmployeeMapper;
import com.yida.security.TokenService;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ServerEndpoint(value="/ws/{sid}", configurator=WebSocketHandshakeConfigurator.class)
public class WebSocketServer {
    static final int AUTHENTICATION_FAILURE_CODE = 4001;
    private static final CloseReason.CloseCode AUTHENTICATION_FAILURE = () -> AUTHENTICATION_FAILURE_CODE;
    private static final Map<String, Session> SESSIONS = new ConcurrentHashMap<>();

    private final TokenService tokenService;
    private final EmployeeMapper employeeMapper;

    public WebSocketServer(TokenService tokenService, EmployeeMapper employeeMapper) {
        this.tokenService = tokenService;
        this.employeeMapper = employeeMapper;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        Object tokenValue = session.getUserProperties().get(WebSocketHandshakeConfigurator.TOKEN_PROPERTY);
        String token = tokenValue == null ? null : tokenValue.toString();
        if (token == null || token.trim().isEmpty()) {
            reject(session, sid, "missing_token", null);
            return;
        }

        AuthenticatedPrincipal principal;
        try {
            principal = tokenService.authenticateAdmin(token);
        } catch (JwtException | IllegalArgumentException | SecurityException ex) {
            reject(session, sid, "invalid_token", ex);
            return;
        } catch (RuntimeException ex) {
            failInternal(session, sid, "token_service_failure", ex);
            return;
        }

        if (principal == null) {
            failInternal(session, sid, "token_service_invalid_result", null);
            return;
        }
        if (!AuthenticatedPrincipal.ADMIN.equals(principal.getType())) {
            reject(session, sid, "invalid_principal_type", null);
            return;
        }
        if (!String.valueOf(principal.getId()).equals(sid)) {
            reject(session, sid, "identity_mismatch", null);
            return;
        }

        final Employee employee;
        try {
            employee = employeeMapper.getById(principal.getId());
        } catch (RuntimeException ex) {
            failInternal(session, sid, "employee_lookup_failure", ex);
            return;
        }
        if (employee == null || !StatusConstant.ENABLE.equals(employee.getStatus())) {
            reject(session, sid, "employee_inactive", null);
            return;
        }

        Session previous = SESSIONS.put(sid, session);
        if (previous != null && previous != session && previous.isOpen()) {
            close(previous, CloseReason.CloseCodes.NORMAL_CLOSURE, "Replaced by newer connection",
                    sid, "connection_replaced");
        }
        log.info("WebSocket connected: sid={}", safeSid(sid));
    }

    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        // 管理端订单提醒当前仅使用服务端推送，客户端消息不参与订单状态迁移。
    }

    @OnClose
    public void onClose(Session session, @PathParam("sid") String sid, CloseReason reason) {
        SESSIONS.remove(sid, session);
        log.info("WebSocket closed: sid={}, closeCode={}", safeSid(sid), reason.getCloseCode().getCode());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        Map<String, String> pathParameters = session == null ? null : session.getPathParameters();
        String sid = pathParameters == null ? null : pathParameters.get("sid");
        if (session != null && sid != null) {
            SESSIONS.remove(sid, session);
        } else if (session != null) {
            SESSIONS.entrySet().removeIf(entry -> entry.getValue() == session);
        }
        log.warn("WebSocket transport error: category=transport_error, exceptionType={}, sid={}",
                exceptionType(error), safeSid(sid));
    }

    public void sendToAllClient(String message) {
        for (Map.Entry<String, Session> entry : SESSIONS.entrySet()) {
            Session session = entry.getValue();
            try {
                if (session.isOpen()) {
                    session.getBasicRemote().sendText(message);
                } else {
                    SESSIONS.remove(entry.getKey(), session);
                }
            } catch (Exception ex) {
                SESSIONS.remove(entry.getKey(), session);
                log.warn("WebSocket send failed: category=send_failure, exceptionType={}, sid={}",
                        exceptionType(ex), safeSid(entry.getKey()));
            }
        }
    }

    private void reject(Session session, String sid, String category, Throwable error) {
        log.warn("WebSocket authentication rejected: category={}, exceptionType={}, sid={}, closeCode={}",
                category, exceptionType(error), safeSid(sid), AUTHENTICATION_FAILURE_CODE);
        close(session, AUTHENTICATION_FAILURE, "Authentication rejected", sid, category);
    }

    private void failInternal(Session session, String sid, String category, Throwable error) {
        log.error("WebSocket internal failure: category={}, exceptionType={}, sid={}, closeCode={}",
                category, exceptionType(error), safeSid(sid),
                CloseReason.CloseCodes.UNEXPECTED_CONDITION.getCode());
        close(session, CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Internal server error", sid, category);
    }

    private void close(Session session, CloseReason.CloseCode code, String reason, String sid, String category) {
        try {
            if (session != null && session.isOpen()) session.close(new CloseReason(code, reason));
        } catch (IOException ex) {
            log.warn("WebSocket close frame failed: category={}, exceptionType={}, sid={}, closeCode={}",
                    category, exceptionType(ex), safeSid(sid), code.getCode());
        }
    }

    private String safeSid(String sid) {
        return sid != null && sid.matches("[0-9]{1,20}") ? sid : "invalid";
    }

    private String exceptionType(Throwable error) {
        return error == null ? "none" : error.getClass().getSimpleName();
    }
}
