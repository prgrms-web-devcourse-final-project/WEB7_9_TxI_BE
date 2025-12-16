package com.back.global.websocket.session;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.back.global.websocket.auth.UserPrincipal;

import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket 세션 관리 클래스
 * 사용자 온라인 여부 체크 및 userId ↔ sessionId 매핑 관리
 */
@Slf4j
@Component
public class WebSocketSessionManager {

	// userId → sessionId 매핑
	// ConcurrentHashMap 사용으로 thread-safe 보장
	private final ConcurrentHashMap<Long, String> userSessions = new ConcurrentHashMap<>();

	/**
	 * 웹소켓 연결 성공 시 호출
	 */
	@EventListener
	public void handleWebSocketConnectListener(SessionConnectEvent event) {
		StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

		if (headerAccessor.getUser() instanceof UserPrincipal principal) {
			Long userId = principal.getUserId();
			String sessionId = headerAccessor.getSessionId();

			userSessions.put(userId, sessionId);
			log.info("웹소켓 세션 저장 - userId: {}, sessionId: {}", userId, sessionId);
		}
	}

	/**
	 * 웹소켓 연결 해제 시 호출
	 */
	@EventListener
	public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
		StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

		if (headerAccessor.getUser() instanceof UserPrincipal principal) {
			Long userId = principal.getUserId();

			userSessions.remove(userId);
			log.info("웹소켓 세션 제거 - userId: {}", userId);
		}
	}

	/**
	 * 사용자 온라인 여부 확인
	 *
	 * @param userId 확인할 사용자 ID
	 * @return 온라인이면 true, 오프라인이면 false
	 */
	public boolean isUserOnline(Long userId) {
		return userSessions.containsKey(userId);
	}

	/**
	 * 특정 사용자의 세션 ID 조회
	 *
	 * @param userId 사용자 ID
	 * @return 세션 ID (없으면 null)
	 */
	public String getSessionId(Long userId) {
		return userSessions.get(userId);
	}

	/**
	 * 현재 온라인 사용자 수
	 *
	 * @return 온라인 사용자 수
	 */
	public int getOnlineUserCount() {
		return userSessions.size();
	}
}
