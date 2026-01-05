package com.back.api.notification.dto.v2;

import java.time.LocalDateTime;

import com.back.domain.notification.entity.V2_Notification;

public record V2_NotificationResponseDto(
	Long id,
	String type,
	String title,
	String content,
	LocalDateTime createdAt,
	Boolean isRead,
	LocalDateTime readAt
) {
	public static V2_NotificationResponseDto from(V2_Notification notification) {

		return new V2_NotificationResponseDto(
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
