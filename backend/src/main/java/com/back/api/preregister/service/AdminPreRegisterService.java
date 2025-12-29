package com.back.api.preregister.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.back.api.event.service.EventService;
import com.back.api.preregister.dto.response.PreRegisterListResponse;
import com.back.domain.event.entity.Event;
import com.back.domain.preregister.entity.PreRegister;
import com.back.domain.preregister.entity.PreRegisterStatus;
import com.back.domain.preregister.repository.PreRegisterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminPreRegisterService {

	private final PreRegisterRepository preRegisterRepository;
	private final EventService eventService;

	public Page<PreRegisterListResponse> getPreRegisterByEventId(Long eventId, int page, int size) {

		Event event = eventService.getEventEntity(eventId);

		Pageable pageable = PageRequest.of(page, size);
		Page<PreRegister> preRegisters = preRegisterRepository.findByEventIdWithUserAndEvent(eventId, pageable);
		return preRegisters.map(PreRegisterListResponse::from);
	}

	public Long getPreRegisterCountByEventId(Long eventId) {
		return preRegisterRepository.countByEvent_IdAndPreRegisterStatus(
			eventId,
			PreRegisterStatus.REGISTERED
		);
	}
}
