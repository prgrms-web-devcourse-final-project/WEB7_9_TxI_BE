package com.back.global.observability;

import org.slf4j.MDC;

public final class MdcContext {

	private MdcContext() {
	}

	public static void putUserId(Long userId) {
		if (userId != null) {
			MDC.put("userId", String.valueOf(userId));
		}
	}

	public static void putEventId(Long eventId) {
		if (eventId != null) {
			MDC.put("eventId", String.valueOf(eventId));
		}
	}

	public static void putSeatId(Long seatId) {
		if (seatId != null) {
			MDC.put("seatId", String.valueOf(seatId));
		}
	}

	public static void putRunId(String runId) {
		if (runId != null && !runId.isBlank()) {
			MDC.put("runId", runId);
		}
	}

	public static void removeUserId() {
		MDC.remove("userId");
	}

	public static void removeEventId() {
		MDC.remove("eventId");
	}

	public static void removeSeatId() {
		MDC.remove("seatId");
	}

	public static void removeRunId() {
		MDC.remove("runId");
	}

	public static void clearAll() {
		MDC.clear();
	}
}
