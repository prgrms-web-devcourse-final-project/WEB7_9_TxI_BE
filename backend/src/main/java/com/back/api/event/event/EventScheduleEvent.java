package com.back.api.event.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class EventScheduleEvent {
	private final Long eventId;
	private final EventScheduleType type;

	public enum EventScheduleType {
		CREATED,
		UPDATED,
		DELETED
	}
}