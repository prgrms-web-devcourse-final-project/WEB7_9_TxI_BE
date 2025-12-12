package com.back.domain.notification.systemEvent;

import com.back.domain.notification.enums.NotificationTypes;

public class TicketMessage extends NotificationMessage {
	private final String eventName;
	private final String seatNumber;
	private final boolean isCancelled;

	public TicketMessage(Long userId, Long ticketId, String eventName,
		String seatNumber, boolean isCancelled) {
		super(userId, "TICKETS", ticketId);
		this.eventName = eventName;
		this.seatNumber = seatNumber;
		this.isCancelled = isCancelled;
	}

	@Override
	public NotificationTypes getNotificationType() {
		return isCancelled ? NotificationTypes.ENTER_NOW : NotificationTypes.PRE_REGISTER_DONE;
	}

	@Override
	public String getTypeDetail() {
		return isCancelled ? "TICKET_CANCELLED" : "TICKET_RESERVED";
	}

	@Override
	public String getTitle() {
		return isCancelled ? "티켓이 취소되었습니다" : "티켓 예매가 완료되었습니다";
	}

	@Override
	public String getMessage() {
		return String.format("%s - 좌석: %s", eventName, seatNumber);
	}
}
