package com.back.api.event.dto.response;

import com.back.domain.event.entity.EventStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 대시보드용 이벤트 현황 조회 응답")
public record AdminEventDashboardResponse(

	@Schema(description = "이벤트 ID", example = "1")
	Long eventId,

	@Schema(description = "이벤트 제목", example = "2025 BTS 콘서트")
	String title,

	@Schema(description = "이벤트 상태", example = "PRE_OPEN")
	EventStatus status,

	@Schema(description = "현재 사전등록 인원 수", example = "1523")
	Long preRegisterCount,

	@Schema(description = "총 판매 좌석 수", example = "850")
	Long totalSoldSeats,

	@Schema(description = "총 판매 금액", example = "127500000")
	Long totalSalesAmount
) {
	public static AdminEventDashboardResponse of(
		Long eventId,
		String title,
		EventStatus status,
		Long preRegisterCount,
		Long totalSoldSeats,
		Long totalSalesAmount
	) {
		return new AdminEventDashboardResponse(
			eventId,
			title,
			status,
			preRegisterCount,
			totalSoldSeats,
			totalSalesAmount
		);
	}
}
