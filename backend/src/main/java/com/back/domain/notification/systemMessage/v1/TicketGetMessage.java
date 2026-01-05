package com.back.domain.notification.systemMessage.v1;

import com.back.domain.notification.enums.DomainName;
import com.back.domain.notification.enums.NotificationTypes;
import com.back.domain.notification.enums.v1.NotificationTypeDetails;

public class TicketGetMessage extends NotificationMessage {
	private final String eventName;

	public TicketGetMessage(Long userId, DomainName fromWhere, Long whereId, String eventName) {
		super(userId, fromWhere, whereId);
		this.eventName = eventName;
	}

	@Override
	public NotificationTypes getNotificationType() {
		return NotificationTypes.TICKET;
	}

	@Override
	public NotificationTypeDetails getTypeDetail() {
		return NotificationTypeDetails.TICKET_GET;
	}

	@Override
	public String getTitle() {
		return "티켓 수령 완료";
	}

	@Override
	public String getMessage() {
		return String.format("[%s]\n티켓 1매가 발급되었습니다", this.eventName);
	}
}
