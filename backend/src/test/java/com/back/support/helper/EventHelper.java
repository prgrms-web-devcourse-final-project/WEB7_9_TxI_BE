package com.back.support.helper;

import org.springframework.stereotype.Component;

import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventCategory;
import com.back.domain.event.entity.EventStatus;
import com.back.domain.event.repository.EventRepository;
import com.back.support.factory.EventFactory;

@Component
public class EventHelper {

	private final EventRepository eventRepository;

	public EventHelper(EventRepository eventRepository) {
		this.eventRepository = eventRepository;
	}

	public Event createEvent() {
		return eventRepository.save(EventFactory.fakeEvent());
	}

	public Event createEvent(String title) {
		return eventRepository.save(EventFactory.fakeEvent(title));
	}

	public Event createEvent(EventCategory category, EventStatus status) {
		return eventRepository.save(EventFactory.fakeEvent(category, status));
	}

	public void clearEvent() {
		eventRepository.deleteAll();
	}
}