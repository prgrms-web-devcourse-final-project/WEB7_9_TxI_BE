package com.back.global.observability;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InfraMetrics {

	private final MeterRegistry meterRegistry;

	public Timer paymentGatewayTimer(String status) {
		return Timer.builder("payment.gateway.duration")
			.tags("status", status)
			.register(meterRegistry);
	}

	public Timer redisOperationTimer(String operation) {
		return Timer.builder("redis.queue.operation.duration")
			.tag("operation", operation)
			.register(meterRegistry);
	}
}
