package com.back.api.notification.listener;

import static java.util.concurrent.TimeUnit.*;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.reset;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.back.api.notification.dto.NotificationResponseDto;
import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventCategory;
import com.back.domain.event.entity.EventStatus;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.enums.DomainName;
import com.back.domain.notification.enums.NotificationTypeDetails;
import com.back.domain.notification.enums.NotificationTypes;
import com.back.domain.notification.repository.NotificationRepository;
import com.back.domain.notification.systemMessage.OrderFailedMessage;
import com.back.domain.notification.systemMessage.OrderSuccessMessage;
import com.back.domain.notification.systemMessage.PreRegisterDoneMessage;
import com.back.domain.notification.systemMessage.QueueEntriesMessage;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserActiveStatus;
import com.back.domain.user.entity.UserRole;
import com.back.domain.user.repository.UserRepository;
import com.back.global.websocket.session.WebSocketSessionManager;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("NotificationEventListener 통합 테스트")
class NotificationEventListenerTest {

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private TransactionHelper transactionHelper;

	@MockBean
	private SimpMessagingTemplate messagingTemplate;

	@MockBean
	private WebSocketSessionManager sessionManager;

	private User testUser;
	private Event testEvent;

	@BeforeEach
	void setUp() {
		// 기존 데이터 정리 (테스트 격리를 위해)
		notificationRepository.deleteAll();
		eventRepository.deleteAll();
		userRepository.deleteAll();

		// 테스트 유저 생성
		testUser = userRepository.save(User.builder()
			.email("test@example.com")
			.nickname("tester")
			.password("encoded_password")
			.fullName("Test User")
			.role(UserRole.NORMAL)
			.activeStatus(UserActiveStatus.ACTIVE)
			.build());

		// 테스트 이벤트 생성
		testEvent = eventRepository.save(Event.builder()
			.title("테스트 이벤트")
			.category(EventCategory.CONCERT)
			.place("테스트 장소")
			.minPrice(50000)
			.maxPrice(150000)
			.preOpenAt(java.time.LocalDateTime.now().plusDays(1))
			.preCloseAt(java.time.LocalDateTime.now().plusDays(3))
			.ticketOpenAt(java.time.LocalDateTime.now().plusDays(5))
			.ticketCloseAt(java.time.LocalDateTime.now().plusDays(7))
			.eventDate(java.time.LocalDateTime.now().plusDays(10))
			.maxTicketAmount(1000)
			.status(EventStatus.READY)
			.build());

		// Mock 초기화
		reset(messagingTemplate, sessionManager);
	}

	@Nested
	@DisplayName("OrdersSuccessMessage 테스트")
	class OrdersSuccessMessageTest {

		@Test
		@DisplayName("온라인 유저 - DB 저장 및 웹소켓 전송")
		void publishOrdersSuccessMessage_UserOnline_SavesAndSendsWebSocket() {
			// given
			given(sessionManager.isUserOnline(testUser.getId())).willReturn(true);

			// when: 이벤트 발행 (별도 트랜잭션으로 커밋되어야 AFTER_COMMIT 리스너 실행)
			transactionHelper.executeInNewTransaction(() -> {
				OrderSuccessMessage message = new OrderSuccessMessage(
					testUser.getId(),
					1L,
					99000L,
					testEvent.getTitle()
				);
				eventPublisher.publishEvent(message);
			});

			// then: 비동기 대기 후 검증
			await().atMost(5, SECONDS).untilAsserted(() -> {
				// DB 저장 확인
				List<Notification> notifications = notificationRepository
					.findTop20ByUserIdOrderByCreateAtDesc(testUser.getId());
				assertThat(notifications).hasSize(1);

				Notification saved = notifications.get(0);
				assertThat(saved.getType()).isEqualTo(NotificationTypes.PAYMENT);
				assertThat(saved.getTypeDetail()).isEqualTo(NotificationTypeDetails.PAYMENT_SUCCESS);
				assertThat(saved.getDomainName()).isEqualTo(DomainName.ORDERS);
				assertThat(saved.getTitle()).isEqualTo("주문 및 결제 완료");
				assertThat(saved.getMessage()).contains("테스트 이벤트", "99000원");
				assertThat(saved.isRead()).isFalse();

				// 웹소켓 전송 확인
				verify(messagingTemplate).convertAndSend(
					eq("/user/" + testUser.getId() + "/notifications"),
					any(NotificationResponseDto.class)
				);
			});
		}

		@Test
		@DisplayName("오프라인 유저 - DB 저장만, 웹소켓 미전송")
		void publishOrdersSuccessMessage_UserOffline_SavesOnlyWithoutWebSocket() {
			// given
			given(sessionManager.isUserOnline(testUser.getId())).willReturn(false);

			// when
			transactionHelper.executeInNewTransaction(() -> {
				OrderSuccessMessage message = new OrderSuccessMessage(
					testUser.getId(),
					1L,
					99000L,
					testEvent.getTitle()
				);
				eventPublisher.publishEvent(message);
			});

			// then
			await().atMost(3, SECONDS).untilAsserted(() -> {
				// DB 저장 확인
				List<Notification> notifications = notificationRepository
					.findTop20ByUserIdOrderByCreateAtDesc(testUser.getId());
				assertThat(notifications).hasSize(1);

				// 웹소켓 미전송 확인
				verify(messagingTemplate, never()).convertAndSend(
					anyString(),
					any(Object.class)
				);
			});
		}

		@Test
		@DisplayName("알림 타입 및 상세 정보 검증")
		void publishOrdersSuccessMessage_VerifyNotificationTypeDetails() {
			// given
			given(sessionManager.isUserOnline(testUser.getId())).willReturn(true);

			// when
			transactionHelper.executeInNewTransaction(() -> {
				OrderSuccessMessage message = new OrderSuccessMessage(
					testUser.getId(),
					1L,
					99000L,
					testEvent.getTitle()
				);
				eventPublisher.publishEvent(message);
			});

			// then
			await().atMost(3, SECONDS).untilAsserted(() -> {
				List<Notification> notifications = notificationRepository
					.findTop20ByUserIdOrderByCreateAtDesc(testUser.getId());

				Notification saved = notifications.get(0);
				assertThat(saved.getType()).isEqualTo(NotificationTypes.PAYMENT);
				assertThat(saved.getTypeDetail()).isEqualTo(NotificationTypeDetails.PAYMENT_SUCCESS);
				assertThat(saved.getDomainName()).isEqualTo(DomainName.ORDERS);
				assertThat(saved.getDomainId()).isEqualTo(1L);
			});
		}
	}

	@Nested
	@DisplayName("OrderFailedMessage 테스트")
	class OrderFailedMessageTest {

		@Test
		@DisplayName("온라인 유저 - 실패 알림 생성 및 전송")
		void publishOrderFailedMessage_UserOnline_SavesAndSendsWebSocket() {
			// given
			given(sessionManager.isUserOnline(testUser.getId())).willReturn(true);

			// when
			transactionHelper.executeInNewTransaction(() -> {
				OrderFailedMessage message = new OrderFailedMessage(
					testUser.getId(),
					154000L,
					51L,
					testEvent.getTitle()
				);
				eventPublisher.publishEvent(message);
			});

			// then
			await().atMost(3, SECONDS).untilAsserted(() -> {
				List<Notification> notifications = notificationRepository
					.findTop20ByUserIdOrderByCreateAtDesc(testUser.getId());
				assertThat(notifications).hasSize(1);

				Notification saved = notifications.get(0);
				assertThat(saved.getTitle()).isEqualTo("주문 및 결제 실패");
				assertThat(saved.getMessage()).contains("테스트 이벤트", "154000원", "실패");

				verify(messagingTemplate).convertAndSend(
					eq("/user/" + testUser.getId() + "/notifications"),
					any(NotificationResponseDto.class)
				);
			});
		}

		@Test
		@DisplayName("실패 알림 타입 검증")
		void publishOrderFailedMessage_VerifyFailedType() {
			// given
			given(sessionManager.isUserOnline(testUser.getId())).willReturn(true);

			// when
			transactionHelper.executeInNewTransaction(() -> {
				OrderFailedMessage message = new OrderFailedMessage(
					testUser.getId(),
					154000L,
					51L,
					testEvent.getTitle()
				);
				eventPublisher.publishEvent(message);
			});

			// then
			await().atMost(3, SECONDS).untilAsserted(() -> {
				List<Notification> notifications = notificationRepository
					.findTop20ByUserIdOrderByCreateAtDesc(testUser.getId());

				Notification saved = notifications.get(0);
				assertThat(saved.getType()).isEqualTo(NotificationTypes.PAYMENT);
				assertThat(saved.getTypeDetail()).isEqualTo(NotificationTypeDetails.PAYMENT_FAILED);
				assertThat(saved.getDomainName()).isEqualTo(DomainName.ORDERS);
			});
		}
	}

	@Nested
	@DisplayName("PreRegisterDoneMessage 테스트")
	class PreRegisterDoneMessageTest {

		@Test
		@DisplayName("사전등록 완료 알림 생성 및 전송")
		void publishPreRegisterDoneMessage_UserOnline_SavesAndSendsWebSocket() {
			// given
			given(sessionManager.isUserOnline(testUser.getId())).willReturn(true);

			// when
			transactionHelper.executeInNewTransaction(() -> {
				PreRegisterDoneMessage message = new PreRegisterDoneMessage(
					testUser.getId(),
					201L,
					testEvent.getTitle()
				);
				eventPublisher.publishEvent(message);
			});

			// then
			await().atMost(3, SECONDS).untilAsserted(() -> {
				List<Notification> notifications = notificationRepository
					.findTop20ByUserIdOrderByCreateAtDesc(testUser.getId());
				assertThat(notifications).hasSize(1);

				Notification saved = notifications.get(0);
				assertThat(saved.getTitle()).isEqualTo("사전등록 완료");
				assertThat(saved.getMessage()).contains("테스트 이벤트", "사전등록이 완료");

				verify(messagingTemplate).convertAndSend(
					eq("/user/" + testUser.getId() + "/notifications"),
					any(NotificationResponseDto.class)
				);
			});
		}

		@Test
		@DisplayName("사전등록 알림 타입 검증")
		void publishPreRegisterDoneMessage_VerifyType() {
			// given
			given(sessionManager.isUserOnline(testUser.getId())).willReturn(true);

			// when
			transactionHelper.executeInNewTransaction(() -> {
				PreRegisterDoneMessage message = new PreRegisterDoneMessage(
					testUser.getId(),
					201L,
					testEvent.getTitle()
				);
				eventPublisher.publishEvent(message);
			});

			// then
			await().atMost(3, SECONDS).untilAsserted(() -> {
				List<Notification> notifications = notificationRepository
					.findTop20ByUserIdOrderByCreateAtDesc(testUser.getId());

				Notification saved = notifications.get(0);
				assertThat(saved.getType()).isEqualTo(NotificationTypes.PRE_REGISTER);
				assertThat(saved.getTypeDetail()).isEqualTo(NotificationTypeDetails.PRE_REGISTER_DONE);
				assertThat(saved.getDomainName()).isEqualTo(DomainName.PRE_REGISTER);
			});
		}
	}

	@Nested
	@DisplayName("QueueEntriesMessage 테스트")
	class QueueEntriesMessageTest {

		@Test
		@DisplayName("대기열 진입 알림 생성 및 전송")
		void publishQueueEntriesMessage_UserOnline_SavesAndSendsWebSocket() {
			// given
			given(sessionManager.isUserOnline(testUser.getId())).willReturn(true);

			// when
			transactionHelper.executeInNewTransaction(() -> {
				QueueEntriesMessage message = new QueueEntriesMessage(
					testUser.getId(),
					101L,
					testEvent.getTitle()
				);
				eventPublisher.publishEvent(message);
			});

			// then
			await().atMost(3, SECONDS).untilAsserted(() -> {
				List<Notification> notifications = notificationRepository
					.findTop20ByUserIdOrderByCreateAtDesc(testUser.getId());
				assertThat(notifications).hasSize(1);

				Notification saved = notifications.get(0);
				assertThat(saved.getTitle()).isEqualTo("티켓팅 시작");
				assertThat(saved.getMessage()).contains("테스트 이벤트", "입장 준비가 완료");

				verify(messagingTemplate).convertAndSend(
					eq("/user/" + testUser.getId() + "/notifications"),
					any(NotificationResponseDto.class)
				);
			});
		}

		@Test
		@DisplayName("대기열 알림 타입 검증")
		void publishQueueEntriesMessage_VerifyType() {
			// given
			given(sessionManager.isUserOnline(testUser.getId())).willReturn(true);

			// when
			transactionHelper.executeInNewTransaction(() -> {
				QueueEntriesMessage message = new QueueEntriesMessage(
					testUser.getId(),
					101L,
					testEvent.getTitle()
				);
				eventPublisher.publishEvent(message);
			});

			// then
			await().atMost(3, SECONDS).untilAsserted(() -> {
				List<Notification> notifications = notificationRepository
					.findTop20ByUserIdOrderByCreateAtDesc(testUser.getId());

				Notification saved = notifications.get(0);
				assertThat(saved.getType()).isEqualTo(NotificationTypes.QUEUE_ENTRIES);
				assertThat(saved.getTypeDetail()).isEqualTo(NotificationTypeDetails.TICKETING_POSSIBLE);
				assertThat(saved.getDomainName()).isEqualTo(DomainName.QUEUE_ENTRIES);
			});
		}
	}

	@Nested
	@DisplayName("에러 시나리오 테스트")
	class ErrorScenarioTest {

		@Test
		@DisplayName("존재하지 않는 유저 ID - DB 저장 실패")
		void publishMessage_NonExistentUser_DoesNotSaveNotification() {
			// given
			Long nonExistentUserId = 99999L;
			given(sessionManager.isUserOnline(nonExistentUserId)).willReturn(true);

			// when
			transactionHelper.executeInNewTransaction(() -> {
				OrderSuccessMessage message = new OrderSuccessMessage(
					nonExistentUserId,
					1L,
					99000L,
					testEvent.getTitle()
				);
				eventPublisher.publishEvent(message);
			});

			// then: 알림이 생성되지 않음
			await().atMost(3, SECONDS).untilAsserted(() -> {
				List<Notification> notifications = notificationRepository
					.findTop20ByUserIdOrderByCreateAtDesc(nonExistentUserId);
				assertThat(notifications).isEmpty();

				// 웹소켓도 전송되지 않음
				verify(messagingTemplate, never()).convertAndSend(
					anyString(),
					any(Object.class)
				);
			});
		}

		@Test
		@DisplayName("웹소켓 전송 실패 - DB는 저장됨, 예외 전파 안 됨")
		void publishMessage_WebSocketFails_StillSavesNotification() {
			// given
			given(sessionManager.isUserOnline(testUser.getId())).willReturn(true);
			willThrow(new RuntimeException("웹소켓 전송 실패"))
				.given(messagingTemplate).convertAndSend(
					anyString(),
					any(Object.class)
				);

			// when
			transactionHelper.executeInNewTransaction(() -> {
				OrderSuccessMessage message = new OrderSuccessMessage(
					testUser.getId(),
					1L,
					99000L,
					testEvent.getTitle()
				);
				eventPublisher.publishEvent(message);
			});

			// then: DB에는 저장됨 (웹소켓 실패가 DB 저장에 영향 없음)
			await().atMost(3, SECONDS).untilAsserted(() -> {
				List<Notification> notifications = notificationRepository
					.findTop20ByUserIdOrderByCreateAtDesc(testUser.getId());
				assertThat(notifications).hasSize(1);

				// 웹소켓 전송 시도는 있었음
				verify(messagingTemplate).convertAndSend(
					eq("/user/" + testUser.getId() + "/notifications"),
					any(NotificationResponseDto.class)
				);
			});
		}

		@Test
		@DisplayName("여러 이벤트 동시 발행 - 모두 정상 처리")
		void publishMultipleMessages_AllProcessedSuccessfully() {
			// given
			given(sessionManager.isUserOnline(testUser.getId())).willReturn(true);

			// when: 4가지 메시지 타입 연속 발행
			transactionHelper.executeInNewTransaction(() -> {
				eventPublisher.publishEvent(new OrderSuccessMessage(
					testUser.getId(), 1L, 99000L, testEvent.getTitle()));
				eventPublisher.publishEvent(new OrderFailedMessage(
					testUser.getId(), 154000L, 51L, testEvent.getTitle()));
				eventPublisher.publishEvent(new PreRegisterDoneMessage(
					testUser.getId(), 201L, testEvent.getTitle()));
				eventPublisher.publishEvent(new QueueEntriesMessage(
					testUser.getId(), 101L, testEvent.getTitle()));
			});

			// then: 4개 모두 저장됨
			await().atMost(5, SECONDS).untilAsserted(() -> {
				List<Notification> notifications = notificationRepository
					.findTop20ByUserIdOrderByCreateAtDesc(testUser.getId());
				assertThat(notifications).hasSize(4);

				// 각 타입별 확인
				assertThat(notifications)
					.extracting(Notification::getType)
					.containsExactlyInAnyOrder(
						NotificationTypes.PAYMENT,
						NotificationTypes.PAYMENT,
						NotificationTypes.PRE_REGISTER,
						NotificationTypes.QUEUE_ENTRIES
					);

				// 웹소켓 4번 전송 확인
				verify(messagingTemplate, times(4)).convertAndSend(
					eq("/user/" + testUser.getId() + "/notifications"),
					any(NotificationResponseDto.class)
				);
			});
		}
	}

}
