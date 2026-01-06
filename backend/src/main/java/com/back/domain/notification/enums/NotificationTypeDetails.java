package com.back.domain.notification.enums;

// 세부 타입
public enum NotificationTypeDetails {
	// QueueEntries
	TICKETING_POSSIBLE,          // 대기열 순서 완료
	TICKETING_EXPIRED, 			 // 대기열 만료

	// PAYMENT
	PAYMENT_SUCCESS,
	PAYMENT_FAILED,

	// PRE_REGISTER
	PRE_REGISTER_DONE,

	// TICKET
	TICKET_GET
}

