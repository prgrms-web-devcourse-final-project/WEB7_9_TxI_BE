package com.back.domain.notification.systemMessage.v2;

import com.back.domain.notification.enums.DomainName;
import com.back.domain.notification.enums.v2.V2_NotificationContext;
import com.back.domain.notification.enums.v2.V2_NotificationVar;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@RequiredArgsConstructor
public class V2_NotificationMessage {
	private final Long userId;
	private final DomainName domainName;
	private final V2_NotificationVar notificationVar;
	private final V2_NotificationContext context;

	public static V2_NotificationMessage preRegisterDone(
		Long userId, String eventTitle) {

		V2_NotificationContext context = V2_NotificationContext.builder()
			.eventTitle(eventTitle)
			.build();

		return new V2_NotificationMessage(
			userId,
			DomainName.PRE_REGISTER,
			V2_NotificationVar.PRE_REGISTER_DONE,
			context
		);
	}

	public static V2_NotificationMessage queueEntered(
		Long userId, String eventTitle) {

		V2_NotificationContext context = V2_NotificationContext.builder()
			.eventTitle(eventTitle)
			.build();

		return new V2_NotificationMessage(
			userId,
			DomainName.QUEUE_ENTRIES,
			V2_NotificationVar.QUEUE_ENTERED,
			context
		);
	}

	public static V2_NotificationMessage queueExpired(
		Long userId, String eventTitle) {

		V2_NotificationContext context = V2_NotificationContext.builder()
			.eventTitle(eventTitle)
			.build();

		return new V2_NotificationMessage(
			userId,
			DomainName.QUEUE_ENTRIES,
			V2_NotificationVar.QUEUE_EXPIRED,
			context
		);
	}

	public static V2_NotificationMessage queueWaiting(
		Long userId, String eventTitle) {

		V2_NotificationContext context = V2_NotificationContext.builder()
			.eventTitle(eventTitle)
			.build();

		return new V2_NotificationMessage(
			userId,
			DomainName.QUEUE_ENTRIES,
			V2_NotificationVar.QUEUE_WAITING,
			context
		);
	}

	public static V2_NotificationMessage paymentSuccess(
		Long userId, String eventTitle, Long amount
	) {
		V2_NotificationContext context = V2_NotificationContext.builder()
			.eventTitle(eventTitle)
			.amount(amount)
			.build();

		return new V2_NotificationMessage(
			userId,
			DomainName.ORDERS,
			V2_NotificationVar.PAYMENT_SUCCESS,
			context
		);
	}
}
