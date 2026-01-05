package com.back.api.notification.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.notification.dto.UnreadCountResponseDto;
import com.back.api.notification.dto.v1.NotificationResponseDto;
import com.back.api.notification.dto.v2.V2_NotificationResponseDto;
import com.back.api.notification.service.NotificationService;
import com.back.global.http.HttpRequestContext;
import com.back.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class NotificationController implements NotificationApi {

	private final NotificationService notificationService;
	private final HttpRequestContext httpRequestContext;

	@Override
	@GetMapping("/v1/notifications")
	public ApiResponse<List<NotificationResponseDto>> getNotifications(
	) {
		Long userId = httpRequestContext.getUserId();

		List<NotificationResponseDto> notifications =
			notificationService.getNotifications(userId);

		return ApiResponse.ok("알림 목록을 불러왔습니다", notifications);
	}

	/**
	 * 읽지 않은 알림 개수 조회
	 */
	@Override
	@GetMapping("/v1/notifications/unread-count")
	public ApiResponse<UnreadCountResponseDto> getUnreadCount(
	) {
		Long userId = httpRequestContext.getUserId();
		long count = notificationService.getUnreadCount(userId);
		return ApiResponse.ok("읽지 않은 알림수", new UnreadCountResponseDto(count));
	}

	/**
	 * 개별 알림 읽음 처리
	 */
	@Override
	@PatchMapping("/v1/notifications/{notificationId}/read")
	public ApiResponse<Void> markAsRead(
		@PathVariable Long notificationId
	) {
		Long userId = httpRequestContext.getUserId();
		notificationService.markAsRead(notificationId, userId);

		return ApiResponse.ok("개별 알림을 읽음 처리 하였습니다.", null);
	}

	/**
	 * 전체 알림 읽음 처리
	 */
	@Override
	@PatchMapping("/v1/notifications/read-all")
	public ApiResponse<Void> markAllAsRead() {
		Long userId = httpRequestContext.getUserId();
		notificationService.markAllAsRead(userId);
		return ApiResponse.ok("모든 알림을 읽음 처리 하였습니다.", null);
	}

	@Override
	@GetMapping("/v2/notifications")
	public ApiResponse<List<V2_NotificationResponseDto>> v2_getNotifications(
	) {
		Long userId = httpRequestContext.getUserId();

		List<V2_NotificationResponseDto> notifications =
			notificationService.v2_getNotifications(userId);

		return ApiResponse.ok("알림 목록을 불러왔습니다", notifications);
	}

	/**
	 * 읽지 않은 알림 개수 조회
	 */
	@GetMapping("/v2/notifications/unread-count")
	public ApiResponse<UnreadCountResponseDto> v2_getUnreadCount(
	) {
		Long userId = httpRequestContext.getUserId();
		long count = notificationService.v2_getUnreadCount(userId);
		return ApiResponse.ok("읽지 않은 알림수", new UnreadCountResponseDto(count));
	}

	/**
	 * 개별 알림 읽음 처리
	 */
	@PatchMapping("/v2/notifications/{notificationId}/read")
	public ApiResponse<Void> v2_markAsRead(
		@PathVariable Long notificationId
	) {
		Long userId = httpRequestContext.getUserId();
		notificationService.v2_markAsRead(notificationId, userId);

		return ApiResponse.ok("개별 알림을 읽음 처리 하였습니다.", null);
	}

	/**
	 * 전체 알림 읽음 처리
	 */
	@PatchMapping("/v2/notifications/read-all")
	public ApiResponse<Void> v2_markAllAsRead() {
		Long userId = httpRequestContext.getUserId();
		notificationService.v2_markAllAsRead(userId);
		return ApiResponse.ok("모든 알림을 읽음 처리 하였습니다.", null);
	}
}
