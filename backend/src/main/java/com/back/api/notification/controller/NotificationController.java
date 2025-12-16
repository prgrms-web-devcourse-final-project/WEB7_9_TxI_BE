package com.back.api.notification.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.notification.dto.NotificationResponseDto;
import com.back.api.notification.dto.UnreadCountResponseDto;
import com.back.api.notification.service.NotificationService;
import com.back.global.response.ApiResponse;
import com.back.global.security.SecurityUser;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController implements NotificationApi {

	private final NotificationService notificationService;

	@Override
	@GetMapping
	public ApiResponse<List<NotificationResponseDto>> getNotifications(
		@AuthenticationPrincipal SecurityUser securityUser
	) {
		Long userId = securityUser.getId();

		List<NotificationResponseDto> notifications =
			notificationService.getNotifications(userId);

		return ApiResponse.ok(notifications);
	}

	/**
	 * 읽지 않은 알림 개수 조회
	 */
	@Override
	@GetMapping("/unread-count")
	public ApiResponse<UnreadCountResponseDto> getUnreadCount(
		@AuthenticationPrincipal SecurityUser securityUser
	) {
		long count = notificationService.getUnreadCount(securityUser.getId());
		return ApiResponse.ok(new UnreadCountResponseDto(count));
	}

	/**
	 * 개별 알림 읽음 처리
	 */
	@Override
	@PatchMapping("/{notificationId}/read")
	public ApiResponse<Void> markAsRead(
		@AuthenticationPrincipal SecurityUser securityUser,
		@PathVariable Long notificationId
	) {
		notificationService.markAsRead(notificationId, securityUser.getId());

		return ApiResponse.noContent("개별 알림을 읽음 처리 하였습니다.");
	}

	/**
	 * 전체 알림 읽음 처리
	 */
	@Override
	@PatchMapping("/read-all")
	public ApiResponse<Void> markAllAsRead(
		@AuthenticationPrincipal SecurityUser securityUser
	) {
		notificationService.markAllAsRead(securityUser.getId());
		return ApiResponse.noContent("모든 알림을 읽음 처리 하였습니다.");
	}
}
