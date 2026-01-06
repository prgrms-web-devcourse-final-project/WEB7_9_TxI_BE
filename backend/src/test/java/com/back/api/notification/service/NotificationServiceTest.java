package com.back.api.notification.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.api.notification.dto.NotificationResponseDto;
import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.enums.DomainName;
import com.back.domain.notification.enums.NotificationVar;
import com.back.domain.notification.repository.NotificationRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserActiveStatus;
import com.back.domain.user.entity.UserRole;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService 단위 테스트")
class NotificationServiceTest {

	@Mock
	private NotificationRepository notificationRepository;

	@InjectMocks
	private NotificationService notificationService;

	private User testUser;
	private List<Notification> testNotifications;
	private static final Long USER_ID = 1L;
	private static final Long OTHER_USER_ID = 2L;
	private static final Long NOTIFICATION_ID = 100L;

	@BeforeEach
	void setUp() {
		// 테스트 사용자 생성
		testUser = User.builder()
			.email("test@test.com")
			.nickname("tester")
			.password("encoded_password")
			.fullName("Test User")
			.role(UserRole.NORMAL)
			.activeStatus(UserActiveStatus.ACTIVE)
			.build();

		// Reflection을 사용하여 User ID 설정 (테스트용)
		setId(testUser, USER_ID);

		// 테스트 알림 데이터 생성 (안읽은 3개, 읽은 2개)
		testNotifications = createTestNotifications();
	}

	/**
	 * 테스트용 알림 데이터 생성
	 */
	private List<Notification> createTestNotifications() {
		List<Notification> notifications = new ArrayList<>();

		// 안읽은 알림 3개
		for (int i = 1; i <= 3; i++) {
			notifications.add(createNotification(
				(long)i,
				"알림 제목 " + i,
				"알림 내용 " + i,
				false
			));
		}

		// 읽은 알림 2개
		for (int i = 4; i <= 5; i++) {
			Notification notification = createNotification(
				(long)i,
				"알림 제목 " + i,
				"알림 내용 " + i,
				false
			);
			notification.markAsRead();
			notifications.add(notification);
		}

		return notifications;
	}

	/**
	 * 알림 생성 헬퍼 메서드
	 */
	private Notification createNotification(Long id, String title, String content, boolean isRead) {
		Notification notification = Notification.builder()
			.user(testUser)
			.type(NotificationVar.PAYMENT_SUCCESS)
			.domainName(DomainName.ORDERS)
			.title(title)
			.content(content)
			.isRead(isRead)
			.build();

		// Reflection을 사용하여 ID 설정 (테스트용)
		setId(notification, id);

		return notification;
	}

	/**
	 * Reflection을 사용하여 엔티티 ID 설정 (테스트용)
	 */
	private void setId(Object entity, Long id) {
		try {
			var idField = entity.getClass().getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(entity, id);
		} catch (Exception e) {
			throw new RuntimeException("ID 설정 실패: " + entity.getClass().getSimpleName(), e);
		}
	}

	@Nested
	@DisplayName("getNotifications 테스트")
	class GetNotificationsTest {

		@Test
		@DisplayName("알림 목록 조회 성공 - 5개 반환")
		void getNotifications_Success() {
			// given
			given(notificationRepository.findTop20ByUserIdOrderByCreateAtDesc(USER_ID))
				.willReturn(testNotifications);

			// when
			List<NotificationResponseDto> result = notificationService.getNotifications(USER_ID);

			// then
			assertThat(result).hasSize(5);
			assertThat(result).extracting("title")
				.containsExactly("알림 제목 1", "알림 제목 2", "알림 제목 3", "알림 제목 4", "알림 제목 5");
			verify(notificationRepository, times(1))
				.findTop20ByUserIdOrderByCreateAtDesc(USER_ID);
		}

		@Test
		@DisplayName("알림이 없는 경우 - 빈 리스트 반환")
		void getNotifications_EmptyList() {
			// given
			given(notificationRepository.findTop20ByUserIdOrderByCreateAtDesc(USER_ID))
				.willReturn(List.of());

			// when
			List<NotificationResponseDto> result = notificationService.getNotifications(USER_ID);

			// then
			assertThat(result).isEmpty();
			verify(notificationRepository, times(1))
				.findTop20ByUserIdOrderByCreateAtDesc(USER_ID);
		}

		@Test
		@DisplayName("알림 내용 검증 - DTO 변환 정확성")
		void getNotifications_DtoMapping() {
			// given
			Notification notification = testNotifications.get(0);
			given(notificationRepository.findTop20ByUserIdOrderByCreateAtDesc(USER_ID))
				.willReturn(List.of(notification));

			// when
			List<NotificationResponseDto> result = notificationService.getNotifications(USER_ID);

			// then
			assertThat(result).hasSize(1);
			NotificationResponseDto dto = result.get(0);
			assertThat(dto.id()).isEqualTo(notification.getId());
			assertThat(dto.title()).isEqualTo(notification.getTitle());
			assertThat(dto.content()).isEqualTo(notification.getContent());
			assertThat(dto.type()).isEqualTo(notification.getType().getFrontType().name());
			assertThat(dto.isRead()).isEqualTo(notification.isRead());
		}
	}

	@Nested
	@DisplayName("getUnreadCount 테스트")
	class GetUnreadCountTest {

		@Test
		@DisplayName("안읽은 알림 개수 조회 성공 - 3개")
		void getUnreadCount_Success() {
			// given
			given(notificationRepository.countByUserIdAndIsReadFalse(USER_ID))
				.willReturn(3L);

			// when
			long result = notificationService.getUnreadCount(USER_ID);

			// then
			assertThat(result).isEqualTo(3L);
			verify(notificationRepository, times(1))
				.countByUserIdAndIsReadFalse(USER_ID);
		}

		@Test
		@DisplayName("모든 알림을 읽은 경우 - 0 반환")
		void getUnreadCount_AllRead() {
			// given
			given(notificationRepository.countByUserIdAndIsReadFalse(USER_ID))
				.willReturn(0L);

			// when
			long result = notificationService.getUnreadCount(USER_ID);

			// then
			assertThat(result).isZero();
			verify(notificationRepository, times(1))
				.countByUserIdAndIsReadFalse(USER_ID);
		}

		@Test
		@DisplayName("알림이 없는 경우 - 0 반환")
		void getUnreadCount_NoNotifications() {
			// given
			given(notificationRepository.countByUserIdAndIsReadFalse(USER_ID))
				.willReturn(0L);

			// when
			long result = notificationService.getUnreadCount(USER_ID);

			// then
			assertThat(result).isZero();
			verify(notificationRepository, times(1))
				.countByUserIdAndIsReadFalse(USER_ID);
		}
	}

	@Nested
	@DisplayName("markAsRead 테스트")
	class MarkAsReadTest {

		@Test
		@DisplayName("알림 읽음 처리 성공")
		void markAsRead_Success() {
			// given
			Notification unreadNotification = testNotifications.get(0); // 안읽은 알림
			given(notificationRepository.findByIdAndUserId(NOTIFICATION_ID, USER_ID))
				.willReturn(Optional.of(unreadNotification));

			// when
			notificationService.markAsRead(NOTIFICATION_ID, USER_ID);

			// then
			assertThat(unreadNotification.isRead()).isTrue();
			assertThat(unreadNotification.getReadAt()).isNotNull();
			verify(notificationRepository, times(1))
				.findByIdAndUserId(NOTIFICATION_ID, USER_ID);
		}

		@Test
		@DisplayName("이미 읽은 알림 재처리 - 멱등성")
		void markAsRead_AlreadyRead() {
			// given
			Notification readNotification = testNotifications.get(3); // 이미 읽은 알림
			assertThat(readNotification.isRead()).isTrue();

			given(notificationRepository.findByIdAndUserId(NOTIFICATION_ID, USER_ID))
				.willReturn(Optional.of(readNotification));

			// when
			notificationService.markAsRead(NOTIFICATION_ID, USER_ID);

			// then
			assertThat(readNotification.isRead()).isTrue();
			verify(notificationRepository, times(1))
				.findByIdAndUserId(NOTIFICATION_ID, USER_ID);
		}

		@Test
		@DisplayName("존재하지 않는 알림 - 예외 발생")
		void markAsRead_NotFound() {
			// given
			given(notificationRepository.findByIdAndUserId(NOTIFICATION_ID, USER_ID))
				.willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> notificationService.markAsRead(NOTIFICATION_ID, USER_ID))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("알림을 찾을 수 없습니다");

			verify(notificationRepository, times(1))
				.findByIdAndUserId(NOTIFICATION_ID, USER_ID);
		}

		@Test
		@DisplayName("다른 사용자의 알림 접근 - 예외 발생")
		void markAsRead_OtherUserNotification() {
			// given
			given(notificationRepository.findByIdAndUserId(NOTIFICATION_ID, OTHER_USER_ID))
				.willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> notificationService.markAsRead(NOTIFICATION_ID, OTHER_USER_ID))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("알림을 찾을 수 없습니다");

			verify(notificationRepository, times(1))
				.findByIdAndUserId(NOTIFICATION_ID, OTHER_USER_ID);
		}
	}

	@Nested
	@DisplayName("markAllAsRead 테스트")
	class MarkAllAsReadTest {

		@Test
		@DisplayName("모든 알림 읽음 처리 성공 - 3개")
		void markAllAsRead_Success() {
			// given
			List<Notification> unreadNotifications = testNotifications.subList(0, 3); // 안읽은 3개
			given(notificationRepository.findByUserIdAndIsReadFalse(USER_ID))
				.willReturn(unreadNotifications);

			// when
			notificationService.markAllAsRead(USER_ID);

			// then
			assertThat(unreadNotifications)
				.allMatch(Notification::isRead)
				.allMatch(n -> n.getReadAt() != null);

			verify(notificationRepository, times(1))
				.findByUserIdAndIsReadFalse(USER_ID);
		}

		@Test
		@DisplayName("안읽은 알림이 없는 경우 - 정상 처리")
		void markAllAsRead_NoUnreadNotifications() {
			// given
			given(notificationRepository.findByUserIdAndIsReadFalse(USER_ID))
				.willReturn(List.of());

			// when & then
			assertThatCode(() -> notificationService.markAllAsRead(USER_ID))
				.doesNotThrowAnyException();

			verify(notificationRepository, times(1))
				.findByUserIdAndIsReadFalse(USER_ID);
		}

		@Test
		@DisplayName("일부만 안읽은 경우 - 안읽은 것만 처리")
		void markAllAsRead_PartialUnread() {
			// given
			List<Notification> unreadNotifications = testNotifications.subList(0, 3); // 안읽은 3개
			List<Notification> readNotifications = testNotifications.subList(3, 5); // 읽은 2개

			// 읽은 알림의 초기 readAt 저장
			List<LocalDateTime> originalReadAts = readNotifications.stream()
				.map(Notification::getReadAt)
				.toList();

			given(notificationRepository.findByUserIdAndIsReadFalse(USER_ID))
				.willReturn(unreadNotifications);

			// when
			notificationService.markAllAsRead(USER_ID);

			// then
			// 안읽은 알림만 읽음 처리됨
			assertThat(unreadNotifications).allMatch(Notification::isRead);

			// 이미 읽은 알림은 영향 없음 (readAt이 변경 안 됨)
			for (int i = 0; i < readNotifications.size(); i++) {
				assertThat(readNotifications.get(i).getReadAt())
					.isEqualTo(originalReadAts.get(i));
			}

			verify(notificationRepository, times(1))
				.findByUserIdAndIsReadFalse(USER_ID);
		}
	}
}
