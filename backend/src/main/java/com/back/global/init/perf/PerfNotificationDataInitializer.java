package com.back.global.init.perf;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.back.domain.event.entity.Event;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.enums.DomainName;
import com.back.domain.notification.enums.NotificationTypeDetails;
import com.back.domain.notification.enums.NotificationTypes;
import com.back.domain.notification.repository.NotificationRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 부하테스트용 알림 데이터 초기화
 * - 1~500번 유저에 대해 1~4번 이벤트의 다양한 알림 생성
 */
@Component
@Slf4j
@RequiredArgsConstructor
@Profile("perf")
public class PerfNotificationDataInitializer {

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;
	private final EventRepository eventRepository;
	private final Random random = new Random(42); // 재현 가능한 랜덤

	public void init() {
		if (notificationRepository.count() > 0) {
			log.info("Notification 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
			return;
		}

		List<User> users = userRepository.findAll();
		if (users.isEmpty()) {
			log.warn("User 데이터가 없습니다. PerfUserDataInitializer를 먼저 실행해주세요.");
			return;
		}

		// Event #1~4 조회
		List<Event> events = eventRepository.findAllById(List.of(1L, 2L, 3L, 4L));
		if (events.isEmpty()) {
			log.warn("Event 데이터가 없습니다. PerfEventDataInitializer를 먼저 실행해주세요.");
			return;
		}

		log.info("Notification 초기 데이터 생성 중: {}명의 사용자 × {}개의 이벤트", users.size(), events.size());

		List<Notification> notifications = new ArrayList<>();

		// 각 유저별로 1~4번 이벤트에 대한 알림 생성
		for (User user : users) {
			for (Event event : events) {
				// 각 이벤트당 1~3개의 랜덤 알림 생성
				int notificationCount = random.nextInt(3) + 1; // 1~3개

				for (int i = 0; i < notificationCount; i++) {
					Notification notification = createRandomNotification(user, event);
					notifications.add(notification);
				}
			}
		}

		notificationRepository.saveAll(notifications);

		long unreadCount = notifications.stream().filter(n -> !n.isRead()).count();
		log.info("✅ Notification 데이터 생성 완료: 총 {}개 (읽음: {}, 안읽음: {})",
			notifications.size(),
			notifications.size() - unreadCount,
			unreadCount);
	}

	/**
	 * 랜덤한 타입의 알림 생성
	 */
	private Notification createRandomNotification(User user, Event event) {
		// 알림 타입 랜덤 선택
		NotificationType[] types = NotificationType.values();
		NotificationType selectedType = types[random.nextInt(types.length)];

		// 읽음 상태 랜덤 설정 (70% 확률로 읽음 처리)
		boolean isRead = random.nextDouble() < 0.7;
		LocalDateTime readAt = isRead ? LocalDateTime.now().minusDays(random.nextInt(30)) : null;

		Notification.NotificationBuilder builder = Notification.builder()
			.user(user)
			.type(selectedType.notificationType)
			.typeDetail(selectedType.typeDetail)
			.domainName(selectedType.domainName)
			.domainId(event.getId())
			.title(selectedType.getTitle())
			.message(selectedType.getMessage(event.getTitle()))
			.isRead(isRead)
			.readAt(readAt);

		return builder.build();
	}

	/**
	 * 알림 타입 정의
	 */
	private enum NotificationType {
		TICKET_GET(
			NotificationTypes.TICKET,
			NotificationTypeDetails.TICKET_GET,
			DomainName.ORDERS,
			"티켓 수령 완료"
		) {
			@Override
			String getMessage(String eventName) {
				return String.format("[%s]\n티켓 1매가 발급되었습니다", eventName);
			}
		},

		PAYMENT_SUCCESS(
			NotificationTypes.PAYMENT,
			NotificationTypeDetails.PAYMENT_SUCCESS,
			DomainName.ORDERS,
			"주문 및 결제 완료"
		) {
			@Override
			String getMessage(String eventName) {
				long amount = 50000 + new Random().nextInt(150000); // 50,000 ~ 200,000
				return String.format("[%s] 티켓 1매가 결제되었습니다\n결제금액: %,d원", eventName, amount);
			}
		},

		PAYMENT_FAILED(
			NotificationTypes.PAYMENT,
			NotificationTypeDetails.PAYMENT_FAILED,
			DomainName.ORDERS,
			"결제 실패"
		) {
			@Override
			String getMessage(String eventName) {
				return String.format("[%s] 결제에 실패했습니다\n다시 시도해주세요", eventName);
			}
		},

		PRE_REGISTER_DONE(
			NotificationTypes.PRE_REGISTER,
			NotificationTypeDetails.PRE_REGISTER_DONE,
			DomainName.PRE_REGISTER,
			"사전등록 완료"
		) {
			@Override
			String getMessage(String eventName) {
				return String.format("[%s]\n사전등록이 완료되었습니다.\n티켓팅 시작일에 알림을 보내드리겠습니다.", eventName);
			}
		},

		TICKETING_POSSIBLE(
			NotificationTypes.QUEUE_ENTRIES,
			NotificationTypeDetails.TICKETING_POSSIBLE,
			DomainName.QUEUE_ENTRIES,
			"티켓팅 시작"
		) {
			@Override
			String getMessage(String eventName) {
				return String.format("[%s]\n입장 준비가 완료되었습니다.\n이제 티켓을 구매하실 수 있습니다.", eventName);
			}
		},

		TICKETING_EXPIRED(
			NotificationTypes.QUEUE_ENTRIES,
			NotificationTypeDetails.TICKETING_EXPIRED,
			DomainName.QUEUE_ENTRIES,
			"티켓팅 만료"
		) {
			@Override
			String getMessage(String eventName) {
				return String.format("[%s]\n입장 시간이 만료되었습니다.", eventName);
			}
		};

		final NotificationTypes notificationType;
		final NotificationTypeDetails typeDetail;
		final DomainName domainName;
		final String title;

		NotificationType(
			NotificationTypes notificationType,
			NotificationTypeDetails typeDetail,
			DomainName domainName,
			String title
		) {
			this.notificationType = notificationType;
			this.typeDetail = typeDetail;
			this.domainName = domainName;
			this.title = title;
		}

		String getTitle() {
			return title;
		}

		abstract String getMessage(String eventName);
	}
}
