package com.yida.config;

import com.yida.websocket.WebSocketHandshakeConfigurator;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket配置类，用于注册WebSocket的Bean
 */
@Configuration
public class WebSocketConfiguration {

    @Bean
    public ServerEndpointExporter serverEndpointExporter(AutowireCapableBeanFactory beanFactory) {
        WebSocketHandshakeConfigurator.initialize(beanFactory);
        return new ServerEndpointExporter();
    }

}
