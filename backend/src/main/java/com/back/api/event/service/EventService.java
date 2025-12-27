package com.back.api.event.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.event.dto.request.EventCreateRequest;
import com.back.api.event.dto.request.EventUpdateRequest;
import com.back.api.event.dto.response.EventListResponse;
import com.back.api.event.dto.response.EventResponse;
import com.back.api.s3.service.S3MoveService;
import com.back.api.s3.service.S3PresignedService;
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
	private final S3MoveService s3MoveService;
	private final S3PresignedService s3PresignedService;

	@Transactional
	public EventResponse createEvent(EventCreateRequest request) {

		validateEventDates(
			request.preOpenAt(),
			request.preCloseAt(),
			request.ticketOpenAt(),
			request.ticketCloseAt()
		);

		validateDuplicateEvent(
			request.title(),
			request.place(),
			request.ticketOpenAt()
		);

		Event event = request.toEntity();
		Event savedEvent = eventRepository.save(event);

		// 이미지가 있으면 temp → events/{eventId}/main.{ext}
		if (savedEvent.getImageUrl() != null && !savedEvent.getImageUrl().isBlank()) {

			// imageUrl 컬럼에는 실제로 S3 objectKey가 저장됨 (URL 아님)
			String tempKey = savedEvent.getImageUrl();
			String finalKey = s3MoveService.moveImage(savedEvent.getId(), tempKey);

			// imageUrl 필드에 최종 key 저장
			savedEvent.changeBasicInfo(
				savedEvent.getTitle(),
				savedEvent.getCategory(),
				savedEvent.getDescription(),
				savedEvent.getPlace(),
				finalKey
			);
		}

		return EventResponse.from(savedEvent);
	}

	@Transactional
	public EventResponse updateEvent(Long eventId, EventUpdateRequest request) {
		Event event = findEventById(eventId);

		validateEventDates(
			request.preOpenAt(),
			request.preCloseAt(),
			request.ticketOpenAt(),
			request.ticketCloseAt()
		);

		validateDuplicateEventForUpdate(
			eventId,
			request.title(),
			request.place(),
			request.ticketOpenAt()
		);

		String imageUrl = event.getImageUrl();

		// 이미지가 변경된 경우
		if (request.imageUrl() != null && request.imageUrl().startsWith("events/temp/")) {
			imageUrl = s3MoveService.moveImage(event.getId(), request.imageUrl());
		}

		event.changeBasicInfo(
			request.title(),
			request.category(),
			request.description(),
			request.place(),
			imageUrl
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
			request.ticketCloseAt(),
			request.eventDate()
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

		String imageUrl = null;

		if (event.getImageUrl() != null && !event.getImageUrl().isBlank()) {
			imageUrl = s3PresignedService.issueDownloadUrl(event.getImageUrl());
		}

		return EventResponse.from(event, imageUrl);
	}

	public Page<EventListResponse> getEvents(EventStatus status, EventCategory category, Pageable pageable) {
		Page<Event> events = eventRepository.findByConditions(status, category, pageable);
		return events.map(EventListResponse::from);
	}

	public Event getEventEntity(Long eventId) {
		return findEventById(eventId);
	}

	private Event findEventById(Long eventId) {
		return eventRepository.findById(eventId)
			.orElseThrow(() -> new ErrorException(EventErrorCode.NOT_FOUND_EVENT));
	}

	private void validateEventDates(LocalDateTime preOpenAt, LocalDateTime preCloseAt,
		LocalDateTime ticketOpenAt, LocalDateTime ticketCloseAt) {

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
		if (eventRepository.existsByTitleAndPlaceAndTicketOpenAtAndDeletedFalse(title, place, ticketOpenAt)) {
			throw new ErrorException(EventErrorCode.DUPLICATE_EVENT);
		}
	}

	private void validateDuplicateEventForUpdate(Long eventId, String title, String place,
		LocalDateTime ticketOpenAt) {
		Optional<Event> duplicateEvent = eventRepository.findByTitleAndPlaceAndTicketOpenAtAndDeletedFalse(
			title, place, ticketOpenAt);

		if (duplicateEvent.isPresent() && !duplicateEvent.get().getId().equals(eventId)) {
			throw new ErrorException(EventErrorCode.DUPLICATE_EVENT);
		}
	}

	public List<Event> findEventsByStatus(EventStatus status) {
		return eventRepository.findByStatus(status);
	}

	public List<Event> findEventsByTicketOpenAtBetweenAndStatus(
		LocalDateTime start,
		LocalDateTime end,
		EventStatus status
	) {
		return eventRepository.findByTicketOpenAtBetweenAndStatus(start, end, status);
	}

}
