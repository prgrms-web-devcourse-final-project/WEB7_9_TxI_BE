package com.back.api.event.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.event.dto.request.EventCreateRequest;
import com.back.api.event.dto.request.EventUpdateRequest;
import com.back.api.event.dto.response.EventListResponse;
import com.back.api.event.dto.response.EventResponse;
import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventCategory;
import com.back.domain.event.entity.EventStatus;
import com.back.domain.event.repository.EventRepository;
import com.back.global.error.code.EventErrorCode;
import com.back.global.error.exception.ErrorException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

	private final EventRepository eventRepository;

	@Transactional
	public EventResponse createEvent(EventCreateRequest request) {
		validateEventDates(request.preOpenAt(), request.preCloseAt(),
			request.ticketOpenAt(), request.ticketCloseAt());
		validateDuplicateEvent(request.title(), request.place(), request.ticketOpenAt());

		Event event = request.toEntity();
		Event savedEvent = eventRepository.save(event);

		return EventResponse.from(savedEvent);
	}

	@Transactional
	public EventResponse updateEvent(Long eventId, EventUpdateRequest request) {
		Event event = findEventById(eventId);

		validateEventDates(request.preOpenAt(), request.preCloseAt(),
			request.ticketOpenAt(), request.ticketCloseAt());
		validateDuplicateEventForUpdate(eventId, request.title(), request.place(), request.ticketOpenAt());

		event.changeBasicInfo(
			request.title(),
			request.category(),
			request.description(),
			request.place(),
			request.imageUrl()
		);
		event.changePriceInfo(
			request.minPrice(),
			request.maxPrice(),
			request.maxTicketAmount()
		);
		event.changePeriod(
			request.preOpenAt(),
			request.preCloseAt(),
			request.ticketOpenAt(),
			request.ticketCloseAt()
		);
		event.changeStatus(request.status());

		return EventResponse.from(event);
	}

	@Transactional
	public void deleteEvent(Long eventId) {
		Event event = findEventById(eventId);
		event.delete();
	}

	public EventResponse getEvent(Long eventId) {
		Event event = findEventById(eventId);
		return EventResponse.from(event);
	}

	public Page<EventListResponse> getEvents(EventStatus status, EventCategory category, Pageable pageable) {
		Page<Event> events = eventRepository.findByConditions(status, category, pageable);
		return events.map(EventListResponse::from);
	}

	private Event findEventById(Long eventId) {
		return eventRepository.findById(eventId)
			.orElseThrow(() -> new ErrorException(EventErrorCode.NOT_FOUND_EVENT));
	}

	private void validateEventDates(LocalDateTime preOpenAt, LocalDateTime preCloseAt,
		LocalDateTime ticketOpenAt, LocalDateTime ticketCloseAt) {
		if (preOpenAt.isBefore(LocalDateTime.now())) {
			throw new ErrorException(EventErrorCode.INVALID_EVENT_DATE);
		}
		if (preOpenAt.isAfter(preCloseAt)) {
			throw new ErrorException(EventErrorCode.INVALID_EVENT_DATE);
		}
		if (ticketOpenAt.isAfter(ticketCloseAt)) {
			throw new ErrorException(EventErrorCode.INVALID_EVENT_DATE);
		}
		if (preCloseAt.isAfter(ticketOpenAt)) {
			throw new ErrorException(EventErrorCode.INVALID_EVENT_DATE);
		}
	}

	private void validateDuplicateEvent(String title, String place, LocalDateTime ticketOpenAt) {
		if (eventRepository.existsByTitleAndPlaceAndTicketOpenAt(title, place, ticketOpenAt)) {
			throw new ErrorException(EventErrorCode.DUPLICATE_EVENT);
		}
	}

	private void validateDuplicateEventForUpdate(Long eventId, String title, String place,
		LocalDateTime ticketOpenAt) {
		eventRepository.findByTitleAndPlaceAndTicketOpenAt(title, place, ticketOpenAt)
			.ifPresent(existingEvent -> {
				if (!existingEvent.getId().equals(eventId)) {
					throw new ErrorException(EventErrorCode.DUPLICATE_EVENT);
				}
			});
	}
}
