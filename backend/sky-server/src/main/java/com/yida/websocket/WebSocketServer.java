package com.yida.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理端订单实时提醒处理器。
 *
 * <p>连接生命周期、关闭帧和消息发送全部使用 Spring WebSocket API，业务层继续通过
 * {@link #sendToAllClient(String)} 广播订单提醒。</p>
 */
@Slf4j
@Component
public class WebSocketServer extends TextWebSocketHandler {
    static final int AUTHENTICATION_FAILURE_CODE = 4001;
    private static final CloseStatus AUTHENTICATION_FAILURE =
            new CloseStatus(AUTHENTICATION_FAILURE_CODE, "Authentication rejected");
    private final Map<String, ManagedSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Object attribute = session.getAttributes()
                .get(WebSocketAuthenticationInterceptor.AUTHENTICATION_ATTRIBUTE);
        if (!(attribute instanceof WebSocketAuthenticationInterceptor.AuthenticationDecision)) {
            failInternal(session, null, "missing_authentication_decision", "none");
            return;
        }

        WebSocketAuthenticationInterceptor.AuthenticationDecision decision =
                (WebSocketAuthenticationInterceptor.AuthenticationDecision) attribute;
        if (decision.getOutcome() == WebSocketAuthenticationInterceptor.Outcome.REJECTED) {
            reject(session, decision);
            return;
        }
        if (decision.getOutcome() == WebSocketAuthenticationInterceptor.Outcome.INTERNAL_FAILURE) {
            failInternal(session, decision.getSid(), decision.getCategory(), decision.getExceptionType());
            return;
        }

        String sid = decision.getSid();
        ManagedSession current = new ManagedSession(session);
        ManagedSession previous = sessions.put(sid, current);
        if (previous != null && previous.session != session && previous.session.isOpen()) {
            close(previous.session, CloseStatus.NORMAL.withReason("Replaced by newer connection"),
                    sid, "connection_replaced");
        }
        log.info("Spring WebSocket connected: sid={}", safeSid(sid));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 管理端订单提醒当前仅使用服务端推送，客户端消息不参与订单状态迁移。
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sid = sidFrom(session);
        removeIfCurrent(sid, session);
        log.info("Spring WebSocket closed: sid={}, closeCode={}", safeSid(sid), status.getCode());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable error) {
        String sid = sidFrom(session);
        removeIfCurrent(sid, session);
        log.warn("Spring WebSocket transport error: category=transport_error, exceptionType={}, sid={}",
                exceptionType(error), safeSid(sid));
        close(session, CloseStatus.SERVER_ERROR, sid, "transport_error");
    }

    public void sendToAllClient(String message) {
        TextMessage payload = new TextMessage(message);
        for (Map.Entry<String, ManagedSession> entry : sessions.entrySet()) {
            WebSocketSession session = entry.getValue().session;
            try {
                if (!session.isOpen()) {
                    sessions.remove(entry.getKey(), entry.getValue());
                    continue;
                }
                // 标准 WebSocketSession 不保证并发发送安全，同一会话内串行化发送。
                synchronized (session) {
                    if (session.isOpen()) session.sendMessage(payload);
                }
            } catch (Exception ex) {
                sessions.remove(entry.getKey(), entry.getValue());
                log.warn("Spring WebSocket send failed: category=send_failure, exceptionType={}, sid={}",
                        exceptionType(ex), safeSid(entry.getKey()));
                close(session, CloseStatus.SERVER_ERROR, entry.getKey(), "send_failure");
            }
        }
    }

    private void reject(WebSocketSession session,
                        WebSocketAuthenticationInterceptor.AuthenticationDecision decision) {
        log.warn("Spring WebSocket authentication rejected: category={}, exceptionType={}, sid={}, closeCode={}",
                decision.getCategory(), decision.getExceptionType(), safeSid(decision.getSid()),
                AUTHENTICATION_FAILURE_CODE);
        close(session, AUTHENTICATION_FAILURE, decision.getSid(), decision.getCategory());
    }

    private void failInternal(WebSocketSession session,
                              String sid,
                              String category,
                              String exceptionType) {
        log.error("Spring WebSocket internal failure: category={}, exceptionType={}, sid={}, closeCode={}",
                category, exceptionType, safeSid(sid), CloseStatus.SERVER_ERROR.getCode());
        close(session, CloseStatus.SERVER_ERROR, sid, category);
    }

    private void close(WebSocketSession session, CloseStatus status, String sid, String category) {
        try {
            if (session != null && session.isOpen()) session.close(status);
        } catch (IOException ex) {
            log.warn("Spring WebSocket close frame failed: category={}, exceptionType={}, sid={}, closeCode={}",
                    category, exceptionType(ex), safeSid(sid), status.getCode());
        }
    }

    private String sidFrom(WebSocketSession session) {
        Object attribute = session.getAttributes()
                .get(WebSocketAuthenticationInterceptor.AUTHENTICATION_ATTRIBUTE);
        if (attribute instanceof WebSocketAuthenticationInterceptor.AuthenticationDecision) {
            return ((WebSocketAuthenticationInterceptor.AuthenticationDecision) attribute).getSid();
        }
        return null;
    }

    private void removeIfCurrent(String sid, WebSocketSession session) {
        if (sid == null) return;
        ManagedSession current = sessions.get(sid);
        if (current != null && current.matches(session)) sessions.remove(sid, current);
    }

    private String safeSid(String sid) {
        return sid != null && sid.matches("[0-9]{1,20}") ? sid : "invalid";
    }

    private String exceptionType(Throwable error) {
        return error == null ? "none" : error.getClass().getSimpleName();
    }

    private static final class ManagedSession {
        private final WebSocketSession session;
        private final String sessionId;

        private ManagedSession(WebSocketSession session) {
            this.session = session;
            this.sessionId = session.getId();
        }

        private boolean matches(WebSocketSession candidate) {
            return session == candidate || Objects.equals(sessionId, candidate.getId());
        }
    }
}
