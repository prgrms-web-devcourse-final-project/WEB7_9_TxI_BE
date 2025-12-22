package com.back.api.event.dto.response;

import java.time.LocalDateTime;

import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventCategory;
import com.back.domain.event.entity.EventStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이벤트 상세 응답 DTO")
public record EventResponse(
	@Schema(description = "이벤트 ID", example = "1")
	Long id,

	@Schema(description = "이벤트 제목", example = "2026 서울 뮤직 페스티벌")
	String title,

	@Schema(description = "이벤트 카테고리", example = "CONCERT")
	EventCategory category,

	@Schema(description = "이벤트 상세 설명", example = "세계 최고의 아티스트들이 모이는 뮤직 페스티벌입니다.")
	String description,

	@Schema(description = "이벤트 장소", example = "잠실 올림픽 주경기장")
	String place,

	@Schema(description = "이벤트 이미지 URL", example = "https://example.com/images/event1.jpg")
	String imageUrl,

	@Schema(description = "최소 티켓 가격 (원)", example = "30000")
	Integer minPrice,

	@Schema(description = "최대 티켓 가격 (원)", example = "150000")
	Integer maxPrice,

	@Schema(description = "사전등록 시작일시", example = "2026-01-11T10:00:00")
	LocalDateTime preOpenAt,

	@Schema(description = "사전등록 종료일시", example = "2026-01-20T23:59:59")
	LocalDateTime preCloseAt,

	@Schema(description = "티켓팅 시작일시", example = "2026-01-25T10:00:00")
	LocalDateTime ticketOpenAt,

	@Schema(description = "티켓팅 종료일시", example = "2026-01-30T23:59:59")
	LocalDateTime ticketCloseAt,

	@Schema(description = "이벤트 날짜 (실제 이벤트 개최일)", example = "2026-02-15T19:00:00")
	LocalDateTime eventDate,

	@Schema(description = "최대 티켓 수량", example = "5000")
	Integer maxTicketAmount,

	@Schema(description = "이벤트 상태 (READY: 준비중, PRE_OPEN: 사전등록중, QUEUE_READY: 대기열 준비, OPEN: 티켓팅 진행중, CLOSED: 마감)",
		example = "PRE_OPEN")
	EventStatus status
) {
	public static EventResponse from(Event event) {
		return new EventResponse(
			event.getId(),
			event.getTitle(),
			event.getCategory(),
			event.getDescription(),
			event.getPlace(),
			event.getImageUrl(),
			event.getMinPrice(),
			event.getMaxPrice(),
			event.getPreOpenAt(),
			event.getPreCloseAt(),
			event.getTicketOpenAt(),
			event.getTicketCloseAt(),
			event.getEventDate(),
			event.getMaxTicketAmount(),
			event.getStatus()
		);
	}
}
