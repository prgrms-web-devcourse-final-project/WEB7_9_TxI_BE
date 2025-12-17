package com.back.domain.notification.systemMessage;

import com.back.domain.notification.enums.DomainName;
import com.back.domain.notification.enums.NotificationTypeDetails;
import com.back.domain.notification.enums.NotificationTypes;

import lombok.Getter;

@Getter
public abstract class NotificationMessage {

	private final Long userId;
	private final DomainName fromWhere;
	private final Long whereId;

	protected NotificationMessage(Long userId, DomainName fromWhere, Long whereId) {
		this.userId = userId;
		this.fromWhere = fromWhere;
		this.whereId = whereId;
	}

	// 각 구체 클래스에서 구현할 것
	public abstract NotificationTypes getNotificationType();

	public abstract NotificationTypeDetails getTypeDetail();

	public abstract String getTitle();

	public abstract String getMessage();
}
