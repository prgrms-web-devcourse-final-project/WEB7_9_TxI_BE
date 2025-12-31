package com.back.api.ticket.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "QR 검증 응답 DTO")
public record QrValidationResponse(

	@Schema(description = "QR 유효 여부", example = "true")
	boolean isValid,

	@Schema(description = "상태 메세지", example = "QR 코드가 유효합니다.")
	String message,

	@Schema(description = "티켓 ID", example = "1")
	Long ticketId,

	@Schema(description = "이벤트 ID", example = "1")
	Long eventId,

	@Schema(description = "이벤트 제목", example = "2024 콘서트")
	String eventTitle,

	@Schema(description = "좌석 코드", example = "A1")
	String seatCode,

	@Schema(description = "소유자 닉네임", example = "testnick")
	String ownerNickname,

	@Schema(description = "이벤트 일시", example = "2026-01-31T20:00:00")
	LocalDateTime eventDate,

	@Schema(description = "QR 코드 발급 시간", example = "2026-01-31T12:00:00")
	LocalDateTime qrIssuedAt

) {
}
