package com.back.api.payment.payment.dto.response;

import java.time.LocalDateTime;

import com.back.domain.payment.order.entity.Order;
import com.back.domain.payment.order.entity.OrderStatus;
import com.back.domain.ticket.entity.Ticket;
import com.back.domain.ticket.entity.TicketStatus;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 결제 완료 영수증 응답 DTO
 * - 결제 완료 화면에 필요한 모든 정보 제공
 */
public record PaymentReceiptResponse(
	@Schema(description = "주문 ID", example = "1")
	Long orderId,

	@Schema(description = "주문 키 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
	String orderKey,

	@Schema(description = "주문 번호", example = "WF4840318933")
	String orderNumber,

	@Schema(description = "결제 키 (PG사 제공)", example = "mock_payment_key_123")
	String paymentKey,

	@Schema(description = "결제 일시", example = "2025-03-15T14:30:00")
	LocalDateTime paidAt,

	@Schema(description = "주문 상태", example = "PAID")
	OrderStatus orderStatus,

	@Schema(description = "결제 금액", example = "100000")
	Long amount,

	@Schema(description = "티켓 ID", example = "1")
	Long ticketId,

	@Schema(description = "티켓 상태", example = "ISSUED")
	TicketStatus ticketStatus,

	@Schema(description = "이벤트 ID", example = "5")
	Long eventId,

	@Schema(description = "이벤트 제목", example = "2024 콘서트")
	String eventTitle,

	@Schema(description = "이벤트 장소", example = "서울 올림픽 체조경기장")
	String eventPlace,

	@Schema(description = "이벤트 진행일", example = "2025년 3월 15일")
	LocalDateTime eventDate,

	@Schema(description = "좌석 ID", example = "903")
	Long seatId,

	@Schema(description = "좌석 코드", example = "A1")
	String seatCode,

	@Schema(description = "좌석 등급", example = "VIP")
	String seatGrade,

	@Schema(description = "좌석 가격", example = "100000")
	Integer seatPrice,

	@Schema(description = "결제 수단", example = "신용카드")
	String paymentMethod
) {
	public static PaymentReceiptResponse from(Order order, Ticket ticket) {
		return new PaymentReceiptResponse(
			order.getId(),
			order.getOrderKey(),
			order.getOrderNumber(),
			order.getPaymentKey(),
			order.getPaidAt(),
			order.getStatus(),
			order.getAmount(),
			ticket.getId(),
			ticket.getTicketStatus(),
			ticket.getEvent().getId(),
			ticket.getEvent().getTitle(),
			ticket.getEvent().getPlace(),
			ticket.getEvent().getEventDate(),
			ticket.getSeat().getId(),
			ticket.getSeat().getSeatCode(),
			ticket.getSeat().getGrade().getDisplayName(),
			ticket.getSeat().getPrice(),
			"신용카드" // 결제 수단
		);
	}
}
