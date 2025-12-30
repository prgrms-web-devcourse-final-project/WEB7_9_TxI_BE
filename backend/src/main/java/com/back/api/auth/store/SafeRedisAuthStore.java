package com.back.api.auth.store;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;

import com.back.api.auth.dto.cache.RefreshTokenCache;
import com.back.domain.auth.repository.RefreshTokenRedisRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SafeRedisAuthStore implements AuthStore {

	private static final String CB_NAME = "redisAuth";
	private static final long COOL_DOWN_MS = 3000;

	private final RefreshTokenRedisRepository redisRepository;
	private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

	// isAvailable()을 매번 ping 하면 비용이 있으니, 최근 실패시간 기반으로 빠르게 false 처리
	private final AtomicLong lastFailureEpochMs = new AtomicLong(0);

	private CircuitBreaker circuitBreaker() {
		return circuitBreakerFactory.create(CB_NAME);
	}

	@Override
	public Optional<RefreshTokenCache> findRefreshCache(long userId) {
		return circuitBreaker().run(
			() -> redisRepository.find(userId),
			throwable -> {
				markFailure(throwable, "find", userId);
				return Optional.empty();
			}
		);
	}

	@Override
	public void saveRefreshCache(long userId, RefreshTokenCache value, Duration ttl) {
		circuitBreaker().run(
			() -> {
				redisRepository.save(userId, value, ttl);
				return null;
			},
			throwable -> {
				markFailure(throwable, "save", userId);
				return null;
			}
		);
	}

	@Override
	public void deleteRefreshCache(long userId) {
		circuitBreaker().run(
			() -> {
				redisRepository.delete(userId);
				return null;
			},
			throwable -> {
				markFailure(throwable, "delete", userId);
				return null;
			}
		);
	}

	@Override
	public boolean isAvailable() {
		// 최근에 장애가 났으면 잠깐 false로 보고 빠르게 반환 (ex: 3초)
		long lastFail = lastFailureEpochMs.get();
		long now = System.currentTimeMillis();
		if (lastFail > 0 && (now - lastFail) < COOL_DOWN_MS) {
			return false;
		}
		// circuit breaker 가 run 에 성공하면 true 반환
		return circuitBreaker().run(
			() -> true,
			throwable -> {
				markFailure(throwable, "available-check", -1L);
				return false;
			}
		);
	}

	private void markFailure(Throwable throwable, String op, long userId) {
		lastFailureEpochMs.set(System.currentTimeMillis());
		if (userId >= 0) {
			log.warn("Redis op failed. op={}, userId={}, cause={}", op, userId, throwable.toString());
		} else {
			log.warn("Redis op failed. op={}, cause={}", op, throwable.toString());
		}
	}
}
