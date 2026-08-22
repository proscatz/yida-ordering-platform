package com.yida.websocket;

import org.junit.jupiter.api.Test;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketHandshakeConfiguratorTest {
    private final WebSocketHandshakeConfigurator configurator = new WebSocketHandshakeConfigurator();

    @Test
    void readsTokenFromContainerParameterMap() {
        ServerEndpointConfig config = endpointConfig();
        HandshakeRequest request = request(Map.of("token", List.of("parameter-token")), URI.create("/ws/7"));

        configurator.modifyHandshake(config, request, mock(HandshakeResponse.class));

        assertEquals("parameter-token", config.getUserProperties().get(WebSocketHandshakeConfigurator.TOKEN_PROPERTY));
    }

    @Test
    void fallsBackToSafelyDecodedRawQueryWhenContainerParameterMapIsEmpty() {
        ServerEndpointConfig config = endpointConfig();
        HandshakeRequest request = request(Collections.emptyMap(), URI.create("/ws/7?other=x&token=a%2Bb%20c"));

        configurator.modifyHandshake(config, request, mock(HandshakeResponse.class));

        assertEquals("a+b c", config.getUserProperties().get(WebSocketHandshakeConfigurator.TOKEN_PROPERTY));
    }

    @Test
    void missingTokenClearsAValueLeftByAnEarlierHandshake() {
        ServerEndpointConfig config = endpointConfig();
        config.getUserProperties().put(WebSocketHandshakeConfigurator.TOKEN_PROPERTY, "stale-token");
        HandshakeRequest request = request(Collections.emptyMap(), URI.create("/ws/7?other=value"));

        configurator.modifyHandshake(config, request, mock(HandshakeResponse.class));

        assertFalse(config.getUserProperties().containsKey(WebSocketHandshakeConfigurator.TOKEN_PROPERTY));
    }

    private ServerEndpointConfig endpointConfig() {
        return ServerEndpointConfig.Builder
                .create(WebSocketServer.class, "/ws/{sid}")
                .configurator(configurator)
                .build();
    }

    private HandshakeRequest request(Map<String, List<String>> parameters, URI uri) {
        HandshakeRequest request = mock(HandshakeRequest.class);
        when(request.getHeaders()).thenReturn(new HashMap<>());
        when(request.getParameterMap()).thenReturn(parameters);
        when(request.getRequestURI()).thenReturn(uri);
        return request;
    }
}
