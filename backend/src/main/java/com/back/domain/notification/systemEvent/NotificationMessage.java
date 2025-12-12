package com.back.domain.notification.systemEvent;

import com.back.domain.notification.enums.NotificationTypes;

import lombok.Getter;

public abstract class NotificationMessage {
	// Getters
	@Getter
	private final Long userId;
	@Getter
	private final String fromWhere;  // EVENT, QUEUE_ENTRIES, ORDERS, PAYMENT, USERS, PRE_REGISTER, TICKETS
	@Getter
	private final Long whereId;

	protected NotificationMessage(Long userId, String fromWhere, Long whereId) {
		this.userId = userId;
		this.fromWhere = fromWhere;
		this.whereId = whereId;
	}

	// 각 구체 클래스에서 구현
	public abstract NotificationTypes getNotificationType();

	public abstract String getTypeDetail();

	public abstract String getTitle();

	public abstract String getMessage();
}
