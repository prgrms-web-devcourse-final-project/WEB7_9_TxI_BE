package com.back.api.payment.order.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "주문 생성 요청 DTO ")
public record OrderRequestDto(
	@Schema(description = "주문 총액", example = "25000")
	@NotNull(message = "주문 총액은 필수입니다. 무료는 0원 입니다")
	Long amount,

	@Schema(description = "이벤트 ID", example = "7")
	@NotNull(message = "이벤트 ID는 필수입니다")
	Long eventId,

	@Schema(description = "유저 ID", example = "5")
	@NotNull(message = "유저 ID는 필수입니다")
	Long userId,

	@Schema(description = "좌석 ID", example = "102")
	Long seatId
) {
}
