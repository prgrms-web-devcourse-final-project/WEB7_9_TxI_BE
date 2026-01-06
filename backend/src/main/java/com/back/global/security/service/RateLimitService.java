package com.back.global.security.service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.back.global.properties.SecurityProperties;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Rate Limiting 서비스 (SMS 전용)
 *
 * 목적:
 * - SMS API 비용 발생 방지
 * - 무한 요청 봇 차단
 *
 * Bucket4j 기반 Token Bucket 알고리즘 사용:
 * - 버킷에 토큰이 채워지고, 요청마다 토큰 소비
 * - 토큰이 없으면 요청 거부
 * - 시간이 지나면 토큰 자동 리필
 *
 * Rate Limit 정책:
 * - SMS API: IP + 전화번호 조합으로 1분당 7회 제한
 *
 * Redis 저장 구조:
 * - Key: "rate_limit:sms:{ip}:{phoneHash}"
 * - Type: String (Bucket4j 직렬화된 상태)
 *
 * 예외 처리:
 * - Redis 장애 시 Rate Limit 해제 (가용성 우선)
 * - 로그만 남기고 요청은 통과시킴
 */
@Slf4j
@Profile("!test")
@Service
@RequiredArgsConstructor
@ConditionalOnBean(LettuceBasedProxyManager.class)
public class RateLimitService {

	private final StringRedisTemplate redisTemplate;
	private final SecurityProperties securityProperties;
	private final LettuceBasedProxyManager<String> proxyManager;

	/**
	 * SMS API Rate Limit 확인
	 *
	 * 키 구성: IP + 전화번호 해시
	 * - 같은 IP에서 여러 사용자가 접속해도 전화번호별로 독립적인 제한
	 *
	 * @param ip          클라이언트 IP
	 * @param phoneNumber 전화번호 (해시 처리됨)
	 * @return 허용 여부
	 */
	public boolean allowSmsRequest(String ip, String phoneNumber) {
		if (!securityProperties.getRateLimit().isEnabled()) {
			return true;
		}

		// IP + 전화번호 해시로 키 구성
		String phoneHash = hashPhoneNumber(phoneNumber);
		String key = "rate_limit:sms:" + ip + ":" + phoneHash;
		int limit = securityProperties.getRateLimit().getSmsPerMinute();

		return checkRateLimit(key, limit, Duration.ofMinutes(1));
	}

	/**
	 * Rate Limit 체크 (Bucket4j Token Bucket 알고리즘)
	 *
	 * @param key      Redis 키
	 * @param limit    제한 횟수
	 * @param duration 시간 단위
	 * @return 허용 여부
	 */
	private boolean checkRateLimit(String key, int limit, Duration duration) {
		try {
			// Bucket 설정
			BucketConfiguration configuration = BucketConfiguration.builder()
				.addLimit(Bandwidth.classic(limit, Refill.intervally(limit, duration)))
				.build();

			// Redis 기반 Bucket 생성
			Bucket bucket = proxyManager.builder().build(key, configuration);

			// 토큰 소비 시도
			boolean consumed = bucket.tryConsume(1);

			if (!consumed) {
				log.warn("[RateLimitService] Rate Limit 초과 - Key: {}, Limit: {}/{}", key, limit, duration);
			}

			return consumed;
		} catch (Exception e) {
			// Redis 장애 시 요청 허용 (가용성 우선)
			log.error("[RateLimitService] Rate Limit 확인 실패 - Key: {}, 오류: {} - 요청 허용", key, e.getMessage());
			return true;
		}
	}

	/**
	 * Rate Limit Salt (고정값)
	 * - Rainbow Table 공격 방지
	 * - 애플리케이션 재시작 시에도 동일한 값 유지 (Rate Limit 일관성)
	 * - 보안: application.yml에서 환경 변수로 주입 권장
	 */
	private static final String RATE_LIMIT_SALT = "TxI_RateLimit_Salt_2025";

	/**
	 * 전화번호 해시 처리
	 * - 개인정보 보호를 위해 전화번호를 직접 저장하지 않음
	 * - Rainbow Table 공격 방지를 위해 고정 Salt 사용
	 *
	 * 보안 고려사항:
	 * - 단순 hashCode()는 Rainbow Table 공격에 취약
	 * - 고정 Salt 추가로 원본 전화번호 유추 방지
	 * - Rate Limit 용도이므로 암호학적 해시(SHA-256)는 불필요 (성능 우선)
	 *
	 * @param phoneNumber 전화번호
	 * @return 해시값
	 */
	private String hashPhoneNumber(String phoneNumber) {
		if (phoneNumber == null) {
			return "null";
		}
		// 고정 Salt를 추가한 해시 (Rainbow Table 공격 방지)
		String saltedValue = phoneNumber + RATE_LIMIT_SALT;
		return String.valueOf(saltedValue.hashCode());
	}

	/**
	 * 남은 토큰 개수 확인 (디버깅/모니터링용)
	 *
	 * @param key Redis 키
	 * @return 남은 토큰 개수 (-1: 에러)
	 */
	public long getRemainingTokens(String key, int limit, Duration duration) {
		try {
			BucketConfiguration configuration = BucketConfiguration.builder()
				.addLimit(Bandwidth.classic(limit, Refill.intervally(limit, duration)))
				.build();

			Bucket bucket = proxyManager.builder().build(key, configuration);
			return bucket.getAvailableTokens();
		} catch (Exception e) {
			log.error("[RateLimitService] 토큰 확인 실패 - Key: {}, 오류: {}", key, e.getMessage());
			return -1;
		}
	}
}
