package com.back.api.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "읽지 않은 알림 개수 조회 응답용 DTO")
public record UnreadCountResponseDto(
	long unreadCount
) { }
