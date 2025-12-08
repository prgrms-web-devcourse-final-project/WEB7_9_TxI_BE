package com.back.api.notification.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.domain.event.entity.Event;
import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.model.NotificationTypeDetails;
import com.back.domain.notification.model.NotificationTypes;
import com.back.domain.notification.repository.NotificationRepository;
import com.back.domain.user.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationService {
	private final NotificationRepository notificationRepository;

	public Notification createNotification(User user,
		Event event,
		NotificationTypes type,
		NotificationTypeDetails typeDetail,
		String title,
		String message) {

		Notification notification = Notification.create(
			type,
			typeDetail,
			title,
			message,
			user,
			event
		);

		return notificationRepository.save(notification);
	}
}
