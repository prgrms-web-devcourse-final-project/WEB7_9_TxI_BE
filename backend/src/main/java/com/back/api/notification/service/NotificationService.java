package com.back.api.notification.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.notification.dto.NotificationResponseDto;
import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
	private final NotificationRepository notificationRepository;

	public List<NotificationResponseDto> getNotifications(Long userId) {
		List<Notification> notifications = notificationRepository
			.findByUserIdOrderByCreateAtDesc(userId);

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
}
