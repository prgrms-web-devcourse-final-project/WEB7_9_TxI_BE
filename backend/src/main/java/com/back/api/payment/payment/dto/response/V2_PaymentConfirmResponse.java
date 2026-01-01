package com.back.api.payment.payment.dto.response;

import java.time.LocalDateTime;

import com.back.domain.payment.order.entity.V2_Order;
import com.back.domain.seat.entity.SeatGrade;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public record V2_PaymentConfirmResponse(
	String orderId,
	boolean success,
	long amount,
	LocalDateTime paidAt,
	String paymentMethod,
	long ticketId,
	String eventTitle,
	String eventPlace,
	LocalDateTime eventDate,
	String seatCode,
	SeatGrade seatGrade
) {
	/**
	 * V2_Order로부터 응답 객체 생성
	 * - 변수 추출로 중복 접근 방지 (getTicket() 1번만 호출)
	 * - N+1 쿼리 방지를 위해 order는 fetch join 필수
	 */
	public static V2_PaymentConfirmResponse from(V2_Order order, boolean success) {
		var ticket = order.getTicket();
		var event = ticket.getEvent();
		var seat = ticket.getSeat();
		var payment = order.getPayment();

		return new V2_PaymentConfirmResponse(
			order.getOrderId(),
			success,
			order.getAmount(),
			ticket.getCreateAt(),
			payment.getMethod(),
			ticket.getId(),
			event.getTitle(),
			event.getPlace(),
			event.getEventDate(),
			seat.getSeatCode(),
			seat.getGrade()
		);
	}

	/**
	 * 간단한 응답 생성 (orderId와 성공 여부만)
	 * - 결제 확인 중복 요청 등에서 사용
	 */
	public V2_PaymentConfirmResponse(String orderId, boolean success) {
		this(orderId, success, 0L, null, null, 0L, null, null, null, null, null);
	}
}
