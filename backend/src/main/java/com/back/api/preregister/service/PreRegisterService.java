package com.back.api.preregister.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.preregister.dto.request.PreRegisterCreateRequest;
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
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public PreRegisterResponse register(Long eventId, Long userId, PreRegisterCreateRequest request) {
		Event event = findEventById(eventId);
		User user = findUserById(userId);

		// 사전등록 기간 검증
		validatePreRegistrationPeriod(event);

		// 본인 인증 정보 검증 (회원가입 정보와 대조)
		validateUserInfo(user, request);

		// 약관 동의 검증
		validateAgreements(request);

		// 중복 등록 방지 (1인 1계정) - 본인 인증 통과 후 검사
		validateDuplicateRegistration(eventId, userId);

		PreRegister preRegister = PreRegister.builder()
			.event(event)
			.user(user)
			.preRegisterAgreeTerms(request.agreeTerms())
			.preRegisterAgreePrivacy(request.agreePrivacy())
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

	/**
	 * 본인 인증 정보 검증 (회원가입 정보와 대조)
	 * - 이름 일치 여부
	 * - 생년월일 일치 여부
	 * - 비밀번호 일치 여부
	 */
	private void validateUserInfo(User user, PreRegisterCreateRequest request) {
		// 이름 검증
		if (!user.getNickname().equals(request.nickname())) {
			throw new ErrorException(PreRegisterErrorCode.INVALID_USER_INFO);
		}

		// 생년월일 검증
		if (user.getBirthDate() == null || !user.getBirthDate().equals(request.birthDate())) {
			throw new ErrorException(PreRegisterErrorCode.INVALID_USER_INFO);
		}

		// 비밀번호 검증
		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new ErrorException(PreRegisterErrorCode.INVALID_PASSWORD);
		}
	}

	/**
	 * 약관 동의 검증
	 * - 이용약관 동의 (필수)
	 * - 개인정보 수집 및 이용 동의 (필수)
	 */
	private void validateAgreements(PreRegisterCreateRequest request) {
		if (!Boolean.TRUE.equals(request.agreeTerms())) {
			throw new ErrorException(PreRegisterErrorCode.TERMS_NOT_AGREED);
		}
		if (!Boolean.TRUE.equals(request.agreePrivacy())) {
			throw new ErrorException(PreRegisterErrorCode.PRIVACY_NOT_AGREED);
		}
	}
}
