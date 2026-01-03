package com.back.global.observability;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.back.domain.queue.repository.QueueEntryRedisRepository;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class QueueMetrics {

	private final QueueEntryRedisRepository queueEntryRedisRepository;
	private final MeterRegistry meterRegistry;
	private final Map<Long, Gauge> registeredGauges = new ConcurrentHashMap<>();

	/**
	 * 이벤트별 대기열 Gauge 등록
	 * 이미 등록된 이벤트는 스킵
	 */
	public void registerQueueGauge(Long eventId) {
		registeredGauges.computeIfAbsent(eventId, id ->
			Gauge.builder("queue.entry.waiting.count",
					() -> queueEntryRedisRepository.getTotalWaitingCount(id))
				.tag("eventId", String.valueOf(id))
				.description("Number of users waiting in queue for event")
				.register(meterRegistry)
		);
	}
}
