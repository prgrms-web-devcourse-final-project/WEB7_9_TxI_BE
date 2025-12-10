package com.back.api.preregister.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.preregister.dto.response.PreRegisterResponse;
import com.back.domain.event.entity.Event;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.preregister.entity.PreRegister;
import com.back.domain.preregister.entity.PreRegisterStatus;
import com.back.domain.preregister.repository.PreRegisterRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.error.code.CommonErrorCode;
import com.back.global.error.code.EventErrorCode;
import com.back.global.error.code.PreRegisterErrorCode;
import com.back.global.error.exception.ErrorException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreRegisterService {

	private final PreRegisterRepository preRegisterRepository;
	private final EventRepository eventRepository;
	private final UserRepository userRepository;

	@Transactional
	public PreRegisterResponse register(Long eventId, Long userId) {
		Event event = findEventById(eventId);
		User user = findUserById(userId);

		validatePreRegistrationPeriod(event);
		validateDuplicateRegistration(eventId, userId);

		PreRegister preRegister = PreRegister.builder()
			.event(event)
			.user(user)
			.build();

		PreRegister savedPreRegister = preRegisterRepository.save(preRegister);
		return PreRegisterResponse.from(savedPreRegister);
	}

	@Transactional
	public void cancel(Long eventId, Long userId) {
		PreRegister preRegister = findPreRegister(eventId, userId);

		if (preRegister.isCanceled()) {
			throw new ErrorException(PreRegisterErrorCode.ALREADY_CANCELED);
		}

		preRegister.cancel();
	}

	public boolean isRegistered(Long eventId, Long userId) {
		return preRegisterRepository.findByEvent_IdAndUser_Id(eventId, userId)
			.map(PreRegister::isRegistered)
			.orElse(false);
	}

	public PreRegisterResponse getMyPreRegister(Long eventId, Long userId) {
		PreRegister preRegister = findPreRegister(eventId, userId);
		return PreRegisterResponse.from(preRegister);
	}

	public Long getRegistrationCount(Long eventId) {
		findEventById(eventId);
		return preRegisterRepository.countByEvent_IdAndPreRegisterStatus(eventId, PreRegisterStatus.REGISTERED);
	}

	private Event findEventById(Long eventId) {
		return eventRepository.findById(eventId)
			.orElseThrow(() -> new ErrorException(EventErrorCode.NOT_FOUND_EVENT));
	}

	private User findUserById(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new ErrorException(CommonErrorCode.NOT_FOUND_USER));
	}

	private PreRegister findPreRegister(Long eventId, Long userId) {
		return preRegisterRepository.findByEvent_IdAndUser_Id(eventId, userId)
			.orElseThrow(() -> new ErrorException(PreRegisterErrorCode.NOT_FOUND_PRE_REGISTER));
	}

	private void validatePreRegistrationPeriod(Event event) {
		LocalDateTime now = LocalDateTime.now();
		if (now.isBefore(event.getPreOpenAt()) || now.isAfter(event.getPreCloseAt())) {
			throw new ErrorException(PreRegisterErrorCode.INVALID_PRE_REGISTRATION_PERIOD);
		}
	}

	private void validateDuplicateRegistration(Long eventId, Long userId) {
		if (preRegisterRepository.existsByEvent_IdAndUser_Id(eventId, userId)) {
			throw new ErrorException(PreRegisterErrorCode.ALREADY_PRE_REGISTERED);
		}
	}
}
