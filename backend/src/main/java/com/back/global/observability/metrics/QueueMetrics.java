package com.back.global.observability.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.back.domain.queue.repository.QueueEntryRedisRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class QueueMetrics {

	private final QueueEntryRedisRepository queueEntryRedisRepository;
	private final MeterRegistry meterRegistry;
	private final Map<Long, Gauge> registeredWaitingGauges = new ConcurrentHashMap<>();
	private final Map<Long, Gauge> registeredEnteredGauges = new ConcurrentHashMap<>();

	/**
	 * 이벤트별 대기열 Gauge 등록
	 * 이미 등록된 이벤트는 스킵
	 */
	public void registerQueueGauge(Long eventId) {
		// WAITING 상태 Gauge 등록
		registeredWaitingGauges.computeIfAbsent(eventId, id ->
			Gauge.builder("queue.waiting.count",
					() -> queueEntryRedisRepository.getTotalWaitingCount(id))
				.tag("eventId", String.valueOf(id))
				.description("Number of users waiting in queue for event")
				.register(meterRegistry)
		);

		// ENTERED 상태 Gauge 등록
		registeredEnteredGauges.computeIfAbsent(eventId, id ->
			Gauge.builder("queue.entered.count",
					() -> queueEntryRedisRepository.getTotalEnteredCount(id))
				.tag("eventId", String.valueOf(id))
				.description("Number of users entered queue for event")
				.register(meterRegistry)
		);
	}

	/**
	 * WAITING → ENTERED 전환 성공
	 */
	public void queueEntrySuccess(Long eventId) {
		Counter.builder("queue.entry.success.count")
			.tag("eventId", String.valueOf(eventId))
			.description("Number of successful queue entries (WAITING -> ENTERED)")
			.register(meterRegistry)
			.increment();
	}

	/**
	 * ENTERED → EXPIRED 전환
	 */
	public void queueExpiration(Long eventId) {
		Counter.builder("queue.expiration.count")
			.tag("eventId", String.valueOf(eventId))
			.description("Number of queue expirations (ENTERED -> EXPIRED)")
			.register(meterRegistry)
			.increment();
	}

	/**
	 * ENTERED → COMPLETED 전환 (결제 완료)
	 */
	public void queueCompletion(Long eventId) {
		Counter.builder("queue.completion.count")
			.tag("eventId", String.valueOf(eventId))
			.description("Number of queue completions (ENTERED -> COMPLETED)")
			.register(meterRegistry)
			.increment();
	}

	/**
	 * ENTERED → WAITING 전환 (뒤로 보내기)
	 */
	public void queueMoveBack(Long eventId) {
		Counter.builder("queue.move.back.count")
			.tag("eventId", String.valueOf(eventId))
			.description("Number of queue move backs (ENTERED -> WAITING)")
			.register(meterRegistry)
			.increment();
	}

	/**
	 * 배치 처리 실행
	 */
	public void queueBatchProcessing(Long eventId) {
		Counter.builder("queue.batch.processing.count")
			.tag("eventId", String.valueOf(eventId))
			.description("Number of batch processing executions")
			.register(meterRegistry)
			.increment();
	}

	/**
	 * 배치당 입장 인원수 기록
	 */
	public void queueBatchEntrySize(Long eventId, int entryCount) {
		DistributionSummary.builder("queue.batch.entry.size")
			.tag("eventId", String.valueOf(eventId))
			.description("Number of users entered per batch")
			.register(meterRegistry)
			.record(entryCount);
	}
}
