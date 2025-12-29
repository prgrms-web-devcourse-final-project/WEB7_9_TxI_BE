package com.back.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.back.global.properties.SiteProperties;
import com.back.global.websocket.auth.WebSocketAuthInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final WebSocketAuthInterceptor webSocketAuthInterceptor;
	private final SiteProperties siteProperties;

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {

		//클라이언트 구독 경로
		registry.enableSimpleBroker("/topic/", "/queue/", "/user");

		//클라이언트 발행 경로
		registry.setApplicationDestinationPrefixes("/app");

		// 특정 유저에게 메시지 보낼 때 사용할 prefix
		registry.setUserDestinationPrefix("/user");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {

		//Websocket/stomp 연결 endpoint
		registry.addEndpoint("/ws")
			.setAllowedOriginPatterns("*")
			// .setAllowedOrigins(siteProperties.getFrontUrl()) 프론트 배포 후 변경
			.withSockJS();
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(webSocketAuthInterceptor);
	}

}
