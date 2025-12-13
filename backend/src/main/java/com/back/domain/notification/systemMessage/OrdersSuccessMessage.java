package com.back.domain.notification.systemMessage;

import static com.back.domain.notification.enums.DomainName.*;
import static com.back.domain.notification.enums.NotificationTypeDetails.*;

import com.back.domain.notification.enums.NotificationTypeDetails;
import com.back.domain.notification.enums.NotificationTypes;

public class OrdersSuccessMessage extends NotificationMessage {
	private final Long amount;
	private final String eventName;

	public OrdersSuccessMessage(Long userId, Long orderId, Long amount, String eventName) {
		super(userId, ORDERS, orderId);
		this.amount = amount;
		this.eventName = eventName;
	}

	@Override
	public NotificationTypes getNotificationType() {
		return NotificationTypes.PAYMENT;  // enum 값
	}

	@Override
	public NotificationTypeDetails getTypeDetail() {
		return PAYMENT_SUCCESS;
	}

	@Override
	public String getTitle() {
		return "주문 및 결제 완료";
	}

	@Override
	public String getMessage() {
		return String.format("[%s]\n결제금액: %d원\n결제에 성공하였습니다", this.eventName, this.amount);
	}
}
