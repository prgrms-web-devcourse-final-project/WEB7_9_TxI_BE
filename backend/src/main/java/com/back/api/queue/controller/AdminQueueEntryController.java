package com.back.api.queue.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.queue.dto.request.ShuffleQueueRequest;
import com.back.api.queue.dto.response.CompletedQueueResponse;
import com.back.api.queue.dto.response.QueueStatisticsResponse;
import com.back.api.queue.dto.response.ShuffleQueueResponse;
import com.back.api.queue.service.QueueEntryProcessService;
import com.back.api.queue.service.QueueEntryReadService;
import com.back.api.queue.service.QueueShuffleService;
import com.back.domain.queue.repository.QueueEntryRedisRepository;
import com.back.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/v1/admin/queues")
@RequiredArgsConstructor
//@PreAuthorize("hasRole('ADMIN')")
public class AdminQueueEntryController implements AdminQueueEntryApi {

	private final QueueShuffleService queueShuffleService;
	private final QueueEntryReadService queueEntryReadService;
	private final QueueEntryProcessService queueEntryProcessService;
	private final QueueEntryRedisRepository queueEntryRedisRepository;

	@Override
	@PostMapping("/{eventId}/shuffle")
	public ApiResponse<ShuffleQueueResponse> shuffleQueue(
		@PathVariable Long eventId,
		@RequestBody @Valid ShuffleQueueRequest request
	) {
		queueShuffleService.shuffleQueue(eventId, request.preRegisteredUserIds());

		ShuffleQueueResponse response = ShuffleQueueResponse.from(
			eventId,
			request.preRegisteredUserIds().size()
		);
		return ApiResponse.created("랜덤 큐가 생성되었습니다.", response);
	}

	@Override
	@GetMapping("/{eventId}/statistics")
	public ApiResponse<QueueStatisticsResponse> getQueueStatistics(
		@PathVariable Long eventId
	) {
		QueueStatisticsResponse response = queueEntryReadService.getQueueStatistics(eventId);
		return ApiResponse.ok("대기열 통계를 조회했습니다.", response);
	}

	//테스트용
	@Override
	@PostMapping("/{eventId}/users/{userId}/complete")
	public ApiResponse<CompletedQueueResponse> completePayment(
		@PathVariable Long eventId,
		@PathVariable Long userId
	) {
		queueEntryProcessService.completePayment(eventId, userId);

		CompletedQueueResponse response = CompletedQueueResponse.from(userId, eventId);
		return ApiResponse.ok("결제 완료 처리되었습니다.", response);

	}

	@Override
	@DeleteMapping("/{eventId}/reset")
	public ApiResponse<Void> resetQueue(
		@PathVariable Long eventId
	) {
		queueEntryRedisRepository.clearAll(eventId);
		return ApiResponse.ok("대기열이 초기화되었습니다.", null);
	}
}
