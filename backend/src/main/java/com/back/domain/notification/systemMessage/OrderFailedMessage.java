package com.back.domain.notification.systemMessage;

import com.back.domain.notification.enums.DomainName;
import com.back.domain.notification.enums.NotificationTypeDetails;
import com.back.domain.notification.enums.NotificationTypes;

public class OrderFailedMessage extends NotificationMessage {
	private final Long amount;
	private final String eventName;
	public OrderFailedMessage(Long userId, Long amount, Long whereId, String eventName) {
		super(userId, DomainName.ORDERS, whereId);
		this.amount = amount;
		this.eventName = eventName;
	}
	@Override
	public NotificationTypes getNotificationType() {
		return NotificationTypes.PAYMENT;  // enum 값
	}

	@Override
	public NotificationTypeDetails getTypeDetail() {
		return NotificationTypeDetails.PAYMENT_FAILED;
	}

	@Override
	public String getTitle() {
		return "주문 및 결제 실패";
	}

	@Override
	public String getMessage() {
		return String.format("[%s]\n결제금액: %d원\n결제에 실패하였습니다", this.eventName, this.amount);
	}
}