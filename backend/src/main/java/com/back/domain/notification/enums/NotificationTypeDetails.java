package com.back.domain.notification.enums;

// 세부 타입
public enum NotificationTypeDetails {
	// 알림
	ENTER_NOW,          // 대기열 순서 완료
	TICKET_ISSUED,      // 티켓 발급 완료
	EVENT_OPEN,         // 이벤트 생성

	// 결제
	PAYMENT_SUCCESS,
	PAYMENT_FAILED,

	// 사전등록
	PRE_REGISTER_DONE;
}

