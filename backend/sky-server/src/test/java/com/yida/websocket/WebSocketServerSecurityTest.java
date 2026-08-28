package com.yida.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketServerSecurityTest {
    @Test
    void rejectedAuthenticationIsClosedWithPolicyCode() throws Exception {
        WebSocketServer server = new WebSocketServer();
        WebSocketSession session = session("session-1",
                WebSocketAuthenticationInterceptor.AuthenticationDecision
                        .rejected("1", "missing_token", null));

        server.afterConnectionEstablished(session);

        verify(session).close(new CloseStatus(WebSocketServer.AUTHENTICATION_FAILURE_CODE,
                "Authentication rejected"));
    }

    @Test
    void internalAuthenticationFailureIsClosedWithServerError() throws Exception {
        WebSocketServer server = new WebSocketServer();
        WebSocketSession session = session("session-1",
                WebSocketAuthenticationInterceptor.AuthenticationDecision
                        .internalFailure("1", "token_service_failure", new IllegalStateException()));

        server.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.SERVER_ERROR);
    }

    @Test
    void acceptedConnectionRemainsOpenAndReceivesBroadcast() throws Exception {
        WebSocketServer server = new WebSocketServer();
        WebSocketSession session = session("session-1",
                WebSocketAuthenticationInterceptor.AuthenticationDecision.accepted("1", 1L));

        server.afterConnectionEstablished(session);
        server.sendToAllClient("notification");

        verify(session, never()).close(any(CloseStatus.class));
        verify(session).sendMessage(new TextMessage("notification"));
    }

    @Test
    void closingReplacedConnectionDoesNotRemoveNewConnection() throws Exception {
        WebSocketServer server = new WebSocketServer();
        WebSocketAuthenticationInterceptor.AuthenticationDecision decision =
                WebSocketAuthenticationInterceptor.AuthenticationDecision.accepted("1", 1L);
        WebSocketSession first = session("session-1", decision);
        WebSocketSession second = session("session-2", decision);

        server.afterConnectionEstablished(first);
        server.afterConnectionEstablished(second);
        server.afterConnectionClosed(first, CloseStatus.NORMAL);
        server.sendToAllClient("new-session-only");

        verify(first).close(CloseStatus.NORMAL.withReason("Replaced by newer connection"));
        verify(first, never()).sendMessage(any());
        verify(second).sendMessage(new TextMessage("new-session-only"));
    }

    private WebSocketSession session(
            String sessionId,
            WebSocketAuthenticationInterceptor.AuthenticationDecision decision) {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(WebSocketAuthenticationInterceptor.AUTHENTICATION_ATTRIBUTE, decision);
        when(session.getId()).thenReturn(sessionId);
        when(session.getAttributes()).thenReturn(attributes);
        when(session.isOpen()).thenReturn(true);
        return session;
    }
}
