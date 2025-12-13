package com.back.api.notification.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.notification.dto.NotificationResponseDto;
import com.back.api.notification.dto.UnreadCountResponseDto;
import com.back.api.notification.service.NotificationService;
import com.back.global.security.SecurityUser;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

	private final NotificationService notificationService;

	@GetMapping
	public ResponseEntity<List<NotificationResponseDto>> getNotifications(
		@AuthenticationPrincipal SecurityUser securityUser
	) {
		Long userId = securityUser.getId();

		List<NotificationResponseDto> notifications =
			notificationService.getNotifications(userId);

		return ResponseEntity.ok(notifications);
	}

	/**
	 * 읽지 않은 알림 개수 조회
	 */
	@GetMapping("/unread-count")
	public ResponseEntity<UnreadCountResponseDto> getUnreadCount(
		@AuthenticationPrincipal SecurityUser securityUser
	) {
		long count = notificationService.getUnreadCount(securityUser.getId());
		return ResponseEntity.ok(new UnreadCountResponseDto(count));
	}

	/**
	 * 개별 알림 읽음 처리
	 */
	@PatchMapping("/{notificationId}/read")
	public ResponseEntity<Void> markAsRead(
		@AuthenticationPrincipal SecurityUser securityUser,
		@PathVariable Long notificationId
	) {
		notificationService.markAsRead(notificationId, securityUser.getId());

		return ResponseEntity.ok().build();
	}

	/**
	 * 전체 알림 읽음 처리
	 */
	@PatchMapping("/read-all")
	public ResponseEntity<Void> markAllAsRead(
		@AuthenticationPrincipal SecurityUser securityUser
	) {
		notificationService.markAllAsRead(securityUser.getId());
		return ResponseEntity.ok().build();
	}
}
