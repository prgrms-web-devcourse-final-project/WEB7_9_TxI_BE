package com.back.domain.notification.systemEvent;

import com.back.domain.notification.enums.NotificationTypes;

public class OrdersMessage extends NotificationMessage {
	private final String orderNumber;
	private final Long amount;

	public OrdersMessage(Long userId, Long orderId, String orderNumber, Long amount) {
		super(userId, "ORDERS", orderId);  // FromWhere.ORDERS.name()
		this.orderNumber = orderNumber;
		this.amount = amount;
	}

	@Override
	public NotificationTypes getNotificationType() {
		return NotificationTypes.PAYMENT;  // enum 값
	}

	@Override
	public String getTypeDetail() {
		return "PAYMENT_SUCCESS";
	}

	@Override
	public String getTitle() {
		return "주문 및 결제가 완료되었습니다";
	}

	@Override
	public String getMessage() {
		return String.format("주문번호: %s, 결제금액: %,d원", orderNumber, amount);
	}
}
