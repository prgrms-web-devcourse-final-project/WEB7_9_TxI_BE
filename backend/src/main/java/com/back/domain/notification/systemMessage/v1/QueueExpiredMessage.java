package com.back.domain.notification.systemMessage.v1;

import static java.lang.String.*;

import com.back.domain.notification.enums.DomainName;
import com.back.domain.notification.enums.NotificationTypes;
import com.back.domain.notification.enums.v1.NotificationTypeDetails;

public class QueueExpiredMessage extends NotificationMessage {
	private final String eventName;

	public QueueExpiredMessage(Long userId, Long domainId, String eventName) {
		super(userId, DomainName.QUEUE_ENTRIES, domainId);
		this.eventName = eventName;
	}

	@Override
	public NotificationTypes getNotificationType() {
		return NotificationTypes.QUEUE_ENTRIES;
	}

	@Override
	public NotificationTypeDetails getTypeDetail() {
		return NotificationTypeDetails.TICKETING_EXPIRED;
	}

	@Override
	public String getTitle() {
		return "티켓팅 종료";
	}

	@Override
	public String getMessage() {
		return format("[%s]\n아쉽게도 티켓팅 가능 시간이 초과되었습니다.\n다음 기회를 노려주세요..", this.eventName);
	}
}
