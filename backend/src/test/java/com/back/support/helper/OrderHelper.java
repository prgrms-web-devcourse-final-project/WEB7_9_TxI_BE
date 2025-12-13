package com.back.support.helper;

import org.springframework.stereotype.Component;

import com.back.domain.payment.order.entity.Order;
import com.back.domain.payment.order.repository.OrderRepository;
import com.back.domain.ticket.entity.Ticket;
import com.back.support.factory.OrderFactory;

@Component
public class OrderHelper {

	private final OrderRepository orderRepository;

	public OrderHelper(OrderRepository orderRepository) {
		this.orderRepository = orderRepository;
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
}
