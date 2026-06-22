package com.guang.miniecommercebackend.config;

import com.guang.miniecommercebackend.security.StompAuthChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration                              // 告诉 Spring：这是配置类
@EnableWebSocketMessageBroker               // 开启 STOMP 消息代理
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    public WebSocketConfig(StompAuthChannelInterceptor stompAuthChannelInterceptor) {
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
    }

    //    前端连哪里？可以想成：在前端和后端之间开一个门，门牌号是 /ws。连接地址：http://localhost:8080/ws
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    //    消息走哪条路？
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");  // 客户端→服务器的路径前缀
        registry.enableSimpleBroker("/topic");              // 服务器→客户端的广播前缀
    }

    //  连上来时验身份
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
