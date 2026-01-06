package com.back.global.init;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.back.domain.event.entity.Event;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.enums.DomainName;
import com.back.domain.notification.repository.NotificationRepository;
import com.back.domain.notification.systemMessage.NotificationMessage;
import com.back.domain.notification.systemMessage.OrderFailedMessage;
import com.back.domain.notification.systemMessage.OrderSuccessMessage;
import com.back.domain.notification.systemMessage.PreRegisterDoneMessage;
import com.back.domain.notification.systemMessage.QueueEntriesMessage;
import com.back.domain.notification.systemMessage.TicketGetMessage;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
@Order(5)
public class NotificationDataInit implements ApplicationRunner {

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;
	private final EventRepository eventRepository;

	@Override
	public void run(ApplicationArguments args) {
		if (notificationRepository.count() > 0) {
			log.info("Notification 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
			return;
		}

		log.info("Notification 초기 데이터를 생성합니다.");

		// 유저 1번, 2번 조회
		Optional<User> user1Opt = userRepository.findById(1L);
		Optional<User> user2Opt = userRepository.findById(2L);

		if (user1Opt.isEmpty() || user2Opt.isEmpty()) {
			log.warn("User ID 1번 또는 2번이 없습니다. UserDataInit을 먼저 실행해주세요.");
			return;
		}

		// 이벤트 1번 조회
		Optional<Event> eventOpt = eventRepository.findById(1L);
		if (eventOpt.isEmpty()) {
			log.warn("Event ID 1번이 없습니다. EventDataInit을 먼저 실행해주세요.");
			return;
		}

		User user1 = user1Opt.get();
		Event event = eventOpt.get();
		String eventName = event.getTitle();

		List<Notification> notifications = new ArrayList<>();

		// ===== 유저 1번 알림 (총 5개) =====
		// 안읽은 알림 3개
		notifications.add(createNotificationFromMessage(user1,
			new OrderSuccessMessage(user1.getId(), 1L, 99000L, eventName), false));

		notifications.add(createNotificationFromMessage(user1,
			new QueueEntriesMessage(user1.getId(), 101L, eventName), false));

		notifications.add(createNotificationFromMessage(user1,
			new PreRegisterDoneMessage(user1.getId(), 201L, eventName), false));

		// 읽은 알림 2개
		notifications.add(createNotificationFromMessage(user1,
			new TicketGetMessage(user1.getId(), DomainName.ORDERS, 301L, eventName), true));

		notifications.add(createNotificationFromMessage(user1,
			new OrderFailedMessage(user1.getId(), 154000L, 51L, eventName), true));

		// ===== 유저 1번 알림 (총 5개) =====
		// 안읽은 알림 3개
		notifications.add(createNotificationFromMessage(user1,
			new PreRegisterDoneMessage(user1.getId(), 202L, eventName), false));

		notifications.add(createNotificationFromMessage(user1,
			new QueueEntriesMessage(user1.getId(), 102L, eventName), false));

		notifications.add(createNotificationFromMessage(user1,
			new OrderSuccessMessage(user1.getId(), 2L, 154000L, eventName), false));

		// 읽은 알림 2개
		notifications.add(createNotificationFromMessage(user1,
			new OrderFailedMessage(user1.getId(), 99000L, 52L, eventName), true));

		notifications.add(createNotificationFromMessage(user1,
			new TicketGetMessage(user1.getId(), DomainName.ORDERS, 302L, eventName), true));

		notifications.add(createNotificationFromMessage(user1,
			new OrderSuccessMessage(user1.getId(), 1L, 99000L, eventName), false));

		notifications.add(createNotificationFromMessage(user1,
			new QueueEntriesMessage(user1.getId(), 101L, eventName), false));

		notifications.add(createNotificationFromMessage(user1,
			new PreRegisterDoneMessage(user1.getId(), 201L, eventName), false));

		// 읽은 알림 2개
		notifications.add(createNotificationFromMessage(user1,
			new TicketGetMessage(user1.getId(), DomainName.ORDERS, 301L, eventName), true));

		notifications.add(createNotificationFromMessage(user1,
			new OrderFailedMessage(user1.getId(), 154000L, 51L, eventName), true));

		// ===== 유저 1번 알림 (총 5개) =====
		// 안읽은 알림 3개
		notifications.add(createNotificationFromMessage(user1,
			new PreRegisterDoneMessage(user1.getId(), 202L, eventName), false));

		notifications.add(createNotificationFromMessage(user1,
			new QueueEntriesMessage(user1.getId(), 102L, eventName), false));

		notifications.add(createNotificationFromMessage(user1,
			new OrderSuccessMessage(user1.getId(), 2L, 154000L, eventName), false));

		// 읽은 알림 2개
		notifications.add(createNotificationFromMessage(user1,
			new OrderFailedMessage(user1.getId(), 99000L, 52L, eventName), true));

		notifications.add(createNotificationFromMessage(user1,
			new TicketGetMessage(user1.getId(), DomainName.ORDERS, 302L, eventName), true));

		notifications.add(createNotificationFromMessage(user1,
			new OrderSuccessMessage(user1.getId(), 1L, 99000L, eventName), false));

		notifications.add(createNotificationFromMessage(user1,
			new QueueEntriesMessage(user1.getId(), 101L, eventName), false));

		notifications.add(createNotificationFromMessage(user1,
			new PreRegisterDoneMessage(user1.getId(), 201L, eventName), false));

		// 읽은 알림 2개
		notifications.add(createNotificationFromMessage(user1,
			new TicketGetMessage(user1.getId(), DomainName.ORDERS, 301L, eventName), true));

		notifications.add(createNotificationFromMessage(user1,
			new OrderFailedMessage(user1.getId(), 154000L, 51L, eventName), true));

		// ===== 유저 1번 알림 (총 5개) =====
		// 안읽은 알림 3개
		notifications.add(createNotificationFromMessage(user1,
			new PreRegisterDoneMessage(user1.getId(), 202L, eventName), false));

		notifications.add(createNotificationFromMessage(user1,
			new QueueEntriesMessage(user1.getId(), 102L, eventName), false));

		notifications.add(createNotificationFromMessage(user1,
			new OrderSuccessMessage(user1.getId(), 2L, 154000L, eventName), false));

		// 읽은 알림 2개
		notifications.add(createNotificationFromMessage(user1,
			new OrderFailedMessage(user1.getId(), 99000L, 52L, eventName), true));

		notifications.add(createNotificationFromMessage(user1,
			new TicketGetMessage(user1.getId(), DomainName.ORDERS, 302L, eventName), true));

		notificationRepository.saveAll(notifications);

		log.info("Notification 초기 데이터 {}개가 생성되었습니다.", notifications.size());
	}

	/**
	 * NotificationMessage로부터 Notification 엔티티 생성
	 *
	 * @param user 대상 유저
	 * @param message 알림 메시지
	 * @param isRead 읽음 여부
	 */
	private Notification createNotificationFromMessage(User user, NotificationMessage message, boolean isRead) {
		Notification notification = Notification.builder()
			.user(user)
			.type(message.getNotificationType())
			.typeDetail(message.getTypeDetail())
			.domainName(message.getDomainName())
			.domainId(message.getDomainId())
			.title(message.getTitle())
			.message(message.getMessage())
			.isRead(false)
			.build();

		// 읽음 처리
		if (isRead) {
			notification.markAsRead();
		}

		return notification;
	}
}
