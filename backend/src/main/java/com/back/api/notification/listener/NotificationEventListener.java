package com.back.api.notification.listener;

import java.util.NoSuchElementException;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.back.api.notification.dto.NotificationResponseDto;
import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.repository.NotificationRepository;
import com.back.domain.notification.systemMessage.NotificationMessage;
import com.back.domain.user.repository.UserRepository;
import com.back.global.websocket.session.WebSocketSessionManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventListener {
	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;
	private final SimpMessagingTemplate messagingTemplate;
	private final WebSocketSessionManager sessionManager;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleNotificationMessage(NotificationMessage message) {
		try {
			Notification notification = Notification.builder()
				.user(
					userRepository.findById(message.getUserId())
						.orElseThrow(
							() -> new NoSuchElementException("ID " + message.getUserId() + "에 해당하는 사용자가 존재하지 않습니다."))
				)
				.type(message.getNotificationType())
				.typeDetail(message.getTypeDetail())
				.domainName(message.getDomainName())
				.domainId(message.getDomainId())
				.title(message.getTitle())
				.message(message.getMessage())
				.isRead(false)
				.build();

			notificationRepository.save(notification);
			log.info("알림 생성 완료 - userId: {}, type: {}, from: {}",
				message.getUserId(),
				message.getNotificationType(),
				message.getDomainName());

			// 웹소켓으로 실시간 알림 전송
			sendNotificationViaWebSocket(message.getUserId(), notification);

		} catch (Exception e) {
			log.error("알림 생성 실패 - userId: {}, type: {}",
				message.getUserId(),
				message.getNotificationType(),
				e);
			// 알림 생성 실패가 원본 트랜잭션에 영향 주지 않음
		}
	}

	/**
	 * 웹소켓으로 실시간 알림 전송
	 *
	 * @param userId 대상 사용자 ID
	 * @param notification 전송할 알림 엔티티
	 */
	private void sendNotificationViaWebSocket(Long userId, Notification notification) {
		// 사용자 온라인 여부 확인
		if (!sessionManager.isUserOnline(userId)) {
			log.debug("사용자 오프라인 - 웹소켓 전송 생략 - userId: {}", userId);
			return;
		}

		try {
			// DTO 변환
			NotificationResponseDto dto = NotificationResponseDto.from(notification);

			// 웹소켓 전송
			messagingTemplate.convertAndSendToUser(
				userId.toString(),
				"/notifications",
				dto
			);

			log.info("웹소켓 전송 성공 - userId: {}, notificationId: {}", userId, notification.getId());

		} catch (Exception e) {
			log.warn("웹소켓 전송 실패 - userId: {}, notificationId: {}, error: {}",
				userId, notification.getId(), e.getMessage());
			// 전송 실패해도 DB에는 저장되어 있으므로 예외를 던지지 않음
		}
	}
}
