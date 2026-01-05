package com.back.api.notification.dto.v1;

import java.time.LocalDateTime;

import com.back.domain.notification.entity.Notification;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "알림 조회 응답용 DTO")
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
	public static NotificationResponseDto from(Notification notification) {
		return new NotificationResponseDto(
			notification.getId(),
			notification.getType().name(),
			notification.getTypeDetail().name(),
			notification.getTitle(),
			notification.getMessage(),
			notification.isRead(),
			notification.getCreateAt(),
			notification.getReadAt()
		);
	}
}
