package com.back.domain.notification.systemMessage;

import static java.lang.String.*;

import com.back.domain.notification.enums.DomainName;
import com.back.domain.notification.enums.NotificationTypeDetails;
import com.back.domain.notification.enums.NotificationTypes;

public class QueueEntriesMessage extends NotificationMessage {
	private final String eventName;

	public QueueEntriesMessage(Long userId, Long domainId, String eventName) {
		super(userId, DomainName.QUEUE_ENTRIES, domainId);
		this.eventName = eventName;
	}

	@Override
	public NotificationTypes getNotificationType() {
		return NotificationTypes.QUEUE_ENTRIES;
	}

	@Override
	public NotificationTypeDetails getTypeDetail() {
		return NotificationTypeDetails.TICKETING_POSSIBLE;
	}

	@Override
	public String getTitle() {
		return "티켓팅 시작";
	}

	@Override
	public String getMessage() {
		return format("[%s]\n입장 준비가 완료되었습니다.\n이제 티켓을 구매하실 수 있습니다.", this.eventName);
	}
}
