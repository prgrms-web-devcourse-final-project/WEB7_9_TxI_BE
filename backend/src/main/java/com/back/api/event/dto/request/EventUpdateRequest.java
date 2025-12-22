package com.back.api.event.dto.request;

import java.time.LocalDateTime;

import com.back.domain.event.entity.EventCategory;
import com.back.domain.event.entity.EventStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "이벤트 수정 요청 DTO")
public record EventUpdateRequest(
	@Schema(description = "이벤트 제목", example = "2026 서울 뮤직 페스티벌 (수정)")
	@NotBlank(message = "이벤트 제목은 필수입니다.")
	String title,

	@Schema(description = "이벤트 카테고리", example = "CONCERT")
	@NotNull(message = "카테고리는 필수입니다.")
	EventCategory category,

	@Schema(description = "이벤트 상세 설명", example = "세계 최고의 아티스트들이 모이는 뮤직 페스티벌입니다.")
	String description,

	@Schema(description = "이벤트 장소", example = "잠실 올림픽 주경기장")
	@NotBlank(message = "장소는 필수입니다.")
	String place,

	@Schema(description = "이벤트 이미지 URL", example = "https://example.com/images/event1.jpg")
	String imageUrl,

	@Schema(description = "최소 티켓 가격 (원)", example = "30000")
	@NotNull(message = "최소 가격은 필수입니다.")
	@Min(value = 0, message = "최소 가격은 0 이상이어야 합니다.")
	Integer minPrice,

	@Schema(description = "최대 티켓 가격 (원)", example = "150000")
	@NotNull(message = "최대 가격은 필수입니다.")
	@Min(value = 0, message = "최대 가격은 0 이상이어야 합니다.")
	Integer maxPrice,

	@Schema(description = "사전등록 시작일시", example = "2026-01-11T10:00:00")
	@NotNull(message = "사전등록 시작일은 필수입니다.")
	LocalDateTime preOpenAt,

	@Schema(description = "사전등록 종료일시", example = "2026-01-20T23:59:59")
	@NotNull(message = "사전등록 종료일은 필수입니다.")
	LocalDateTime preCloseAt,

	@Schema(description = "티켓팅 시작일시", example = "2026-01-25T10:00:00")
	@NotNull(message = "티켓팅 시작일은 필수입니다.")
	LocalDateTime ticketOpenAt,

	@Schema(description = "티켓팅 종료일시", example = "2026-01-30T23:59:59")
	@NotNull(message = "티켓팅 종료일은 필수입니다.")
	LocalDateTime ticketCloseAt,

	@Schema(description = "이벤트 날짜 (실제 이벤트 개최일)", example = "2026-02-15T19:00:00")
	@NotNull(message = "이벤트 날짜는 필수입니다.")
	LocalDateTime eventDate,

	@Schema(description = "최대 티켓 수량", example = "5000")
	@NotNull(message = "최대 티켓 수량은 필수입니다.")
	@Min(value = 1, message = "최대 티켓 수량은 1 이상이어야 합니다.")
	Integer maxTicketAmount,

	@Schema(description = "이벤트 상태", example = "PRE_OPEN")
	@NotNull(message = "이벤트 상태는 필수입니다.")
	EventStatus status
) {
}
