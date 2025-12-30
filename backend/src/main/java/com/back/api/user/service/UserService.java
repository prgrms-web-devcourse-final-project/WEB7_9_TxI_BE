package com.back.api.user.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.queue.service.QueueEntryProcessService;
import com.back.api.queue.service.QueueEntryReadService;
import com.back.api.ticket.service.TicketService;
import com.back.api.auth.service.ActiveSessionCache;
import com.back.api.user.dto.request.UpdateProfileRequest;
import com.back.api.user.dto.response.UserProfileResponse;
import com.back.domain.auth.repository.RefreshTokenRepository;
import com.back.domain.queue.entity.QueueEntry;
import com.back.domain.ticket.entity.Ticket;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.error.code.AuthErrorCode;
import com.back.global.error.code.UserErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.http.HttpRequestContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final ActiveSessionCache activeSessionCache;
	private final HttpRequestContext requestContext;
	private final QueueEntryReadService queueEntryReadService;
	private final QueueEntryProcessService queueEntryProcessService;
	private final TicketService ticketService;

	@Transactional
	public UserProfileResponse getUser(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ErrorException(UserErrorCode.NOT_FOUND));

		return UserProfileResponseMapper.from(user);
	}

	@Transactional
	public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ErrorException(UserErrorCode.NOT_FOUND));

		String newNickname = request.nickname() != null ? request.nickname() : user.getNickname();
		String newFullName = request.fullName() != null ? request.fullName() : user.getFullName();
		LocalDate newBirthDate = request.toBirthDate() != null ? request.toBirthDate() : user.getBirthDate();

		if (request.nickname() != null && !request.nickname().equals(user.getNickname())) {
			if (userRepository.existsByNickname(request.nickname())) {
				throw new ErrorException(AuthErrorCode.ALREADY_EXIST_NICKNAME);
			}
		}

		user.update(newFullName, newNickname, newBirthDate);

		return UserProfileResponseMapper.from(user);
	}

	@Transactional
	public void deleteUser(long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ErrorException(UserErrorCode.NOT_FOUND));

		handleDeleteUserQueue(userId);

		// 모든 기기의 refreshToken 무효화 (전 기기 로그아웃 효과)
		refreshTokenRepository.revokeAllByUserId(userId);

		// activeSession redis 캐시 무효화
		activeSessionCache.evict(userId);

		// 현재 요청의 쿠키 삭제 (현재 기기 즉시 로그아웃 UX)
		requestContext.deleteAuthCookies();

		user.softDelete();
	}

	private void handleDeleteUserQueue(long userId) {
		// 1. 사전등록 인원: 랜덤 큐 스케줄러 동작 전까지 회원탈퇴 가능
		List<QueueEntry> waitingOrEnteredQueues = queueEntryReadService.getWaitingOrEnteredQueues(userId);

		// 2. 티켓을 구매한 상태면 행사 오픈날짜가 지나고 회원탈퇴 할 수 있도록 하기
		List<Ticket> tickets = ticketService.getMyIssuedOrPaidTicketsBeforeEvent(userId);

		if (waitingOrEnteredQueues.isEmpty() && tickets.isEmpty()) {
			return;
		}

		// 랜덤 큐에 배정된 이후회원 탈퇴를 시도하면, 사용자 삭제: 배정된 랜덤큐에서 해당 사옹자 제거
		if (!waitingOrEnteredQueues.isEmpty() && tickets.isEmpty()) {
			removeUserInQueues(waitingOrEnteredQueues, userId);
			log.info("Success remove WAITING|ENTERED queues for deleted userId: {}", userId);
			return;
		}

		log.error("Can't delete user for userId: {}", userId);
		throw new ErrorException(UserErrorCode.CAN_NOT_DELETE_USER);
	}

	private void removeUserInQueues(List<QueueEntry> queues, long userId) {
		for (QueueEntry queue : queues) {
			queueEntryProcessService.expireWaitingAndEnteredEntry(queue.getEventId(), userId);
		}
	}
}
