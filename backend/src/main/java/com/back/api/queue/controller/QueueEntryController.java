package com.back.api.queue.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.queue.dto.response.QueueEntryStatusResponse;
import com.back.api.queue.service.QueueEntryReadService;
import com.back.global.http.HttpRequestContext;
import com.back.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * 대기열 조회 API (일반 사용자)
 * 
 * 인증된 사용자만 자신의 대기열 정보를 조회할 수 있습니다.
 */
@RestController
@RequestMapping("/api/v1/queues")
@RequiredArgsConstructor
public class QueueEntryController implements QueueEntryApi {

	private final QueueEntryReadService queueEntryReadService;
	private final HttpRequestContext httpRequestContext;

	/**
	 * 내 대기열 상태 조회
	 * 
	 * 인증된 사용자의 대기열 상태를 조회합니다.
	 * 
	 * @param eventId 이벤트 ID
	 * @return QueueEntryStatusResponse (WAITING, ENTERED, EXPIRED, COMPLETED)
	 */
	@Override
	@GetMapping("/{eventId}/status")
	public ApiResponse<QueueEntryStatusResponse> getMyQueueEntryStatus(
		@PathVariable Long eventId
	) {
		// ✅ 인증된 사용자 ID 가져오기
		Long userId = httpRequestContext.getUser().getId();
		
		QueueEntryStatusResponse response = queueEntryReadService.getMyQueueStatus(eventId, userId);
		return ApiResponse.ok("대기열 상태를 조회했습니다.", response);
	}

	/**
	 * 대기열 진입 여부 확인
	 * 
	 * 인증된 사용자가 대기열에 있는지 확인합니다.
	 * 
	 * @param eventId 이벤트 ID
	 * @return boolean (true: 대기열에 있음, false: 없음)
	 */
	@Override
	@GetMapping("/{eventId}/exists")
	public ApiResponse<Boolean> existsInQueue(
		@PathVariable Long eventId
	) {
		// ✅ 인증된 사용자 ID 가져오기
		Long userId = httpRequestContext.getUser().getId();
		
		boolean exists = queueEntryReadService.existsInWaitingQueue(eventId, userId);
		return ApiResponse.ok("대기열 진입 여부를 확인했습니다.", exists);
	}
}
