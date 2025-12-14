package com.back.global.websocket.auth;

import java.security.Principal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * WebSocket Principal 구현체
 * STOMP 세션에서 사용자를 식별하기 위한 클래스
 */
@Getter
@RequiredArgsConstructor
public class UserPrincipal implements Principal {

	private final Long userId;

	/**
	 * Principal의 name은 String이어야 하므로 userId를 String으로 변환
	 * convertAndSendToUser(name, ...) 호출 시 이 값과 매칭됨
	 */
	@Override
	public String getName() {
		return userId.toString();
	}
}
