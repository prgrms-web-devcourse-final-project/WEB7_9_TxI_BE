package com.back.domain.notification.systemMessage.v1;

import com.back.domain.notification.enums.DomainName;
import com.back.domain.notification.enums.NotificationTypes;
import com.back.domain.notification.enums.v1.NotificationTypeDetails;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public abstract class NotificationMessage {

	private final Long userId;
	private final DomainName domainName;
	private final Long domainId;
	private final String orderId;

	protected NotificationMessage(Long userId, DomainName doaminName, Long domainId) {
		this(userId, doaminName, domainId, null);
	}

	protected NotificationMessage(Long userId, DomainName domainName, String orderId) {
		this(userId, domainName, null, orderId);
	}

	// 각 구체 클래스에서 구현할 것
	public abstract NotificationTypes getNotificationType();

	public abstract NotificationTypeDetails getTypeDetail();

	public abstract String getTitle();

	public abstract String getMessage();
}
