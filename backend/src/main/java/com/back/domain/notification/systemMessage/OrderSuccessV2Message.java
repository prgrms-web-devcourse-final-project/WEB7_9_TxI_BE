package com.back.domain.notification.systemMessage;

import static com.back.domain.notification.enums.NotificationTypeDetails.*;

import com.back.domain.notification.enums.DomainName;
import com.back.domain.notification.enums.NotificationTypeDetails;
import com.back.domain.notification.enums.NotificationTypes;

public class OrderSuccessV2Message extends NotificationMessage {
	private final Long amount;
	private final String eventName;

	public OrderSuccessV2Message(Long userId, String orderId, Long amount, String eventName) {
		super(userId, DomainName.ORDERS, orderId);
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
		return String.format("[%s] 티켓 1매가 결제되었습니다\n결제금액: %d원", this.eventName, this.amount);
	}
}
