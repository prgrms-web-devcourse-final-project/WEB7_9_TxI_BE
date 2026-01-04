package com.back.global.observability.metrics;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SchedulerMetrics {

	private final MeterRegistry meterRegistry;

	public Timer schedulerTimer(String jobName) {
		return Timer.builder("scheduler.execution.duration")
			.tag("jobName", jobName)
			.register(meterRegistry);
	}

	public void recordDuration(String jobName, long durationMs) {
		schedulerTimer(jobName).record(durationMs, TimeUnit.MILLISECONDS);
	}
}
