package com.back.support.helper;

import org.springframework.stereotype.Component;

import com.back.domain.payment.order.entity.Order;
import com.back.domain.payment.order.entity.V2_Order;
import com.back.domain.payment.order.repository.OrderRepository;
import com.back.domain.payment.order.repository.V2_OrderRepository;
import com.back.domain.ticket.entity.Ticket;
import com.back.support.factory.OrderFactory;

@Component
public class OrderHelper {

	private final OrderRepository orderRepository;
	private final V2_OrderRepository v2OrderRepository;

	public OrderHelper(OrderRepository orderRepository, V2_OrderRepository v2OrderRepository) {
		this.orderRepository = orderRepository;
		this.v2OrderRepository = v2OrderRepository;
	}

	/**
	 * PENDING 상태 Order 저장
	 */
	public Order createPendingOrder(Ticket ticket, Long amount) {
		Order order = OrderFactory.fakePendingOrder(ticket, amount);
		return orderRepository.save(order);
	}

	/**
	 * PAID 상태 Order 저장
	 */
	public Order createPaidOrder(Ticket ticket, Long amount, String paymentKey) {
		Order order = OrderFactory.fakePaidOrder(ticket, amount, paymentKey);
		return orderRepository.save(order);
	}

	/**
	 * FAILED 상태 Order 저장
	 */
	public Order createFailedOrder(Ticket ticket, Long amount) {
		Order order = OrderFactory.fakeFailedOrder(ticket, amount);
		return orderRepository.save(order);
	}

	public void clearOrders() {
		orderRepository.deleteAll();
	}

	// ===== V2_Order 헬퍼 메서드 =====

	/**
	 * V2 PENDING 상태 Order 저장
	 */
	public V2_Order createV2PendingOrder(Ticket ticket, Long amount) {
		V2_Order order = OrderFactory.fakeV2PendingOrder(ticket, amount);
		return v2OrderRepository.save(order);
	}

	/**
	 * V2 PAID 상태 Order 저장
	 */
	public V2_Order createV2PaidOrder(Ticket ticket, Long amount, String paymentKey) {
		V2_Order order = OrderFactory.fakeV2PaidOrder(ticket, amount, paymentKey);
		return v2OrderRepository.save(order);
	}

	/**
	 * V2 FAILED 상태 Order 저장
	 */
	public V2_Order createV2FailedOrder(Ticket ticket, Long amount) {
		V2_Order order = OrderFactory.fakeV2FailedOrder(ticket, amount);
		return v2OrderRepository.save(order);
	}

	public void clearV2Orders() {
		v2OrderRepository.deleteAll();
	}
}
