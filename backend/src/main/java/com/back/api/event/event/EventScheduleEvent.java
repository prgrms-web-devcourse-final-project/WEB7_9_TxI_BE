package com.back.api.event.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class EventScheduleEvent {
	private final Long eventId;
	private final EventScheduleType type;

	public enum EventScheduleType {
		CREATED,    // 이벤트 생성
		UPDATED,    // 이벤트 수정
		DELETED     // 이벤트 삭제
	}
}