package com.back.api.payment.order.service;

import org.springframework.stereotype.Service;

import com.back.domain.payment.order.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {
	private final OrderRepository orderRepository;
}
