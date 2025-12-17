package com.back.global.websocket.auth;

import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import com.back.global.security.JwtProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket 연결 시 STOMP 헤더에서 JWT를 검증하는 Interceptor
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

	private static final String BEARER_PREFIX = "Bearer ";
	private static final String AUTHORIZATION_HEADER = "Authorization";

	private final JwtProvider jwtProvider;

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

		if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
			// STOMP CONNECT 프레임일 때만 인증 처리
			String authHeader = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);

			if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
				log.warn("웹소켓 연결 거부 - Authorization 헤더 없음 또는 형식 오류");
				throw new IllegalArgumentException("Authorization 헤더가 없거나 형식이 올바르지 않습니다");
			}

			String token = authHeader.substring(BEARER_PREFIX.length());

			// JWT 만료 검증
			if (jwtProvider.isExpired(token)) {
				log.warn("웹소켓 연결 거부 - JWT 만료");
				throw new IllegalArgumentException("만료된 토큰입니다");
			}

			// JWT 파싱 및 userId 추출
			Map<String, Object> payload = jwtProvider.payloadOrNull(token);
			if (payload == null) {
				log.warn("웹소켓 연결 거부 - JWT 파싱 실패");
				throw new IllegalArgumentException("유효하지 않은 토큰입니다");
			}

			Long userId = ((Number) payload.get("id")).longValue();

			// Principal 설정 (convertAndSendToUser에서 사용)
			accessor.setUser(new UserPrincipal(userId));

			log.info("웹소켓 연결 성공 - userId: {}", userId);
		}

		return message;
	}
}
