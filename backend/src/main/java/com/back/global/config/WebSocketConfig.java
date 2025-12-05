package com.back.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {

		//클라이언트 구독 경로
		registry.enableSimpleBroker("/topic/", "/queue/");

		//클라이언트 발행 경로
		registry.setApplicationDestinationPrefixes("/app");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {

		//Websocket/stomp 연결 endpoint
		registry.addEndpoint("/ws")
			.setAllowedOriginPatterns("*")
			.withSockJS();
	}
}
