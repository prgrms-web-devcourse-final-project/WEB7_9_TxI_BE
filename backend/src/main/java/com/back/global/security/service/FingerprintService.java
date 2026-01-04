package com.back.global.security.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.back.global.properties.SecurityProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Device Fingerprint 검증 서비스
 *
 * 목적:
 * - IP를 바꿔가며 접속하는 업자 탐지
 * - FingerprintJS를 통해 브라우저/디바이스 고유 ID 추적
 *
 * 동작 방식:
 * 1. 프론트엔드에서 visitorId(fingerprint) 전송
 * 2. Redis에 fingerprint별 시도 횟수, 실패 횟수 저장
 * 3. 차단 기준 검증 (2가지)
 *
 * Redis 저장 구조:
 * - Key: "fingerprint:{visitorId}"
 * - Value: JSON { totalAttempts, failedAttempts, successCount }
 * - TTL: 24시간
 *
 * 차단 기준:
 * 1. 최대 시도 횟수 초과 (성공 폭탄 공격 방어)
 *    - 전체 시도 >= 10회
 * 2. 높은 실패율 (봇/어뷰징 방어)
 *    - 전체 시도 >= 5회 AND 실패율 >= 80%
 *
 * 예외 처리:
 * - visitorId가 없는 요청: 1차 허용 (IP Rate Limit으로만 통제)
 * - Redis 장애 시: 요청 허용 (가용성 우선)
 */
@Slf4j
@Profile("!test")
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "security.bot-protection.fingerprint.enabled", havingValue = "true", matchIfMissing = false)
public class FingerprintService {

	private static final String REDIS_KEY_PREFIX = "fingerprint:";

	private final StringRedisTemplate redisTemplate;
	private final SecurityProperties securityProperties;
	private final ObjectMapper objectMapper;

	/**
	 * Fingerprint 검증
	 *
	 * @param visitorId FingerprintJS visitorId
	 * @return 허용 여부
	 */
	public boolean validateFingerprint(String visitorId) {
		if (!securityProperties.getFingerprint().isEnabled()) {
			return true;
		}

		// visitorId가 없으면 허용 (1차)
		if (visitorId == null || visitorId.isEmpty()) {
			log.debug("[FingerprintService] visitorId 없음 - 1차 허용");
			return true;
		}

		try {
			String key = REDIS_KEY_PREFIX + visitorId;

			// Redis Hash에서 데이터 읽기
			Integer totalAttempts = getHashValueAsInt(key, "totalAttempts");
			Integer failedAttempts = getHashValueAsInt(key, "failedAttempts");

			// 차단 기준 1: 최대 시도 횟수 초과 (성공 폭탄 공격 방어)
			int maxAttempts = securityProperties.getFingerprint().getMaxAttempts();
			if (totalAttempts >= maxAttempts) {
				log.warn("[FingerprintService] Fingerprint 차단 (최대 시도 횟수 초과) - visitorId: {}, 시도: {}/{}, 실패: {}",
					visitorId, totalAttempts, maxAttempts, failedAttempts);
				return false;
			}

			// 차단 기준 2: 최소 시도 횟수 이상 + 높은 실패율 (봇/어뷰징 방어)
			if (totalAttempts >= securityProperties.getFingerprint().getMinAttempts()) {
				// 부동소수점 오차 방지: 백분율 정수 비교
				int failureRatePercent = (failedAttempts * 100) / totalAttempts;
				int thresholdPercent = (int)(securityProperties.getFingerprint().getFailureRateThreshold() * 100);

				if (failureRatePercent >= thresholdPercent) {
					log.warn("[FingerprintService] Fingerprint 차단 (높은 실패율) - visitorId: {}, 시도: {}, 실패율: {}%",
						visitorId, totalAttempts, failureRatePercent);
					return false;
				}
			}

			return true;
		} catch (Exception e) {
			// Redis 장애 시 허용 (가용성 우선)
			log.error("[FingerprintService] Fingerprint 검증 실패 - visitorId: {}, 오류: {} - 요청 허용",
				visitorId, e.getMessage());
			return true;
		}
	}

	/**
	 * Redis Hash에서 정수 값 가져오기
	 *
	 * @param key   Redis 키
	 * @param field Hash 필드명
	 * @return 정수 값 (없으면 0)
	 */
	private Integer getHashValueAsInt(String key, String field) {
		Object value = redisTemplate.opsForHash().get(key, field);
		if (value == null) {
			return 0;
		}
		if (value instanceof Integer) {
			return (Integer)value;
		}
		if (value instanceof String) {
			return Integer.parseInt((String)value);
		}
		if (value instanceof Long) {
			return ((Long)value).intValue();
		}
		return 0;
	}

	/**
	 * 시도 기록 (성공/실패) - 원자적 증가 (Race Condition 방지)
	 *
	 * Redis Hash + HINCRBY를 사용하여 동시성 문제 해결
	 *
	 * @param visitorId FingerprintJS visitorId
	 * @param success   성공 여부
	 */
	public void recordAttempt(String visitorId, boolean success) {
		if (!securityProperties.getFingerprint().isEnabled()) {
			return;
		}

		if (visitorId == null || visitorId.isEmpty()) {
			return;
		}

		try {
			String key = REDIS_KEY_PREFIX + visitorId;

			// Redis Hash 원자적 증가 (Race Condition 방지)
			Long totalAttempts = redisTemplate.opsForHash().increment(key, "totalAttempts", 1);

			if (success) {
				redisTemplate.opsForHash().increment(key, "successCount", 1);
			} else {
				redisTemplate.opsForHash().increment(key, "failedAttempts", 1);
			}

			// TTL 설정 (첫 시도 또는 갱신)
			long ttl = securityProperties.getFingerprint().getTtlSeconds();
			redisTemplate.expire(key, Duration.ofSeconds(ttl));

			log.debug("[FingerprintService] 시도 기록 - visitorId: {}, 성공: {}, 총시도: {}",
				visitorId, success, totalAttempts);
		} catch (Exception e) {
			log.error("[FingerprintService] 시도 기록 실패 - visitorId: {}, 오류: {}", visitorId, e.getMessage());
		}
	}

	/**
	 * Fingerprint 데이터 초기화 (관리자용)
	 *
	 * @param visitorId FingerprintJS visitorId
	 */
	public void resetFingerprint(String visitorId) {
		if (visitorId == null || visitorId.isEmpty()) {
			return;
		}

		String key = REDIS_KEY_PREFIX + visitorId;
		redisTemplate.delete(key);
		log.info("[FingerprintService] Fingerprint 초기화 - visitorId: {}", visitorId);
	}
}
