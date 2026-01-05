package com.back.domain.notification.systemMessage;

import com.back.domain.notification.enums.DomainName;
import com.back.domain.notification.enums.NotificationContext;
import com.back.domain.notification.enums.NotificationVar;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@RequiredArgsConstructor
public class NotificationMessage {
	private final Long userId;
	private final DomainName domainName;
	private final NotificationVar notificationVar;
	private final NotificationContext context;

	public static NotificationMessage signUp(
		Long userId, String userName) {

		NotificationContext context = NotificationContext.builder()
			.userName(userName)
			.build();

		return new NotificationMessage(
			userId,
			DomainName.USERS,
			NotificationVar.SIGN_UP,
			context
		);
	}

	public static NotificationMessage preRegisterDone(
		Long userId, String eventTitle) {

		NotificationContext context = NotificationContext.builder()
			.eventTitle(eventTitle)
			.build();

		return new NotificationMessage(
			userId,
			DomainName.PRE_REGISTER,
			NotificationVar.PRE_REGISTER_DONE,
			context
		);
	}

	public static NotificationMessage queueEntered(
		Long userId, String eventTitle) {

		NotificationContext context = NotificationContext.builder()
			.eventTitle(eventTitle)
			.build();

		return new NotificationMessage(
			userId,
			DomainName.QUEUE_ENTRIES,
			NotificationVar.QUEUE_ENTERED,
			context
		);
	}

	public static NotificationMessage queueExpired(
		Long userId, String eventTitle) {

		NotificationContext context = NotificationContext.builder()
			.eventTitle(eventTitle)
			.build();

		return new NotificationMessage(
			userId,
			DomainName.QUEUE_ENTRIES,
			NotificationVar.QUEUE_EXPIRED,
			context
		);
	}

	public static NotificationMessage queueWaiting(
		Long userId, String eventTitle, Long waitingNum) {

		NotificationContext context = NotificationContext.builder()
			.eventTitle(eventTitle)
			.waitingNum(waitingNum)
			.build();

		return new NotificationMessage(
			userId,
			DomainName.QUEUE_ENTRIES,
			NotificationVar.QUEUE_WAITING,
			context
		);
	}

	public static NotificationMessage paymentSuccess(
		Long userId, String eventTitle, Long amount
	) {
		NotificationContext context = NotificationContext.builder()
			.eventTitle(eventTitle)
			.amount(amount)
			.build();

		return new NotificationMessage(
			userId,
			DomainName.ORDERS,
			NotificationVar.PAYMENT_SUCCESS,
			context
		);
	}
}
