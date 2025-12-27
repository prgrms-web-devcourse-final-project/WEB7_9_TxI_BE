package com.back.api.payment.payment.dto.response;

import com.back.domain.payment.payment.entity.ApproveStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

// 백엔드 <-> 토스페이먼츠 API 서버 : 결제 승인 응답 DTO
public record TossPaymentResponse(
	@JsonProperty("paymentKey")
	String paymentKey,

	@JsonProperty("status")
	ApproveStatus status,

	@JsonProperty("method")
	String method,

	@JsonProperty("totalAmount")
	Long totalAmount,

	@JsonProperty("approvedAt")
	String approvedAt
) {

}
