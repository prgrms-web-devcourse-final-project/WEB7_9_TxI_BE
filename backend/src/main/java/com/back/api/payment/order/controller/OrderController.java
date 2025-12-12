package com.back.api.payment.order.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.payment.order.dto.request.OrderRequestDto;
import com.back.api.payment.order.dto.response.OrderResponseDto;
import com.back.api.payment.order.service.OrderService;
import com.back.global.http.HttpRequestContext;
import com.back.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class OrderController implements OrderApi {
	private final OrderService orderService;
	private final HttpRequestContext httpRequestContext;

	@PostMapping("/api/v1/order")
	public ApiResponse<OrderResponseDto> createOrder(@RequestBody OrderRequestDto orderRequestDto) {

		Long userId = httpRequestContext.getUser().getId();

		OrderResponseDto orderResponseDto = orderService.createOrder(orderRequestDto, userId);
		return ApiResponse.ok(orderResponseDto);
	}

}
