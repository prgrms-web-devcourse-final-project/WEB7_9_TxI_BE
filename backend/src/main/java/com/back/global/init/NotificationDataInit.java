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
import com.back.domain.notification.entity.V2_Notification;
import com.back.domain.notification.enums.DomainName;
import com.back.domain.notification.enums.v2.V2_NotificationVar;
import com.back.domain.notification.repository.V2_NotificationRepository;
import com.back.domain.notification.systemMessage.v2.V2_NotificationMessage;
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

	private final V2_NotificationRepository notificationRepository;
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
		User user2 = user2Opt.get();
		Event event = eventOpt.get();
		String eventTitle = event.getTitle();

		List<V2_Notification> notifications = new ArrayList<>();

		// ===== 유저 1번 알림 =====
		// 회원가입 알림
		notifications.add(createNotification(
			user1,
			V2_NotificationMessage.signUp(user1.getId(), user1.getNickname()),
			false
		));

		// 사전등록 완료
		notifications.add(createNotification(
			user1,
			V2_NotificationMessage.preRegisterDone(user1.getId(), eventTitle),
			false
		));

		// 대기열 대기중
		notifications.add(createNotification(
			user1,
			V2_NotificationMessage.queueWaiting(user1.getId(), eventTitle, 42L),
			false
		));

		// 대기열 입장
		notifications.add(createNotification(
			user1,
			V2_NotificationMessage.queueEntered(user1.getId(), eventTitle),
			true
		));

		// 결제 성공
		notifications.add(createNotification(
			user1,
			V2_NotificationMessage.paymentSuccess(user1.getId(), eventTitle, 99000L),
			true
		));

		// ===== 유저 2번 알림 =====
		// 회원가입 알림
		notifications.add(createNotification(
			user2,
			V2_NotificationMessage.signUp(user2.getId(), user2.getNickname()),
			false
		));

		// 사전등록 완료
		notifications.add(createNotification(
			user2,
			V2_NotificationMessage.preRegisterDone(user2.getId(), eventTitle),
			false
		));

		// 대기열 대기중
		notifications.add(createNotification(
			user2,
			V2_NotificationMessage.queueWaiting(user2.getId(), eventTitle, 128L),
			false
		));

		// 대기열 만료
		notifications.add(createNotification(
			user2,
			V2_NotificationMessage.queueExpired(user2.getId(), eventTitle),
			false
		));

		// 결제 성공
		notifications.add(createNotification(
			user2,
			V2_NotificationMessage.paymentSuccess(user2.getId(), eventTitle, 154000L),
			true
		));

		notificationRepository.saveAll(notifications);

		log.info("Notification 초기 데이터 {}개가 생성되었습니다.", notifications.size());
	}

	/**
	 * V2_NotificationMessage로부터 V2_Notification 엔티티 생성
	 *
	 * @param user 대상 유저
	 * @param message 알림 메시지
	 * @param isRead 읽음 여부
	 */
	private V2_Notification createNotification(
		User user,
		V2_NotificationMessage message,
		boolean isRead
	) {
		V2_NotificationVar notificationVar = message.getNotificationVar();

		V2_Notification notification = V2_Notification.builder()
			.user(user)
			.type(notificationVar)
			.title(notificationVar.getTitle())
			.content(notificationVar.formatMessage(message.getContext()))
			.domainName(message.getDomainName())
			.isRead(false)
			.build();

		// 읽음 처리
		if (isRead) {
			notification.markAsRead();
		}

		return notification;
	}
}
