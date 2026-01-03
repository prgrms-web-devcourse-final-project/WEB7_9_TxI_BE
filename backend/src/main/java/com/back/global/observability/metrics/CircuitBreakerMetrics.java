package com.back.global.observability.metrics;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CircuitBreakerMetrics {

	private final MeterRegistry meterRegistry;
	private final AtomicInteger state = new AtomicInteger(0);

	@PostConstruct
	void register() {
		Gauge.builder("circuit.breaker.state", state, AtomicInteger::get)
			.description("0=CLOSED, 1=OPEN, 2=HALF_OPEN")
			.register(meterRegistry);
	}

	public void closed() {
		state.set(0);
	}

	public void open() {
		state.set(1);
	}

	public void halfOpen() {
		state.set(2);
	}

}
