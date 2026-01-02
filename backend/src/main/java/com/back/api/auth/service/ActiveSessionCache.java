package com.back.api.auth.service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.back.api.auth.dto.cache.ActiveSessionDto;
import com.back.domain.auth.entity.ActiveSession;
import com.back.domain.auth.repository.ActiveSessionRepository;
import com.back.global.error.code.AuthErrorCode;
import com.back.global.error.exception.ErrorException;

import lombok.extern.slf4j.Slf4j;

/**
 * ActiveSession Redis 캐시
 * - Redis 우선 조회, Redis miss 시 DB 조회 후 캐싱
 * - Redis 장애 시 fast-fail (DB 과부하 방지)
 * - String 직렬화: "{sessionId}:{tokenVersion}"
 * - TTL 랜덤 지터로 캐시 stampede 방지
 * - 로그인/로그아웃 시 자동 무효화
 */
@Component
@Slf4j
public class ActiveSessionCache {

	private static final String KEY_PREFIX = "active_session:";
	private static final double JITTER_RATIO = 0.1; // ±10% 지터
	private static final String DELIMITER = ":"; // sessionId:tokenVersion

	private final RedisTemplate<String, String> redisTemplate;
	private final ActiveSessionRepository activeSessionRepository;

	public ActiveSessionCache(
		@Qualifier("stringTemplate") RedisTemplate<String, String> redisTemplate,
		ActiveSessionRepository activeSessionRepository
	) {
		this.redisTemplate = redisTemplate;
		this.activeSessionRepository = activeSessionRepository;
	}

	@Value("${custom.jwt.access-token-duration:3600}")
	private long accessTokenDurationSeconds;

	/**
	 * ActiveSession 조회 (Redis 우선, miss 시 DB 조회 후 캐싱)
	 * - Redis 장애 시 fast-fail (TEMPORARY_AUTH_UNAVAILABLE)
	 */
	public Optional<ActiveSessionDto> get(long userId) {
		String key = getKey(userId);

		try {
			// Redis에서 조회 시도
			String cached = redisTemplate.opsForValue().get(key);
			if (cached != null) {
				return Optional.of(deserialize(cached));
			}

			// Redis miss, DB에서 조회 후 캐싱
			Optional<ActiveSession> dbSession = activeSessionRepository.findByUserId(userId);

			if (dbSession.isPresent()) {
				ActiveSessionDto dto = ActiveSessionDto.from(dbSession.get());
				set(userId, dto); // 캐싱
				return Optional.of(dto);
			}

			return Optional.empty();

		} catch (Exception e) {
			// [Fast-fail] Redis 장애 시 즉시 예외 발생
			log.error("Redis failure detected for userId: {}, failing fast to prevent DB overload", userId, e);
			throw new ErrorException(AuthErrorCode.TEMPORARY_AUTH_UNAVAILABLE);
		}
	}

	/**
	 * ActiveSession 캐싱 (TTL 랜덤 지터 적용)
	 * - 캐시 stampede 방지를 위해 TTL에 ±10% 지터 추가
	 */
	public void set(long userId, ActiveSessionDto session) {
		String key = getKey(userId);
		String value = serialize(session);
		Duration ttl = getTtlWithJitter();

		try {
			redisTemplate.opsForValue().set(key, value, ttl);
		} catch (Exception e) {
			// 캐싱 실패 시 로깅만 (다음 요청 시 DB에서 조회 가능)
			log.warn("Failed to cache ActiveSession for userId: {}", userId, e);
		}
	}

	//ActiveSession 캐시 무효화
	public void evict(long userId) {
		String key = getKey(userId);

		try {
			Boolean deleted = redisTemplate.delete(key);
		} catch (Exception e) {
			// 무효화 실패 시 로깅만 (TTL 만료 시 자동 삭제됨)
			log.warn("Failed to evict ActiveSession cache for userId: {}", userId, e);
		}
	}

	// Redis key 생성
	private String getKey(long userId) {
		return KEY_PREFIX + userId;
	}

	// dto -> string / {sessionId}:{tokenVersion}
	private String serialize(ActiveSessionDto dto) {
		return dto.getSessionId() + DELIMITER + dto.getTokenVersion();
	}

	// string -> dto
	private ActiveSessionDto deserialize(String value) {
		String[] parts = value.split(DELIMITER, 2);
		if (parts.length != 2) {
			throw new IllegalArgumentException("Invalid cached session format: " + value);
		}

		String sessionId = parts[0];
		long tokenVersion = Long.parseLong(parts[1]);

		return new ActiveSessionDto(sessionId, tokenVersion);
	}

	/**
	 * TTL with random jitter (±10%)
	 * - 캐시 stampede 방지
	 * - 동시 만료로 인한 DB 부하 분산
	 */
	private Duration getTtlWithJitter() {
		double jitter = 1.0 + (ThreadLocalRandom.current().nextDouble() * 2 - 1) * JITTER_RATIO;
		long ttlWithJitter = (long)(accessTokenDurationSeconds * jitter);
		return Duration.ofSeconds(ttlWithJitter);
	}
}
