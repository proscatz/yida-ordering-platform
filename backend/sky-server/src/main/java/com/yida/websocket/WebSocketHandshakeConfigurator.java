package com.yida.websocket;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class WebSocketHandshakeConfigurator extends ServerEndpointConfig.Configurator {
    static final String TOKEN_PROPERTY = "authenticated.websocket.token";
    private static volatile AutowireCapableBeanFactory beanFactory;

    public static void initialize(AutowireCapableBeanFactory factory) {
        beanFactory = factory;
    }

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        AutowireCapableBeanFactory factory = beanFactory;
        if (factory == null) {
            throw new InstantiationException("WebSocket endpoint factory is not initialized");
        }
        try {
            return factory.createBean(endpointClass);
        } catch (BeansException ex) {
            InstantiationException failure = new InstantiationException("Unable to create WebSocket endpoint");
            failure.initCause(ex);
            throw failure;
        }
    }

    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
        config.getUserProperties().remove(TOKEN_PROPERTY);
        String token = bearer(firstHeader(request.getHeaders(), "Authorization"));
        if (token == null) token = firstHeader(request.getHeaders(), "token");
        if (token == null) token = firstParameter(request.getParameterMap(), "token");
        if (token == null) token = queryParameter(request.getRequestURI(), "token");
        if (token != null) config.getUserProperties().put(TOKEN_PROPERTY, token);
    }

    private String firstHeader(Map<String, List<String>> values, String name) {
        for (Map.Entry<String, List<String>> entry : values.entrySet())
            if (entry.getKey().equalsIgnoreCase(name) && !entry.getValue().isEmpty()) return entry.getValue().get(0);
        return null;
    }
    private String firstParameter(Map<String, List<String>> values, String name) {
        List<String> found=values.get(name); return found==null||found.isEmpty()?null:found.get(0);
    }
    private String bearer(String value) {
        if (value == null) return null;
        String token = value.regionMatches(true,0,"Bearer ",0,7)?value.substring(7).trim():value.trim();
        return token.isEmpty() ? null : token;
    }

    private String queryParameter(URI uri, String name) {
        if (uri == null || uri.getRawQuery() == null) return null;
        for (String pair : uri.getRawQuery().split("&")) {
            int separator = pair.indexOf('=');
            String rawName = separator < 0 ? pair : pair.substring(0, separator);
            if (!name.equals(decode(rawName))) continue;
            String rawValue = separator < 0 ? "" : pair.substring(separator + 1);
            return bearer(decode(rawValue));
        }
        return null;
    }

    private String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (Exception ignored) {
            return null;
        }
    }
}
