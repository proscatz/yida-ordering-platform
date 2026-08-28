package com.yida.websocket;

import com.yida.context.AuthenticatedPrincipal;
import com.yida.entity.Employee;
import com.yida.mapper.EmployeeMapper;
import com.yida.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketAuthenticationInterceptorTest {
    private TokenService tokenService;
    private EmployeeMapper employeeMapper;
    private WebSocketAuthenticationInterceptor interceptor;

    @BeforeEach
    void setUp() {
        tokenService = mock(TokenService.class);
        employeeMapper = mock(EmployeeMapper.class);
        interceptor = new WebSocketAuthenticationInterceptor(tokenService, employeeMapper);
    }

    @Test
    void acceptsSafelyDecodedQueryTokenForMatchingEnabledEmployee() {
        when(tokenService.authenticateAdmin("a+b c"))
                .thenReturn(new AuthenticatedPrincipal(7L, "ADMIN", "test-jti"));
        when(employeeMapper.getById(7L)).thenReturn(Employee.builder().id(7L).status(1).build());

        WebSocketAuthenticationInterceptor.AuthenticationDecision decision =
                handshake(URI.create("/ws/7?other=x&token=a%2Bb%20c"), new HttpHeaders());

        assertEquals(WebSocketAuthenticationInterceptor.Outcome.ACCEPTED, decision.getOutcome());
        assertEquals("7", decision.getSid());
        assertEquals(7L, decision.getEmployeeId());
    }

    @Test
    void authorizationBearerHeaderTakesPrecedenceOverQueryToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("header-token");
        when(tokenService.authenticateAdmin("header-token"))
                .thenReturn(new AuthenticatedPrincipal(7L, "ADMIN", "test-jti"));
        when(employeeMapper.getById(7L)).thenReturn(Employee.builder().id(7L).status(1).build());

        WebSocketAuthenticationInterceptor.AuthenticationDecision decision =
                handshake(URI.create("/ws/7?token=query-token"), headers);

        assertEquals(WebSocketAuthenticationInterceptor.Outcome.ACCEPTED, decision.getOutcome());
    }

    @Test
    void missingTokenCreatesAuthenticationRejectionDecision() {
        WebSocketAuthenticationInterceptor.AuthenticationDecision decision =
                handshake(URI.create("/ws/7"), new HttpHeaders());

        assertEquals(WebSocketAuthenticationInterceptor.Outcome.REJECTED, decision.getOutcome());
        assertEquals("missing_token", decision.getCategory());
    }

    @Test
    void identityMismatchCreatesAuthenticationRejectionDecision() {
        when(tokenService.authenticateAdmin("token"))
                .thenReturn(new AuthenticatedPrincipal(8L, "ADMIN", "test-jti"));

        WebSocketAuthenticationInterceptor.AuthenticationDecision decision =
                handshake(URI.create("/ws/7?token=token"), new HttpHeaders());

        assertEquals(WebSocketAuthenticationInterceptor.Outcome.REJECTED, decision.getOutcome());
        assertEquals("identity_mismatch", decision.getCategory());
    }

    @Test
    void disabledEmployeeCreatesAuthenticationRejectionDecision() {
        when(tokenService.authenticateAdmin("token"))
                .thenReturn(new AuthenticatedPrincipal(7L, "ADMIN", "test-jti"));
        when(employeeMapper.getById(7L)).thenReturn(Employee.builder().id(7L).status(0).build());

        WebSocketAuthenticationInterceptor.AuthenticationDecision decision =
                handshake(URI.create("/ws/7?token=token"), new HttpHeaders());

        assertEquals(WebSocketAuthenticationInterceptor.Outcome.REJECTED, decision.getOutcome());
        assertEquals("employee_inactive", decision.getCategory());
    }

    private WebSocketAuthenticationInterceptor.AuthenticationDecision handshake(URI uri, HttpHeaders headers) {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getURI()).thenReturn(uri);
        when(request.getHeaders()).thenReturn(headers);
        Map<String, Object> attributes = new HashMap<>();

        assertTrue(interceptor.beforeHandshake(
                request,
                mock(ServerHttpResponse.class),
                mock(WebSocketHandler.class),
                attributes));

        return (WebSocketAuthenticationInterceptor.AuthenticationDecision)
                attributes.get(WebSocketAuthenticationInterceptor.AUTHENTICATION_ATTRIBUTE);
    }
}

