package com.yida.config;

import com.yida.websocket.WebSocketAuthenticationInterceptor;
import com.yida.websocket.WebSocketServer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Spring WebSocket 配置：注册订单提醒处理器和握手鉴权拦截器。
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfiguration implements WebSocketConfigurer {
    private final WebSocketServer webSocketServer;
    private final WebSocketAuthenticationInterceptor authenticationInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketServer, "/ws/{sid}")
                .addInterceptors(authenticationInterceptor)
                .setAllowedOriginPatterns("*");
    }
}

