package com.back.api.queue.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.back.api.queue.dto.response.CompletedQueueResponse;
import com.back.api.queue.dto.response.EnteredQueueResponse;
import com.back.api.queue.dto.response.ExpiredQueueResponse;
import com.back.api.queue.dto.response.MoveToBackResponse;
import com.back.api.ticket.service.TicketService;
import com.back.config.TestRedisConfig;
import com.back.domain.event.entity.Event;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.queue.entity.QueueEntry;
import com.back.domain.queue.entity.QueueEntryStatus;
import com.back.domain.queue.repository.QueueEntryRedisRepository;
import com.back.domain.queue.repository.QueueEntryRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.global.error.code.QueueEntryErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.event.EventPublisher;
import com.back.global.properties.QueueSchedulerProperties;
import com.back.support.factory.EventFactory;
import com.back.support.factory.UserFactory;

@ExtendWith(MockitoExtension.class)
@Import(TestRedisConfig.class)
@DisplayName("QueueEntryProcessService 단위 테스트")
class QueueEntryProcessServiceTest {

	private QueueEntryProcessService queueEntryProcessService;

	@Mock
	private QueueEntryRepository queueEntryRepository;

	@Mock
	private QueueEntryRedisRepository queueEntryRedisRepository;

	@Mock
	private EventPublisher eventPublisher;

	@Mock
	private QueueEntryReadService queueEntryReadService;

	@Mock
	private EventRepository eventRepository;

	@Mock
	private TicketService ticketService;

	private QueueSchedulerProperties queueSchedulerProperties;

	private Event testEvent;
	private User testUser;
	private QueueEntry testQueueEntry;
	private Long eventId;
	private Long userId;
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		eventId = 1L;
		userId = 100L;
		passwordEncoder = new BCryptPasswordEncoder();

		queueSchedulerProperties = new QueueSchedulerProperties();
		QueueSchedulerProperties.Entry entry = new QueueSchedulerProperties.Entry();
		entry.setBatchSize(10);
		entry.setMaxEnteredLimit(100);
		queueSchedulerProperties.setEntry(entry);

		queueEntryProcessService = new QueueEntryProcessService(
			queueEntryRepository,
			queueEntryRedisRepository,
			eventPublisher,
			queueSchedulerProperties,
			queueEntryReadService,
			eventRepository,
			ticketService
		);

		testEvent = EventFactory.fakeEvent("TestEvent");
		testUser = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();

		ReflectionTestUtils.setField(testEvent, "id", eventId);
		ReflectionTestUtils.setField(testUser, "id", userId);

		testQueueEntry = new QueueEntry(testUser, testEvent, 5);
	}

	@Nested
	@DisplayName("processEntry 테스트")
	class ProcessEntryTest {

		@Test
		@DisplayName("WAITING 상태의 대기열 입장 처리")
		void processEntry_WaitingStatus_Success() {
			// given
			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));
			given(queueEntryRepository.save(any(QueueEntry.class)))
				.willReturn(testQueueEntry);

			// when
			queueEntryProcessService.processEntry(eventId, userId);

			// then
			assertThat(testQueueEntry.getQueueEntryStatus()).isEqualTo(QueueEntryStatus.ENTERED);
			assertThat(testQueueEntry.getEnteredAt()).isNotNull();
			assertThat(testQueueEntry.getExpiredAt()).isNotNull();

			then(queueEntryRepository).should().save(testQueueEntry);
			then(queueEntryRedisRepository).should().moveToEnteredQueue(eventId, userId);
			then(queueEntryRedisRepository).should().incrementEnteredCount(eventId);
			then(eventPublisher).should().publishEvent(any(EnteredQueueResponse.class));
		}

		@Test
		@DisplayName("존재하지 않는 대기열 입장 처리 시 예외 발생")
		void processEntry_NotFound_ThrowsException() {
			// given
			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> queueEntryProcessService.processEntry(eventId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", QueueEntryErrorCode.NOT_FOUND_QUEUE_ENTRY);

			then(queueEntryRepository).should(never()).save(any());
			then(eventPublisher).should(never()).publishEvent(any());
		}

		@Test
		@DisplayName("이미 입장한 대기열 재입장 시 예외 발생")
		void processEntry_AlreadyEntered_ThrowsException() {
			// given
			testQueueEntry.enterQueue();

			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));

			// when & then
			assertThatThrownBy(() -> queueEntryProcessService.processEntry(eventId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", QueueEntryErrorCode.ALREADY_ENTERED);

			then(queueEntryRepository).should(never()).save(any());
		}

		@Test
		@DisplayName("만료된 대기열 입장 시 예외 발생")
		void processEntry_Expired_ThrowsException() {
			// given
			testQueueEntry.enterQueue();
			testQueueEntry.expire();

			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));

			// when & then
			assertThatThrownBy(() -> queueEntryProcessService.processEntry(eventId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", QueueEntryErrorCode.ALREADY_EXPIRED);

			then(queueEntryRepository).should(never()).save(any());
		}

		@Test
		@DisplayName("결제 완료된 대기열 입장 시 예외 발생")
		void processEntry_Completed_ThrowsException() {
			// given
			testQueueEntry.enterQueue();
			testQueueEntry.completePayment();

			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));

			// when & then
			assertThatThrownBy(() -> queueEntryProcessService.processEntry(eventId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", QueueEntryErrorCode.ALREADY_COMPLETED);

			then(queueEntryRepository).should(never()).save(any());
		}

		@Test
		@DisplayName("Redis 업데이트 실패해도 DB는 정상 저장된다")
		void processEntry_RedisFailure_DbSaveSuccess() {
			// given
			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));
			given(queueEntryRepository.save(any(QueueEntry.class)))
				.willReturn(testQueueEntry);
			willThrow(new RuntimeException("Redis error"))
				.given(queueEntryRedisRepository).moveToEnteredQueue(eventId, userId);

			// when
			queueEntryProcessService.processEntry(eventId, userId);

			// then
			assertThat(testQueueEntry.getQueueEntryStatus()).isEqualTo(QueueEntryStatus.ENTERED);
			then(queueEntryRepository).should().save(testQueueEntry);
			then(eventPublisher).should().publishEvent(any(EnteredQueueResponse.class));
		}
	}

	@Nested
	@DisplayName("processBatchEntry 테스트")
	class ProcessBatchEntryTest {

		@Test
		@DisplayName("여러 사용자 일괄 입장 처리")
		void processBatchEntry_Success() {
			// given
			Long userId1 = 100L;
			Long userId2 = 101L;
			Long userId3 = 102L;
			List<Long> userIds = List.of(userId1, userId2, userId3);

			User user1 = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
			User user2 = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
			User user3 = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
			ReflectionTestUtils.setField(user1, "id", userId1);
			ReflectionTestUtils.setField(user2, "id", userId2);
			ReflectionTestUtils.setField(user3, "id", userId3);

			QueueEntry entry1 = new QueueEntry(user1, testEvent, 1);
			QueueEntry entry2 = new QueueEntry(user2, testEvent, 2);
			QueueEntry entry3 = new QueueEntry(user3, testEvent, 3);

			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId1))
				.willReturn(Optional.of(entry1));
			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId2))
				.willReturn(Optional.of(entry2));
			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId3))
				.willReturn(Optional.of(entry3));
			given(queueEntryRepository.save(any(QueueEntry.class)))
				.willAnswer(invocation -> invocation.getArgument(0));

			// when
			queueEntryProcessService.processBatchEntry(eventId, userIds);

			// then
			then(queueEntryRepository).should(times(3)).save(any(QueueEntry.class));
			then(eventPublisher).should(times(3)).publishEvent(any(EnteredQueueResponse.class));
		}

		@Test
		@DisplayName("일부 사용자 입장 실패 시 나머지는 계속 처리")
		void processBatchEntry_PartialFailure_ContinuesProcessing() {
			// given
			Long userId1 = 100L;
			Long userId2 = 101L;
			Long userId3 = 102L;
			List<Long> userIds = List.of(userId1, userId2, userId3);

			User user1 = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
			User user3 = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
			ReflectionTestUtils.setField(user1, "id", userId1);
			ReflectionTestUtils.setField(user3, "id", userId3);

			QueueEntry entry1 = new QueueEntry(user1, testEvent, 1);
			QueueEntry entry3 = new QueueEntry(user3, testEvent, 3);

			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId1))
				.willReturn(Optional.of(entry1));
			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId2))
				.willReturn(Optional.empty()); // userId2는 실패
			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId3))
				.willReturn(Optional.of(entry3));
			given(queueEntryRepository.save(any(QueueEntry.class)))
				.willAnswer(invocation -> invocation.getArgument(0));

			// when
			queueEntryProcessService.processBatchEntry(eventId, userIds);

			// then
			then(queueEntryRepository).should(times(2)).save(any(QueueEntry.class));
			then(eventPublisher).should(times(2)).publishEvent(any(EnteredQueueResponse.class));
		}
	}

	@Nested
	@DisplayName("processEventQueueEntries 테스트")
	class ProcessEventQueueEntriesTest {

		@Test
		@DisplayName("대기 인원이 없을 때의 처리")
		void processEventQueueEntries_NoWaiting_DoesNothing() {
			// given
			given(queueEntryRedisRepository.getTotalWaitingCount(eventId))
				.willReturn(0L);

			// when
			queueEntryProcessService.processEventQueueEntries(testEvent);

			// then
			then(queueEntryRedisRepository).should().getTotalWaitingCount(eventId);
			then(queueEntryRedisRepository).should(never()).getTotalEnteredCount(any());
		}

		@Test
		@DisplayName("최대 수용 인원 도달 시 입장 멈춤")
		void processEventQueueEntries_MaxCapacity_DoesNotProcess() {
			// given
			queueSchedulerProperties.getEntry().setMaxEnteredLimit(100);

			given(queueEntryRedisRepository.getTotalWaitingCount(eventId))
				.willReturn(10L);
			given(queueEntryRedisRepository.getTotalEnteredCount(eventId))
				.willReturn(100L);

			// when
			queueEntryProcessService.processEventQueueEntries(testEvent);

			// then
			then(queueEntryRedisRepository).should(never()).getTopWaitingUsers(any(), anyInt());
		}

		@Test
		@DisplayName("배치 사이즈만큼 사용자 입장 처리")
		void processEventQueueEntries_ProcessesBatchSize() {
			// given
			queueSchedulerProperties.getEntry().setBatchSize(3);
			queueSchedulerProperties.getEntry().setMaxEnteredLimit(100);

			given(queueEntryRedisRepository.getTotalWaitingCount(eventId))
				.willReturn(10L);
			given(queueEntryRedisRepository.getTotalEnteredCount(eventId))
				.willReturn(50L);

			Set<Object> topUsers = Set.of("100", "101", "102");
			given(queueEntryRedisRepository.getTopWaitingUsers(eventId, 3))
				.willReturn(topUsers);

			// Mock QueueEntry 설정
			for (Object userIdObj : topUsers) {
				Long uid = Long.parseLong(userIdObj.toString());
				User user = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
				ReflectionTestUtils.setField(user, "id", uid);
				QueueEntry entry = new QueueEntry(user, testEvent, 1);

				given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, uid))
					.willReturn(Optional.of(entry));
			}
			given(queueEntryRepository.save(any(QueueEntry.class)))
				.willAnswer(invocation -> invocation.getArgument(0));

			// when
			queueEntryProcessService.processEventQueueEntries(testEvent);

			// then
			then(queueEntryRedisRepository).should().getTopWaitingUsers(eventId, 3);
			then(queueEntryRepository).should(times(3)).save(any(QueueEntry.class));
		}

		@Test
		@DisplayName("빈 자리보다 대기 인원이 적으면 대기 인원만큼만 입장 처리")
		void processEventQueueEntries_LessWaitingThanAvailable() {
			// given
			queueSchedulerProperties.getEntry().setBatchSize(10);
			queueSchedulerProperties.getEntry().setMaxEnteredLimit(100);

			int waitingCount = 3; // 대기 인원이 적음

			given(queueEntryRedisRepository.getTotalWaitingCount(eventId))
				.willReturn((long)waitingCount);
			given(queueEntryRedisRepository.getTotalEnteredCount(eventId))
				.willReturn(50L);

			Set<Object> topUsers = Set.of("100", "101", "102");
			given(queueEntryRedisRepository.getTopWaitingUsers(eventId, waitingCount))
				.willReturn(topUsers);

			for (Object userIdObj : topUsers) {
				Long uid = Long.parseLong(userIdObj.toString());
				User user = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
				ReflectionTestUtils.setField(user, "id", uid);
				QueueEntry entry = new QueueEntry(user, testEvent, 1);

				given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, uid))
					.willReturn(Optional.of(entry));
			}
			given(queueEntryRepository.save(any(QueueEntry.class)))
				.willAnswer(invocation -> invocation.getArgument(0));

			// when
			queueEntryProcessService.processEventQueueEntries(testEvent);

			// then
			then(queueEntryRedisRepository).should().getTopWaitingUsers(eventId, waitingCount);
			then(queueEntryRepository).should(times(waitingCount)).save(any(QueueEntry.class));
		}
	}

	@Nested
	@DisplayName("canEnterEntry 테스트")
	class CanEnterEntryTest {

		@Test
		@DisplayName("WAITING 상태 입장 처리")
		void canEnterEntry_WaitingStatus_ReturnsTrue() {
			// given
			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));

			// when
			boolean result = queueEntryProcessService.canEnterEntry(eventId, userId);

			// then
			assertThat(result).isTrue();
		}

		@Test
		@DisplayName("ENTERED 상태")
		void canEnterEntry_EnteredStatus_ReturnsFalse() {
			// given
			testQueueEntry.enterQueue();

			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));

			// when
			boolean result = queueEntryProcessService.canEnterEntry(eventId, userId);

			// then
			assertThat(result).isFalse();
		}

		@Test
		@DisplayName("대기열 없으면 입장 불가능")
		void canEnterEntry_NotFound_ReturnsFalse() {
			// given
			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.empty());

			// when
			boolean result = queueEntryProcessService.canEnterEntry(eventId, userId);

			// then
			assertThat(result).isFalse();
		}
	}

	@Nested
	@DisplayName("expireEntry 테스트")
	class ExpireEntryTest {

		@Test
		@DisplayName("ENTERED 상태의 대기열 만료 처리")
		void expireEntry_EnteredStatus_Success() {
			// given
			testQueueEntry.enterQueue();

			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));
			given(queueEntryRepository.save(any(QueueEntry.class)))
				.willReturn(testQueueEntry);

			// when
			queueEntryProcessService.expireEntry(eventId, userId);

			// then
			assertThat(testQueueEntry.getQueueEntryStatus()).isEqualTo(QueueEntryStatus.EXPIRED);

			then(queueEntryRepository).should().save(testQueueEntry);
			then(queueEntryRedisRepository).should().removeFromEnteredQueue(eventId, userId);
			then(eventPublisher).should().publishEvent(any(ExpiredQueueResponse.class));
		}

		@Test
		@DisplayName("이미 만료된 대기열 처리 X")
		void expireEntry_AlreadyExpired_DoesNothing() {
			// given
			testQueueEntry.enterQueue();
			testQueueEntry.expire();

			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));

			// when
			queueEntryProcessService.expireEntry(eventId, userId);

			// then
			then(queueEntryRepository).should(never()).save(any());
			then(eventPublisher).should(never()).publishEvent(any());
		}

		@Test
		@DisplayName("WAITING 상태의 대기열 만료 처리 X")
		void expireEntry_WaitingStatus_DoesNothing() {
			// given - testQueueEntry는 WAITING 상태

			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));

			// when
			queueEntryProcessService.expireEntry(eventId, userId);

			// then
			then(queueEntryRepository).should(never()).save(any());
			then(eventPublisher).should(never()).publishEvent(any());
		}

		@Test
		@DisplayName("존재하지 않는 대기열 만료 시 예외 발생")
		void expireEntry_NotFound_ThrowsException() {
			// given
			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> queueEntryProcessService.expireEntry(eventId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", QueueEntryErrorCode.NOT_FOUND_QUEUE_ENTRY);

			then(queueEntryRepository).should(never()).save(any());
		}
	}

	@Nested
	@DisplayName("expireBatchEntries 테스트")
	class ExpireBatchEntriesTest {

		@Test
		@DisplayName("여러 대기열 일괄 만료 처리")
		void expireBatchEntries_Success() {
			// given
			User user1 = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
			User user2 = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
			ReflectionTestUtils.setField(user1, "id", 100L);
			ReflectionTestUtils.setField(user2, "id", 101L);

			QueueEntry entry1 = new QueueEntry(user1, testEvent, 1);
			QueueEntry entry2 = new QueueEntry(user2, testEvent, 2);
			entry1.enterQueue();
			entry2.enterQueue();

			List<QueueEntry> entries = List.of(entry1, entry2);

			given(queueEntryRepository.findByEvent_IdAndUser_Id(eq(eventId), any()))
				.willReturn(Optional.of(entry1), Optional.of(entry2));
			given(queueEntryRepository.save(any(QueueEntry.class)))
				.willAnswer(invocation -> invocation.getArgument(0));

			// when
			queueEntryProcessService.expireBatchEntries(entries);

			// then
			then(queueEntryRepository).should(times(2)).save(any(QueueEntry.class));
			then(eventPublisher).should(times(2)).publishEvent(any(ExpiredQueueResponse.class));
		}
	}

	@Nested
	@DisplayName("completePayment 테스트")
	class CompletePaymentTest {

		@Test
		@DisplayName("ENTERED 상태 대기열 결제 완료 처리")
		void completePayment_EnteredStatus_Success() {
			// given
			testQueueEntry.enterQueue();

			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));
			given(queueEntryRepository.save(any(QueueEntry.class)))
				.willReturn(testQueueEntry);

			// when
			queueEntryProcessService.completePayment(eventId, userId);

			// then
			assertThat(testQueueEntry.getQueueEntryStatus()).isEqualTo(QueueEntryStatus.COMPLETED);

			then(queueEntryRepository).should().save(testQueueEntry);
			then(queueEntryRedisRepository).should().removeFromEnteredQueue(eventId, userId);
			then(eventPublisher).should().publishEvent(any(CompletedQueueResponse.class));
		}

		@Test
		@DisplayName("이미 결제 완료된 대기열 재결제 시 예외 발생")
		void completePayment_AlreadyCompleted_ThrowsException() {
			// given
			testQueueEntry.enterQueue();
			testQueueEntry.completePayment();

			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));

			// when & then
			assertThatThrownBy(() -> queueEntryProcessService.completePayment(eventId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", QueueEntryErrorCode.ALREADY_COMPLETED);

			then(queueEntryRepository).should(never()).save(any());
		}

		@Test
		@DisplayName("만료된 대기열 결제 시 예외 발생")
		void completePayment_Expired_ThrowsException() {
			// given
			testQueueEntry.enterQueue();
			testQueueEntry.expire();

			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));

			// when & then
			assertThatThrownBy(() -> queueEntryProcessService.completePayment(eventId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", QueueEntryErrorCode.ALREADY_EXPIRED);

			then(queueEntryRepository).should(never()).save(any());
		}

		@Test
		@DisplayName("WAITING 상태의 대기열 결제 시 예외 발생")
		void completePayment_WaitingStatus_ThrowsException() {
			// given
			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));

			// when & then
			assertThatThrownBy(() -> queueEntryProcessService.completePayment(eventId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", QueueEntryErrorCode.NOT_ENTERED_STATUS);

			then(queueEntryRepository).should(never()).save(any());
		}

		@Test
		@DisplayName("존재하지 않는 대기열 결제 시 예외 발생")
		void completePayment_NotFound_ThrowsException() {
			// given
			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> queueEntryProcessService.completePayment(eventId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", QueueEntryErrorCode.NOT_FOUND_QUEUE_ENTRY);

			then(queueEntryRepository).should(never()).save(any());
		}
	}

	@Nested
	@DisplayName("대기열 상태 검증 테스트")
	class QueueEntryValidationTest {

		@Test
		@DisplayName("입장 처리 전 상태 검증")
		void validateEntry_StatusChecks_WorkCorrectly() {
			// WAITING → 성공
			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));
			given(queueEntryRepository.save(any(QueueEntry.class)))
				.willReturn(testQueueEntry);

			assertThatCode(() -> queueEntryProcessService.processEntry(eventId, userId))
				.doesNotThrowAnyException();

			// ENTERED → 실패
			testQueueEntry = new QueueEntry(testUser, testEvent, 5);
			testQueueEntry.enterQueue();

			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));

			assertThatThrownBy(() -> queueEntryProcessService.processEntry(eventId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", QueueEntryErrorCode.ALREADY_ENTERED);
		}

		@Test
		@DisplayName("결제 완료 전 상태 검증")
		void validatePaymentCompletion_StatusChecks_WorkCorrectly() {
			// ENTERED → 성공
			testQueueEntry.enterQueue();

			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));
			given(queueEntryRepository.save(any(QueueEntry.class)))
				.willReturn(testQueueEntry);

			assertThatCode(() -> queueEntryProcessService.completePayment(eventId, userId))
				.doesNotThrowAnyException();

			// WAITING → 실패
			testQueueEntry = new QueueEntry(testUser, testEvent, 5);

			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));

			assertThatThrownBy(() -> queueEntryProcessService.completePayment(eventId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", QueueEntryErrorCode.NOT_ENTERED_STATUS);
		}
	}

	@Nested
	@DisplayName("이벤트 발행 검증 테스트")
	class EventPublishTest {

		@Test
		@DisplayName("입장 처리 시 EnteredQueueResponse 이벤트 발행")
		void processEntry_PublishesEnteredEvent() {
			// given
			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));
			given(queueEntryRepository.save(any(QueueEntry.class)))
				.willReturn(testQueueEntry);

			// when
			queueEntryProcessService.processEntry(eventId, userId);

			// then
			then(eventPublisher).should().publishEvent(any(EnteredQueueResponse.class));
		}

		@Test
		@DisplayName("만료 처리 시 ExpiredQueueResponse 이벤트 발행")
		void expireEntry_PublishesExpiredEvent() {
			// given
			testQueueEntry.enterQueue();

			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));
			given(queueEntryRepository.save(any(QueueEntry.class)))
				.willReturn(testQueueEntry);

			// when
			queueEntryProcessService.expireEntry(eventId, userId);

			// then
			then(eventPublisher).should().publishEvent(any(ExpiredQueueResponse.class));
		}

		@Test
		@DisplayName("결제 완료 시 CompletedQueueResponse 이벤트 발행")
		void completePayment_PublishesCompletedEvent() {
			// given
			testQueueEntry.enterQueue();

			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));
			given(queueEntryRepository.save(any(QueueEntry.class)))
				.willReturn(testQueueEntry);

			// when
			queueEntryProcessService.completePayment(eventId, userId);

			// then
			then(eventPublisher).should().publishEvent(any(CompletedQueueResponse.class));
		}

		@Test
		@DisplayName("입장 처리 실패 시 이벤트가 발행 X")
		void processEntry_Failure_DoesNotPublishEvent() {
			// given
			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> queueEntryProcessService.processEntry(eventId, userId))
				.isInstanceOf(ErrorException.class);

			then(eventPublisher).should(never()).publishEvent(any());
		}
	}

	@Nested
	@DisplayName("moveToBackQueue 테스트")
	class MoveToBackQueueTest {

		@Test
		@DisplayName("ENTERED 상태에서 맨 뒤로 이동 - 성공")
		void moveToBackQueue_Success() {

			//given
			testQueueEntry.enterQueue();

			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));
			given(queueEntryRepository.findMaxRankInQueue(eventId))
				.willReturn(Optional.of(10L));
			given(queueEntryRepository.countByEvent_IdAndQueueEntryStatus(eventId, QueueEntryStatus.WAITING))
				.willReturn(3L);
			given(queueEntryRepository.save(any(QueueEntry.class)))
				.willAnswer(invocation -> invocation.getArgument(0));

			// when
			MoveToBackResponse response = queueEntryProcessService.moveToBackQueue(eventId, userId);

			// then
			assertThat(response.userId()).isEqualTo(userId);
			assertThat(response.previousRank()).isEqualTo(5);
			assertThat(response.newRank()).isEqualTo(11);
			assertThat(response.totalWaitingUsers()).isEqualTo(3);
			assertThat(testQueueEntry.getQueueEntryStatus()).isEqualTo(QueueEntryStatus.WAITING);
			assertThat(testQueueEntry.getQueueRank()).isEqualTo(11);
			assertThat(testQueueEntry.getEnteredAt()).isNull();
			assertThat(testQueueEntry.getExpiredAt()).isNull();

			then(queueEntryRepository).should().save(testQueueEntry);
			then(queueEntryRedisRepository).should().removeFromEnteredQueue(eventId, userId);
			then(queueEntryRedisRepository).should().addToWaitingQueue(eventId, userId, 11);

		}

		@Test
		@DisplayName("여러 ENTERED 사용자 있을 때 - 전체 최대 rank 기준으로 맨 뒤 배정")
		void moveToBackQueue_WithMultipleEnteredUsers() {

			// given
			testQueueEntry.enterQueue();

			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));
			given(queueEntryRepository.findMaxRankInQueue(eventId))
				.willReturn(Optional.of(150L)); // ENTERED 사용자 중 최대 rank
			given(queueEntryRepository.countByEvent_IdAndQueueEntryStatus(eventId, QueueEntryStatus.WAITING))
				.willReturn(140L); // WAITING 사용자 수
			given(queueEntryRepository.save(any(QueueEntry.class)))
				.willAnswer(invocation -> invocation.getArgument(0));

			// when
			MoveToBackResponse response = queueEntryProcessService.moveToBackQueue(eventId, userId);

			// then
			assertThat(response.newRank()).isEqualTo(151);
			assertThat(response.totalWaitingUsers()).isEqualTo(140);
		}

		@Test
		@DisplayName("WAITING 상태에서 맨 뒤로 이동 시도 - 실패")
		void moveToBackQueue_NotEnteredStatus_Waiting() {

			//given
			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));

			// when & then
			assertThatThrownBy(() ->
				queueEntryProcessService.moveToBackQueue(eventId, userId)
			)
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", QueueEntryErrorCode.NOT_ENTERED_STATUS);

			then(queueEntryRepository).should(never()).save(any());
		}

		@Test
		@DisplayName("EXPIRED 상태에서 맨 뒤로 이동 시도 - 실패")
		void moveToBackQueue_NotEnteredStatus_Expired() {

			// given
			testQueueEntry.enterQueue();
			testQueueEntry.expire();

			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));

			// when & then
			assertThatThrownBy(() ->
				queueEntryProcessService.moveToBackQueue(eventId, userId)
			)
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", QueueEntryErrorCode.NOT_ENTERED_STATUS);

			then(queueEntryRepository).should(never()).save(any());
		}

		@Test
		@DisplayName("COMPLETED 상태에서 맨 뒤로 이동 시도 - 실패")
		void moveToBackQueue_NotEnteredStatus_Completed() {

			// given
			testQueueEntry.enterQueue();
			testQueueEntry.completePayment();

			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));

			// when & then
			assertThatThrownBy(() ->
				queueEntryProcessService.moveToBackQueue(eventId, userId)
			)
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", QueueEntryErrorCode.NOT_ENTERED_STATUS);

			then(queueEntryRepository).should(never()).save(any());
		}

	}

}
