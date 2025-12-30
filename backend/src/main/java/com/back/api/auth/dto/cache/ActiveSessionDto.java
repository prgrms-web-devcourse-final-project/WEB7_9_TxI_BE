package com.back.api.auth.dto.cache;

import java.io.Serializable;

import com.back.domain.auth.entity.ActiveSession;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ActiveSession Redis 캐시용 DTO
 * - DB 조회 없이 sessionId와 tokenVersion 검증
 * - Redis 직렬화를 위해 Serializable 구현
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ActiveSessionDto implements Serializable {
	private static final long serialVersionUID = 1L;

	private String sessionId;
	private long tokenVersion;

	public static ActiveSessionDto from(ActiveSession session) {
		return new ActiveSessionDto(
			session.getSessionId(),
			session.getTokenVersion()
		);
	}

	public boolean matches(String sid, long version) {
		return this.sessionId.equals(sid) && this.tokenVersion == version;
	}
}
