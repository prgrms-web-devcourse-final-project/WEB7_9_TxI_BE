package com.back.global.observability;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ErrorMetrics {

	private final MeterRegistry meterRegistry;

	public void applicationError(String errorCode, int httpStatus) {
		meterRegistry.counter(
			"application.errors",
			"errorCode", errorCode,
			"httpStatus", String.valueOf(httpStatus)
		).increment();
	}

	public void circuitBreakerOpen(String target) {
		meterRegistry.counter(
			"circuit.breaker.open",
			"target", target
		).increment();
	}

	public void dbTransactionRollback(String reason) {
		meterRegistry.counter(
			"db.transaction.rollback",
			"reason", reason
		).increment();
	}
}
