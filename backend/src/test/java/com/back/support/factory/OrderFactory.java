package com.back.support.factory;

import java.util.UUID;

import com.back.domain.payment.order.entity.Order;
import com.back.domain.payment.order.entity.OrderStatus;
import com.back.domain.ticket.entity.Ticket;

public class OrderFactory extends BaseFactory {

	/**
	 * 기본 Order 생성 (PENDING 상태, 저장 X)
	 */
	public static Order fakeOrder(Ticket ticket, Long amount) {
		return Order.builder()
			.ticket(ticket)
			.amount(amount)
			.status(OrderStatus.PENDING)
			.orderKey(UUID.randomUUID().toString())
			.build();
	}

	/**
	 * PENDING 상태 Order 생성 (저장 X)
	 */
	public static Order fakePendingOrder(Ticket ticket, Long amount) {
		return Order.builder()
			.ticket(ticket)
			.amount(amount)
			.status(OrderStatus.PENDING)
			.orderKey(UUID.randomUUID().toString())
			.build();
	}

	/**
	 * PAID 상태 Order 생성 (저장 X)
	 */
	public static Order fakePaidOrder(Ticket ticket, Long amount, String paymentKey) {
		return Order.builder()
			.ticket(ticket)
			.amount(amount)
			.status(OrderStatus.PAID)
			.orderKey(UUID.randomUUID().toString())
			.paymentKey(paymentKey)
			.build();
	}

	/**
	 * FAILED 상태 Order 생성 (저장 X)
	 */
	public static Order fakeFailedOrder(Ticket ticket, Long amount) {
		return Order.builder()
			.ticket(ticket)
			.amount(amount)
			.status(OrderStatus.FAILED)
			.orderKey(UUID.randomUUID().toString())
			.build();
	}
}
