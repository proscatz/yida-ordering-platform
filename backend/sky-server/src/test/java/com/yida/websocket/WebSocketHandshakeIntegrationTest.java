package com.yida.websocket;

import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure;
import com.yida.config.WebSocketConfiguration;
import com.yida.context.AuthenticatedPrincipal;
import com.yida.entity.Employee;
import com.yida.mapper.EmployeeMapper;
import com.yida.security.TokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = WebSocketHandshakeIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.main.banner-mode=off",
                "spring.profiles.active=ws-test"
        })
class WebSocketHandshakeIntegrationTest {
    private static final String VALID_TOKEN = "valid-admin-token";
    private static final String INVALID_TOKEN = "invalid-admin-token";
    private static final String REVOKED_TOKEN = "revoked-admin-token";
    private static final String SERVICE_FAILURE_TOKEN = "service-failure-token";
    private static final String NULL_PRINCIPAL_TOKEN = "null-principal-token";
    private static final long EMPLOYEE_ID = 7L;

    @LocalServerPort
    private int port;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private EmployeeMapper employeeMapper;

    @org.springframework.beans.factory.annotation.Autowired
    private WebSocketServer server;

    private final List<Connection> openedConnections = new ArrayList<>();

    @BeforeEach
    void configureAuthentication() {
        when(tokenService.authenticateAdmin(VALID_TOKEN))
                .thenReturn(new AuthenticatedPrincipal(EMPLOYEE_ID, "ADMIN", "test-jti"));
        when(tokenService.authenticateAdmin(INVALID_TOKEN)).thenThrow(new IllegalArgumentException("invalid"));
        when(tokenService.authenticateAdmin(REVOKED_TOKEN)).thenThrow(new IllegalArgumentException("revoked"));
        when(tokenService.authenticateAdmin(SERVICE_FAILURE_TOKEN)).thenThrow(new IllegalStateException("unavailable"));
        when(tokenService.authenticateAdmin(NULL_PRINCIPAL_TOKEN)).thenReturn(null);
        when(employeeMapper.getById(EMPLOYEE_ID))
                .thenReturn(Employee.builder().id(EMPLOYEE_ID).status(1).build());
    }

    @AfterEach
    void closeRemainingClients() {
        for (Connection connection : openedConnections) {
            if (!connection.listener.closed.isDone() && !connection.socket.isOutputClosed()) {
                connection.socket.sendClose(WebSocket.NORMAL_CLOSURE, "test cleanup").join();
            }
        }
    }

    @Test
    void correctTokenAndSidRemainConnected() throws Exception {
        Connection connection = connect(String.valueOf(EMPLOYEE_ID), VALID_TOKEN, null);

        Thread.sleep(3_000);

        assertFalse(connection.listener.closed.isDone(), "valid connection must remain open");
    }

    @Test
    void missingTokenReceivesAuthenticationCloseCode() throws Exception {
        Connection connection = connect(String.valueOf(EMPLOYEE_ID), null, null);

        assertEquals(WebSocketServer.AUTHENTICATION_FAILURE_CODE, connection.listener.awaitCloseCode());
    }

    @Test
    void invalidTokenReceivesAuthenticationCloseCode() throws Exception {
        Connection connection = connect(String.valueOf(EMPLOYEE_ID), INVALID_TOKEN, null);

        assertEquals(WebSocketServer.AUTHENTICATION_FAILURE_CODE, connection.listener.awaitCloseCode());
    }

    @Test
    void mismatchedSidReceivesAuthenticationCloseCode() throws Exception {
        Connection connection = connect("8", VALID_TOKEN, null);

        assertEquals(WebSocketServer.AUTHENTICATION_FAILURE_CODE, connection.listener.awaitCloseCode());
    }

    @Test
    void disabledEmployeeReceivesAuthenticationCloseCode() throws Exception {
        when(employeeMapper.getById(EMPLOYEE_ID))
                .thenReturn(Employee.builder().id(EMPLOYEE_ID).status(0).build());
        Connection connection = connect(String.valueOf(EMPLOYEE_ID), VALID_TOKEN, null);

        assertEquals(WebSocketServer.AUTHENTICATION_FAILURE_CODE, connection.listener.awaitCloseCode());
    }

    @Test
    void revokedTokenReceivesAuthenticationCloseCode() throws Exception {
        Connection connection = connect(String.valueOf(EMPLOYEE_ID), REVOKED_TOKEN, null);

        assertEquals(WebSocketServer.AUTHENTICATION_FAILURE_CODE, connection.listener.awaitCloseCode());
    }

    @Test
    void internalAuthenticationServiceFailureReceivesServerErrorCloseCode() throws Exception {
        Connection connection = connect(String.valueOf(EMPLOYEE_ID), SERVICE_FAILURE_TOKEN, null);

        assertEquals(1011, connection.listener.awaitCloseCode());
    }

    @Test
    void invalidAuthenticationServiceResultReceivesServerErrorCloseCode() throws Exception {
        Connection connection = connect(String.valueOf(EMPLOYEE_ID), NULL_PRINCIPAL_TOKEN, null);

        assertEquals(1011, connection.listener.awaitCloseCode());
    }

    @Test
    void browserOriginIsAccepted() throws Exception {
        Connection connection = connect(String.valueOf(EMPLOYEE_ID), VALID_TOKEN, "http://localhost:5174");

        Thread.sleep(500);

        assertFalse(connection.listener.closed.isDone());
    }

    @Test
    void clientNormalCloseIsAcknowledgedAsNormal() throws Exception {
        Connection connection = connect(String.valueOf(EMPLOYEE_ID), VALID_TOKEN, null);

        connection.socket.sendClose(WebSocket.NORMAL_CLOSURE, "client closed").get(3, TimeUnit.SECONDS);

        assertEquals(WebSocket.NORMAL_CLOSURE, connection.listener.awaitCloseCode());
    }

    @Test
    void newerConnectionReplacesOldWithoutRemovingNewSession() throws Exception {
        Connection first = connect(String.valueOf(EMPLOYEE_ID), VALID_TOKEN, null);
        Connection second = connect(String.valueOf(EMPLOYEE_ID), VALID_TOKEN, null);

        assertEquals(WebSocket.NORMAL_CLOSURE, first.listener.awaitCloseCode());
        assertFalse(second.listener.closed.isDone());

        server.sendToAllClient("integration-notification");
        assertEquals("integration-notification", second.listener.awaitMessage());
        assertTrue(first.listener.messages.isEmpty());
    }

    private Connection connect(String sid, String token, String origin) throws Exception {
        RecordingListener listener = new RecordingListener();
        String target = "ws://127.0.0.1:" + port + "/ws/" + sid;
        if (token != null) target += "?token=" + token;
        java.net.http.WebSocket.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build()
                .newWebSocketBuilder();
        if (origin != null) builder.header("Origin", origin);
        WebSocket socket = builder.buildAsync(URI.create(target), listener).get(5, TimeUnit.SECONDS);
        Connection connection = new Connection(socket, listener);
        openedConnections.add(connection);
        listener.opened.get(3, TimeUnit.SECONDS);
        return connection;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            DruidDataSourceAutoConfigure.class,
            RedisAutoConfiguration.class,
            RabbitAutoConfiguration.class
    })
    @Import({WebSocketConfiguration.class, WebSocketServer.class})
    static class TestApplication {
    }

    private static final class Connection {
        private final WebSocket socket;
        private final RecordingListener listener;

        private Connection(WebSocket socket, RecordingListener listener) {
            this.socket = socket;
            this.listener = listener;
        }
    }

    private static final class RecordingListener implements WebSocket.Listener {
        private final CompletableFuture<Void> opened = new CompletableFuture<>();
        private final CompletableFuture<Integer> closed = new CompletableFuture<>();
        private final CompletableFuture<String> firstMessage = new CompletableFuture<>();
        private final List<String> messages = new ArrayList<>();

        @Override
        public void onOpen(WebSocket webSocket) {
            opened.complete(null);
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            String message = data.toString();
            messages.add(message);
            firstMessage.complete(message);
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            closed.complete(statusCode);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            closed.completeExceptionally(error);
        }

        private int awaitCloseCode() throws Exception {
            return closed.get(3, TimeUnit.SECONDS);
        }

        private String awaitMessage() throws Exception {
            return firstMessage.get(3, TimeUnit.SECONDS);
        }
    }
}
