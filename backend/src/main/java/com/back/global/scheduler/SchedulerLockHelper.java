package com.back.global.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Component;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 동적 스케줄러용 분산 락 헬퍼

@Component
@RequiredArgsConstructor
@Slf4j
public class SchedulerLockHelper {

	private final LockProvider lockProvider;

	public boolean executeWithLock(
		String lockName,
		Runnable task,
		Duration lockAtMostFor, //최대 락 유지 시간
		Duration lockAtLeastFor // 최소 락 유지 시간
	) {
		Instant now = Instant.now();

		LockConfiguration lockConfig = new LockConfiguration(
			now,
			lockName,
			lockAtMostFor,
			lockAtLeastFor
		);

		Optional<SimpleLock> lock = lockProvider.lock(lockConfig);

		if (lock.isEmpty()) {
			return false;
		}

		try {
			task.run();
			return true;
		} finally {
			lock.get().unlock();
		}
	}

	public boolean executeWithLock(String lockName, Runnable task) {
		return executeWithLock(
			lockName,
			task,
			Duration.ofMinutes(2),
			Duration.ofSeconds(10)
		);
	}
}
