package com.back.api.payment.order.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.payment.order.dto.request.OrderRequestDto;
import com.back.api.payment.order.dto.response.OrderResponseDto;
import com.back.api.payment.order.service.OrderService;
import com.back.domain.payment.order.entity.Order;
import com.back.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class OrderController implements OrderApi {
	private final OrderService orderService;

	@PostMapping("/api/v1/order")
	public ApiResponse<OrderResponseDto> createOrder(@RequestBody OrderRequestDto orderRequestDto) {
		Order order = orderService.createOrder(orderRequestDto);
		OrderResponseDto orderResponseDto = OrderResponseDto.toDto(order);
		return ApiResponse.ok(orderResponseDto);
	}

}
