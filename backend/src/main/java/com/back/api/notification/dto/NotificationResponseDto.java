package com.back.api.notification.dto;

import java.time.LocalDateTime;

import com.back.domain.notification.entity.Notification;

public record NotificationResponseDto(
	Long id,
	String type,
	String title,
	String content,
	LocalDateTime createdAt,
	Boolean isRead,
	LocalDateTime readAt
) {
	public static NotificationResponseDto from(Notification notification) {

		return new NotificationResponseDto(
			notification.getId(),
			notification.getType().getFrontType().name(),
			notification.getTitle(),
			notification.getContent(),
			notification.getCreateAt(),
			notification.isRead(),
			notification.getReadAt()
		);
	}
}
