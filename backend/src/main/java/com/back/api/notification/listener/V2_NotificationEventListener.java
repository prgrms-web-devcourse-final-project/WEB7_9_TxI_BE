package com.back.api.notification.listener;

import java.util.NoSuchElementException;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.back.api.notification.dto.v2.V2_NotificationResponseDto;
import com.back.domain.notification.entity.V2_Notification;
import com.back.domain.notification.repository.V2_NotificationRepository;
import com.back.domain.notification.systemMessage.v2.V2_NotificationMessage;
import com.back.domain.user.repository.UserRepository;
import com.back.global.websocket.session.WebSocketSessionManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class V2_NotificationEventListener {
	private final V2_NotificationRepository notificationRepository;
	private final UserRepository userRepository;
	private final SimpMessagingTemplate messagingTemplate;
	private final WebSocketSessionManager sessionManager;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleNotificationMessage(V2_NotificationMessage message) {
		try {
			V2_Notification notification = V2_Notification.builder()
				.user(
					userRepository.findById(message.getUserId())
						.orElseThrow(
							() -> new NoSuchElementException("ID " + message.getUserId() + "에 해당하는 사용자가 존재하지 않습니다."))
				)
				.type(message.getNotificationVar())
				.domainName(message.getDomainName())
				.title(message.getNotificationVar().getTitle())
				.content(message.getNotificationVar().formatMessage(message.getContext()))
				.isRead(false)
				.build();

			notificationRepository.save(notification);

			// 웹소켓으로 실시간 알림 전송
			v2_sendNotificationViaWebSocket(message.getUserId(), notification);

		} catch (Exception e) {
		}
	}

	/**
	 * 웹소켓으로 실시간 알림 전송
	 *
	 * @param userId 대상 사용자 ID
	 * @param notification 전송할 알림 엔티티
	 */
	private void v2_sendNotificationViaWebSocket(Long userId, V2_Notification notification) {

		boolean isOnline = sessionManager.isUserOnline(userId);

		if (!isOnline) {
			//log.debug("사용자 오프라인 - 웹소켓 전송 생략 - userId: {}", userId);
			return;
		}

		try {
			V2_NotificationResponseDto dto = V2_NotificationResponseDto.from(notification);

			// convertAndSendToUser 대신 직접 경로로 전송
			String directDestination = "/user/" + userId + "/notifications";

			messagingTemplate.convertAndSend(directDestination, dto);

			//log.info("=== 웹소켓 전송 성공 - userId: {}, notificationId: {}", userId, notification.getId());

		} catch (Exception e) {
			//log.error("=== 웹소켓 전송 실패 - userId: {}, error: {}", userId, e.getMessage(), e);
		}
	}
}
