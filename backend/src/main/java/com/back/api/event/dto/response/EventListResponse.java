package com.back.api.event.dto.response;

import java.time.LocalDateTime;

import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventCategory;
import com.back.domain.event.entity.EventStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이벤트 목록 응답 DTO")
public record EventListResponse(
	@Schema(description = "이벤트 ID", example = "1")
	Long id,

	@Schema(description = "이벤트 제목", example = "2025 서울 뮤직 페스티벌")
	String title,

	@Schema(description = "이벤트 카테고리", example = "CONCERT")
	EventCategory category,

	@Schema(description = "이벤트 장소", example = "잠실 올림픽 주경기장")
	String place,

	@Schema(description = "이벤트 이미지 URL", example = "https://example.com/images/event1.jpg")
	String imageUrl,

	@Schema(description = "최소 티켓 가격 (원)", example = "30000")
	Integer minPrice,

	@Schema(description = "최대 티켓 가격 (원)", example = "150000")
	Integer maxPrice,

	@Schema(description = "사전등록 시작일시", example = "2025-01-01T10:00:00")
	LocalDateTime preOpenAt,

	@Schema(description = "사전등록 종료일시", example = "2025-01-10T23:59:59")
	LocalDateTime preCloseAt,

	@Schema(description = "티켓팅 시작일시", example = "2025-01-15T10:00:00")
	LocalDateTime ticketOpenAt,

	@Schema(description = "이벤트 상태 (READY: 준비중, PRE_OPEN: 사전등록중, QUEUE_READY: 대기열 준비, OPEN: 티켓팅 진행중, CLOSED: 마감)",
		example = "PRE_OPEN")
	EventStatus status,

	@Schema(description = "생성일시", example = "2024-12-01T10:00:00")
	LocalDateTime createdAt
) {
	public static EventListResponse from(Event event) {
		return new EventListResponse(
			event.getId(),
			event.getTitle(),
			event.getCategory(),
			event.getPlace(),
			event.getImageUrl(),
			event.getMinPrice(),
			event.getMaxPrice(),
			event.getPreOpenAt(),
			event.getPreCloseAt(),
			event.getTicketOpenAt(),
			event.getStatus(),
			event.getCreateAt()
		);
	}
}
