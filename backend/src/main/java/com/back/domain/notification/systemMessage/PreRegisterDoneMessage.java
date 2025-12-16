package com.back.domain.notification.systemMessage;

import static java.lang.String.*;

import com.back.domain.notification.enums.DomainName;
import com.back.domain.notification.enums.NotificationTypeDetails;
import com.back.domain.notification.enums.NotificationTypes;

public class PreRegisterDoneMessage extends NotificationMessage {
	private final String eventName;

	public PreRegisterDoneMessage(Long userId, Long whereId, String eventName) {
		super(userId, DomainName.PRE_REGISTER, whereId);
		this.eventName = eventName;
	}

	@Override
	public NotificationTypes getNotificationType() {
		return NotificationTypes.PRE_REGISTER;
	}

	@Override
	public NotificationTypeDetails getTypeDetail() {
		return NotificationTypeDetails.PRE_REGISTER_DONE;
	}

	@Override
	public String getTitle() {
		return "사전등록 완료";
	}

	@Override
	public String getMessage() {
		return format("[%s]\n사전등록이 완료되었습니다.\n티켓팅 시작일에 알림을 보내드리겠습니다.", this.eventName);
	}
}
