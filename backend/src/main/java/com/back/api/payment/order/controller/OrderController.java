package com.back.api.payment.order.controller;

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
		log.info("Create order ing~~~~");
		Long userId = httpRequestContext.getUser().getId();
		log.info("%d 번 유저가 주문을 시작하였습니다",userId);

		OrderResponseDto orderResponseDto = orderService.createOrder(orderRequestDto, userId);
		return ApiResponse.ok(orderResponseDto);
	}

	@GetMapping("/v2/orders/test")
	public ApiResponse<String> test(){
		log.info("test");
		return ApiResponse.ok("okok");
	}

}
