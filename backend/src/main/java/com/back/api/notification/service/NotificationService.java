package com.back.api.notification.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.notification.dto.v1.NotificationResponseDto;
import com.back.api.notification.dto.v2.V2_NotificationResponseDto;
import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.entity.V2_Notification;
import com.back.domain.notification.repository.NotificationRepository;
import com.back.domain.notification.repository.V2_NotificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
	private final NotificationRepository notificationRepository;
	private final V2_NotificationRepository v2_notificationRepository;

	public List<NotificationResponseDto> getNotifications(Long userId) {
		List<Notification> notifications = notificationRepository
			.findTop20ByUserIdOrderByCreateAtDesc(userId);

		return notifications.stream()
			.map(NotificationResponseDto::from)
			.toList();
	}

	public long getUnreadCount(Long userId) {
		return notificationRepository.countByUserIdAndIsReadFalse(userId);
	}

	@Transactional
	public void markAsRead(Long notificationId, Long userId) {
		Notification notification = notificationRepository
			.findByIdAndUserId(notificationId, userId)
			.orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다"));

		notification.markAsRead();
	}

	@Transactional
	public void markAllAsRead(Long userId) {
		List<Notification> notifications = notificationRepository
			.findByUserIdAndIsReadFalse(userId);

		notifications.forEach(Notification::markAsRead);
	}

	public List<V2_NotificationResponseDto> v2_getNotifications(Long userId) {
		List<V2_Notification> notifications = v2_notificationRepository
			.findTop20ByUserIdOrderByCreateAtDesc(userId);

		return notifications.stream()
			.map(V2_NotificationResponseDto::from)
			.toList();
	}

	public long v2_getUnreadCount(Long userId) {
		return v2_notificationRepository.countByUserIdAndIsReadFalse(userId);
	}

	@Transactional
	public void v2_markAsRead(Long notificationId, Long userId) {
		V2_Notification notification = v2_notificationRepository
			.findByIdAndUserId(notificationId, userId)
			.orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다"));

		notification.markAsRead();
	}

	@Transactional
	public void v2_markAllAsRead(Long userId) {
		List<V2_Notification> notifications = v2_notificationRepository
			.findByUserIdAndIsReadFalse(userId);

		notifications.forEach(V2_Notification::markAsRead);
	}
}
