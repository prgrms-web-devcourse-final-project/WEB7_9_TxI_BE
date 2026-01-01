package com.back.support.helper;

import org.springframework.stereotype.Component;

import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventCategory;
import com.back.domain.event.entity.EventStatus;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.store.entity.Store;
import com.back.support.factory.EventFactory;

@Component
public class EventHelper {

	private final EventRepository eventRepository;

	public EventHelper(EventRepository eventRepository) {
		this.eventRepository = eventRepository;
	}

	public Event createEvent(Store store) {
		return eventRepository.save(EventFactory.fakeEvent(store));
	}

	public Event createEvent(Store store, String title) {
		return eventRepository.save(EventFactory.fakeEvent(store, title));
	}

	public Event createEvent(Store store, EventCategory category, EventStatus status) {
		return eventRepository.save(EventFactory.fakeEvent(store, category, status));
	}

	public Event createPreOpenEvent(Store store) {
		return eventRepository.save(EventFactory.fakePreOpenEvent(store));
	}

	public Event createReadyEvent(Store store) {
		return eventRepository.save(EventFactory.fakeReadyEvent(store));
	}

	public Event createPastEvent(Store store, String title) {
		return eventRepository.save(EventFactory.fakePastEvent(store, title));
	}

	public void clearEvent() {
		eventRepository.deleteAll();
	}
}
