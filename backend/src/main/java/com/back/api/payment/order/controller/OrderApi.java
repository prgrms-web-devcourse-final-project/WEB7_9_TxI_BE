package com.back.api.payment.order.controller;

import org.springframework.web.bind.annotation.RequestBody;

import com.back.api.payment.order.dto.request.OrderRequestDto;
import com.back.api.payment.order.dto.response.OrderResponseDto;
import com.back.global.config.swagger.ApiErrorCode;
import com.back.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Order API", description = "주문 CRUD API")
public interface OrderApi {
	@Operation(
		summary = "주문 생성 및 결제",
		description = "새로운 주문을 생성합니다. v1에서는 주문과 결제가 통합되어 진행됩니다."
			+ "데이터베이스에 이벤트,유저,좌석이 있어야합니다"
	)
	@ApiErrorCode({
		"NOT_FOUND_EVENT",
		"NOT_FOUND_SEAT",
		"NOT_FOUND_USER",
	})

	ApiResponse<OrderResponseDto> createOrder(@Valid @RequestBody OrderRequestDto orderRequestDto);
}
