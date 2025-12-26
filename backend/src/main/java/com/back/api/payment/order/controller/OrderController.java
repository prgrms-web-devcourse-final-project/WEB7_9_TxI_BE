package com.back.api.payment.order.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.payment.order.dto.request.OrderRequestDto;
import com.back.api.payment.order.dto.response.OrderResponseDto;
import com.back.api.payment.order.dto.response.V2_OrderResponseDto;
import com.back.api.payment.order.service.OrderService;
import com.back.global.http.HttpRequestContext;
import com.back.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class OrderController implements OrderApi {
	private final OrderService orderService;
	private final HttpRequestContext httpRequestContext;

	@PostMapping("/v1/order")
	public ApiResponse<OrderResponseDto> createOrder(@RequestBody OrderRequestDto orderRequestDto) {

		Long userId = httpRequestContext.getUser().getId();

		OrderResponseDto orderResponseDto = orderService.createOrder(orderRequestDto, userId);
		return ApiResponse.ok("주문이 생성되었습니다",orderResponseDto);
	}

	@PostMapping("/v2/orders/prepare")
	public ApiResponse<V2_OrderResponseDto> v2_createOrder(@RequestBody OrderRequestDto orderRequestDto) {

		Long userId = httpRequestContext.getUser().getId();

		V2_OrderResponseDto orderResponseDto = orderService.v2_createOrder(orderRequestDto, userId);

		return ApiResponse.ok("주문이 생성되었습니다",orderResponseDto);
	}
}
