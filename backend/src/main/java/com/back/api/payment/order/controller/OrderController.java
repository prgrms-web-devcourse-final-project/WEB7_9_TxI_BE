package com.back.api.payment.order.controller;

import com.back.domain.payment.order.entity.Order;
import com.back.domain.payment.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.payment.order.dto.request.OrderRequestDto;
import com.back.api.payment.order.dto.response.OrderResponseDto;
import com.back.api.payment.order.service.OrderService;
import com.back.global.http.HttpRequestContext;
import com.back.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class OrderController implements OrderApi {
	private static final Logger log = LoggerFactory.getLogger(OrderController.class);
	private final OrderService orderService;
	private final HttpRequestContext httpRequestContext;

	@PostMapping("/v2/orders/prepare")
	public ApiResponse<OrderResponseDto> createOrder(@RequestBody OrderRequestDto orderRequestDto) {

		Long userId = httpRequestContext.getUser().getId();

		OrderResponseDto orderResponseDto = orderService.createOrder(orderRequestDto, userId);

		return ApiResponse.ok(orderResponseDto);
	}
}
