package com.back.api.preregister.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.preregister.dto.request.PreRegisterCreateRequest;
import com.back.api.preregister.dto.response.PreRegisterResponse;
import com.back.domain.event.entity.Event;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.notification.systemMessage.PreRegisterDoneMessage;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreRegisterService {

	private final PreRegisterRepository preRegisterRepository;
	private final EventRepository eventRepository;
	private final UserRepository userRepository;
	private final ApplicationEventPublisher eventPublisher;
	private final StringRedisTemplate redisTemplate;

	private static final String SMS_VERIFIED_PREFIX = "SMS_VERIFIED:";

	@Transactional
	public PreRegisterResponse register(Long eventId, Long userId, PreRegisterCreateRequest request) {
		Event event = findEventById(eventId);
		User user = findUserById(userId);

		// 사전등록 기간 검증
		validatePreRegistrationPeriod(event);

		// SMS 인증 완료 여부 검증
		validateSmsVerification(request.phoneNumber());

		// 본인 인증 정보 검증 (회원가입 정보와 대조)
		validateUserInfo(user, request);

		// 약관 동의 검증
		validateAgreements(request);

		// 기존 사전등록 확인 (CANCELED 상태면 재활용)
		Optional<PreRegister> existingPreRegister = preRegisterRepository.findByEvent_IdAndUser_Id(eventId, userId);

		if (existingPreRegister.isPresent()) {
			PreRegister preRegister = existingPreRegister.get();

			// REGISTERED 상태면 중복 등록 예외
			if (preRegister.isRegistered()) {
				throw new ErrorException(PreRegisterErrorCode.ALREADY_PRE_REGISTERED);
			}

			// CANCELED 상태면 재등록 (상태만 변경)
			preRegister.reRegister();
			return PreRegisterResponse.from(preRegister);
		}

		// 새로운 사전등록 생성
		PreRegister preRegister = PreRegister.builder()
			.event(event)
			.user(user)
			.preRegisterAgreeTerms(request.agreeTerms())
			.preRegisterAgreePrivacy(request.agreePrivacy())
			.build();

		PreRegister savedPreRegister = preRegisterRepository.save(preRegister);

		eventPublisher.publishEvent(
			new PreRegisterDoneMessage(
				userId,
				savedPreRegister.getId(),
				event.getTitle()
			)
		);

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

	/**
	 * 본인 인증 정보 검증 (회원가입 정보와 대조)
	 * - 생년월일 일치 여부
	 * - 이름은 User 엔티티에서 자동으로 가져옴
	 */
	private void validateUserInfo(User user, PreRegisterCreateRequest request) {
		// 생년월일 검증
		if (user.getBirthDate() == null || !user.getBirthDate().equals(request.birthDate())) {
			throw new ErrorException(PreRegisterErrorCode.INVALID_USER_INFO);
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

	/**
	 * SMS 인증 완료 여부 검증
	 * - SMS 인증을 통과한 경우 Redis에 인증 완료 플래그가 저장됨
	 * - 인증 완료 후 사전등록 시 해당 플래그를 검증
	 */
	private void validateSmsVerification(String phoneNumber) {
		String verifiedKey = SMS_VERIFIED_PREFIX + phoneNumber;
		String verified = redisTemplate.opsForValue().get(verifiedKey);

		if (verified == null || !Boolean.parseBoolean(verified)) {
			log.warn("SMS 인증 미완료 - 전화번호: {}", phoneNumber);
			throw new ErrorException(PreRegisterErrorCode.SMS_VERIFICATION_NOT_COMPLETED);
		}

		// 인증 완료 플래그 삭제 (재사용 방지)
		redisTemplate.delete(verifiedKey);
		log.info("SMS 인증 완료 확인 - 전화번호: {}", phoneNumber);
	}
}
