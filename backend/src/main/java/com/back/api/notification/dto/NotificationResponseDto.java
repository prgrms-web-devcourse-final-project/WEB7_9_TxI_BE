package com.back.api.notification.dto;

import java.time.LocalDateTime;

import com.back.domain.notification.entity.Notification;

// 알림 목록 응답용 DTO

public record NotificationResponseDto(
	Long id,
	String type,          // enum name
	String typeDetail,
	String title,
	String message,
	boolean isRead,
	LocalDateTime createdAt,
	LocalDateTime readAt
) {
	public static NotificationResponseDto from(Notification n) {
		return new NotificationResponseDto(
			n.getId(),
			n.getType().name(),
			n.getTypeDetail().name(),
			n.getTitle(),
			n.getMessage(),
			n.isRead(),
			n.getCreateAt(),
			n.getReadAt()
		);
	}
}
