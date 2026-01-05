package com.back.api.notification.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;

import com.back.api.notification.dto.UnreadCountResponseDto;
import com.back.api.notification.dto.v2.V2_NotificationResponseDto;
import com.back.global.config.swagger.ApiErrorCode;
import com.back.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Notification API", description = "알림 API")
public interface NotificationApi {
	@Operation(summary = "알림 조회 (최신 20개)", description = "웹소켓이 연결되기 전 발생한 알림들을 조회할 수 있습니다(초기 데이터 로딩). 웹소켓이 연결되면 새로운 알림은 웹소켓으로 전달됩니다 ")
	@ApiErrorCode({
		"NOTIFICATION_NOT_FOUND",
		"NOTIFICATION_ACCESS_DENIED"
	})
	ApiResponse<List<V2_NotificationResponseDto>> v2_getNotifications(
	);

	@Operation(summary = "읽지 않은 알림 개수 조회", description = "웹소켓이 연결된 직후 초기 데이터 로딩을 위해 이용됩니다")
	@ApiErrorCode({
		"NOTIFICATION_ACCESS_DENIED"
	})
	ApiResponse<UnreadCountResponseDto> v2_getUnreadCount(
	);

	@Operation(summary = "단일 알림 읽음 처리")
	@ApiErrorCode({
		"NOTIFICATION_ACCESS_DENIED",
		"INVALID_NOTIFICATION_ID",
		"NOTIFICATION_PROCESS_FAILED"
	})
	ApiResponse<Void> v2_markAsRead(
		@PathVariable Long notificationId
	);

	@Operation(summary = "모든 알림 읽음 처리")
	@ApiErrorCode({
		"NOTIFICATION_ACCESS_DENIED",
		"INVALID_NOTIFICATION_ID",
		"NOTIFICATION_PROCESS_FAILED"
	})
	ApiResponse<Void> v2_markAllAsRead(
	);

}
