package com.back.api.preregister.dto.response;

import java.time.LocalDateTime;

import com.back.domain.event.entity.Event;
import com.back.domain.preregister.entity.PreRegister;
import com.back.domain.preregister.entity.PreRegisterStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사전등록 응답")
public record PreRegisterResponse(

	@Schema(description = "사전등록 ID", example = "1")
	Long id,

	@Schema(description = "사용자 ID", example = "1")
	Long userId,

	@Schema(description = "이벤트 ID", example = "1")
	Long eventId,

	@Schema(description = "사전등록 상태", example = "REGISTERED")
	PreRegisterStatus status,

	@Schema(description = "등록일시", example = "2025-12-10T10:00:00")
	LocalDateTime createdAt,

	@Schema(description = "이벤트 이미지 URL", example = "https://example.com/image.jpg")
	String imageUrl,

	@Schema(description = "이벤트 제목", example = "아이유 콘서트")
	String eventTitle,

	@Schema(description = "이벤트 날짜", example = "2025-03-15T19:00:00")
	LocalDateTime eventDate,

	@Schema(description = "이벤트 장소", example = "잠실 올림픽 주경기장")
	String place,

	@Schema(description = "티켓 오픈 날짜", example = "2025-02-20T10:00:00")
	LocalDateTime ticketOpenAt
) {

	public static PreRegisterResponse from(PreRegister preRegister) {
		Event event = preRegister.getEvent();
		return new PreRegisterResponse(
			preRegister.getId(),
			preRegister.getUserId(),
			preRegister.getEventId(),
			preRegister.getPreRegisterStatus(),
			preRegister.getCreateAt(),
			event.getImageUrl(),
			event.getTitle(),
			event.getEventDate(),
			event.getPlace(),
			event.getTicketOpenAt()
		);
	}
}
