package com.back.global.logging;

import org.slf4j.MDC;

public final class MdcContext {

	private MdcContext() {
	}

	public static void putUserId(Long userId) {
		if (userId != null) {
			MDC.put("userId", String.valueOf(userId));
		}
	}

	public static void removeUserId() {
		MDC.remove("userId");
	}
}