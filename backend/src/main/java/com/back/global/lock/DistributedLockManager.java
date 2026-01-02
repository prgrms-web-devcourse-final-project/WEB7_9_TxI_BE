package com.back.global.lock;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import com.back.global.error.code.SeatErrorCode;
import com.back.global.error.exception.ErrorException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redisson 분산 락 관리자
 * - 비즈니스 로직과 분산 락 처리를 분리
 * - 락 획득 → 트랜잭션 실행 → 트랜잭션 커밋 → 락 해제 순서 보장
 * - 성능 최적화: 락 대기/보유 시간 최소화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLockManager {

	private final RedissonClient redissonClient;

	// 성능 최적화: 락 시간 대폭 단축
	private static final long LOCK_WAIT_TIME_MS = 100L;  // 100ms (기존 3초 → 100ms)
	private static final long LOCK_LEASE_TIME_MS = 500L; // 500ms (기존 5초 → 500ms)

	/**
	 * 분산 락을 획득하고 비즈니스 로직 실행
	 * @param lockKey 락 키
	 * @param task 실행할 비즈니스 로직 (트랜잭션 메서드)
	 * @return 비즈니스 로직 실행 결과
	 */
	public <T> T executeWithLock(String lockKey, Supplier<T> task) {
		RLock lock = redissonClient.getLock(lockKey);

		try {
			// 락 획득 시도 (짧은 대기 시간으로 빠른 실패)
			boolean acquired = lock.tryLock(LOCK_WAIT_TIME_MS, LOCK_LEASE_TIME_MS, TimeUnit.MILLISECONDS);
			if (!acquired) {
				log.warn("Failed to acquire lock. lockKey={}", lockKey);
				throw new ErrorException(SeatErrorCode.SEAT_LOCK_ACQUISITION_FAILED);
			}

			// 락 획득 후 비즈니스 로직 실행 (트랜잭션 처리됨)
			return task.get();

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Lock acquisition interrupted. lockKey={}", lockKey, e);
			throw new ErrorException(SeatErrorCode.SEAT_LOCK_INTERRUPTED);
		} finally {
			// 락 해제 (현재 스레드가 보유한 경우만)
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	/**
	 * void 반환 메서드용 락 실행
	 */
	public void executeWithLock(String lockKey, Runnable task) {
		executeWithLock(lockKey, () -> {
			task.run();
			return null;
		});
	}

	/**
	 * 좌석 락 키 생성
	 */
	public static String generateSeatLockKey(Long eventId, Long seatId) {
		return "seat:lock:" + eventId + ":" + seatId;
	}
}
