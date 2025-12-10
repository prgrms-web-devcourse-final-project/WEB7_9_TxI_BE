package com.back.domain.ticket.entity;

public enum TicketStatus {
	DRAFT,      // 좌석 선택 완료, 결제 대기
	PAID,       // 결제 완료 (선택사항, ISSUED로 바로 가도 됨)
	ISSUED,     // 티켓 발급 완료
	USED,       // 사용 완료
	FAILED,     // 결제 실패
	CANCELLED   // 취소됨
}
