package com.back.api.notification.listener;

import java.util.NoSuchElementException;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.repository.NotificationRepository;
import com.back.domain.notification.systemMessage.NotificationMessage;
import com.back.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventListener {
	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleNotificationMessage(NotificationMessage message) {
		try {
			Notification notification = Notification.builder()
				.user(
					userRepository.findById(message.getUserId())
					.orElseThrow(() -> new NoSuchElementException("ID " + message.getUserId() + "에 해당하는 사용자가 존재하지 않습니다."))
				)
				.type(message.getNotificationType())
				.typeDetail(message.getTypeDetail())
				.fromWhere(message.getFromWhere())
				.whereId(message.getWhereId())
				.title(message.getTitle())
				.message(message.getMessage())
				.isRead(false)
				.build();

			notificationRepository.save(notification);
			//TODO : 로그 컨벤션대로 나중에 수정할것
			log.info("알림 생성 완료 - userId: {}, type: {}, from: {}",
				message.getUserId(),
				message.getNotificationType(),
				message.getFromWhere());

		} catch (Exception e) {
			log.error("알림 생성 실패 - userId: {}, type: {}",
				message.getUserId(),
				message.getNotificationType(),
				e);
			// 알림 생성 실패가 원본 트랜잭션에 영향 주지 않음
		}
	}
}
