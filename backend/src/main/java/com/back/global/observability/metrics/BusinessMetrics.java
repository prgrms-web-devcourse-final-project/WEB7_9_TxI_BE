package com.back.global.observability.metrics;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BusinessMetrics {
	private final MeterRegistry meterRegistry;

	public void seatSelectionSuccess(Long eventId) {
		meterRegistry.counter(
			"seat.selection.success",
			"eventId", String.valueOf(eventId)
		).increment();
	}

	public void seatSelectionFailure(Long eventId, String reason) {
		meterRegistry.counter(
			"seat.selection.failure",
			"eventId", String.valueOf(eventId),
			"reason", reason
		).increment();
	}

	public void seatConcurrencyConflict(Long eventId) {
		meterRegistry.counter(
			"seat.concurrency.conflicts",
			"eventId", String.valueOf(eventId)
		).increment();
	}

	/* =========== payment =========== */

	public void paymentConfirmSuccess(Long eventId) {
		meterRegistry.counter(
			"payment.confirm.success",
			"eventId", String.valueOf(eventId)
		).increment();
	}

	public void paymentConfirmFailure(String reason) {
		meterRegistry.counter(
			"payment.confirm.failure",
			"reason", reason
		).increment();
	}

	/* =========== ticket =========== */

	public void draftTicketExpired(Long eventId) {
		meterRegistry.counter(
			"ticket.draft.expiration.count",
			"eventId", String.valueOf(eventId)
		).increment();
	}
}
