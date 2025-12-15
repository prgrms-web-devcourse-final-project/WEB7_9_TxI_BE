package com.back.api.queue.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.back.api.event.service.EventService;
import com.back.config.TestRedisConfig;
import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventStatus;
import com.back.domain.queue.entity.QueueEntry;
import com.back.domain.queue.entity.QueueEntryStatus;
import com.back.domain.queue.repository.QueueEntryRedisRepository;
import com.back.domain.queue.repository.QueueEntryRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.domain.user.repository.UserRepository;
import com.back.global.error.code.QueueEntryErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.support.factory.EventFactory;
import com.back.support.factory.UserFactory;

@ExtendWith(MockitoExtension.class)
@Import(TestRedisConfig.class)
@DisplayName("QueueShuffleService 단위 테스트")
class QueueShuffleServiceTest {

	private QueueShuffleService queueShuffleService;

	@Mock
	private QueueEntryRepository queueEntryRepository;

	@Mock
	private QueueEntryRedisRepository queueEntryRedisRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private EventService eventService;

	private Event testEvent;
	private List<User> testUsers;
	private List<Long> testUserIds;
	private Long eventId;
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		eventId = 1L;
		passwordEncoder = new BCryptPasswordEncoder();

		queueShuffleService = new QueueShuffleService(
			queueEntryRepository,
			queueEntryRedisRepository,
			userRepository,
			eventService
		);

		testEvent = EventFactory.fakeEvent("Test Event");
		ReflectionTestUtils.setField(testEvent, "id", eventId);

		testUsers = new ArrayList<>();
		testUserIds = new ArrayList<>();
		for (int i = 1; i <= 10; i++) {
			User user = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
			Long userId = (long) (100 + i);
			ReflectionTestUtils.setField(user, "id", userId);
			testUsers.add(user);
			testUserIds.add(userId);
		}
	}

	@Nested
	@DisplayName("shuffleQueue 테스트")
	class ShuffleQueueTest {

		@Test
		@DisplayName("사전 등록된 사용자 목록으로 랜덤 대기열 생성 성공")
		void shuffleQueue_Success() {
			// given
			given(eventService.getEventEntity(eventId)).willReturn(testEvent);
			given(queueEntryRepository.countByEvent_Id(eventId)).willReturn(0L);
			given(userRepository.findAllById(testUserIds)).willReturn(testUsers);

			// when
			queueShuffleService.shuffleQueue(eventId, testUserIds);

			// then
			then(eventService).should().getEventEntity(eventId);
			then(queueEntryRepository).should().countByEvent_Id(eventId);
			then(userRepository).should().findAllById(testUserIds);
			then(queueEntryRepository).should().saveAll(anyList());
			then(queueEntryRedisRepository).should(times(testUserIds.size()))
				.addToWaitingQueue(eq(eventId), anyLong(), anyInt());
			assertThat(testEvent.getStatus()).isEqualTo(EventStatus.QUEUE_READY);
		}

		@Test
		@DisplayName("대기열 생성 시 순위가 1부터 순차적으로 배정됨")
		void shuffleQueue_RankAssignedSequentially() {
			// given
			given(eventService.getEventEntity(eventId)).willReturn(testEvent);
			given(queueEntryRepository.countByEvent_Id(eventId)).willReturn(0L);
			given(userRepository.findAllById(testUserIds)).willReturn(testUsers);

			ArgumentCaptor<List<QueueEntry>> captor = ArgumentCaptor.forClass(List.class);

			// when
			queueShuffleService.shuffleQueue(eventId, testUserIds);

			// then
			then(queueEntryRepository).should().saveAll(captor.capture());

			List<QueueEntry> savedEntries = captor.getValue();
			assertThat(savedEntries).hasSize(testUserIds.size());

			// 모든 QueueEntry가 WAITING 상태인지 확인
			assertThat(savedEntries)
				.allMatch(entry -> entry.getQueueEntryStatus() == QueueEntryStatus.WAITING);

			// 순위가 1부터 시작하는지 확인
			List<Integer> ranks = savedEntries.stream()
				.map(QueueEntry::getQueueRank)
				.sorted()
				.toList();

			assertThat(ranks).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
		}

		@Test
		@DisplayName("사전 등록 목록이 null이면 예외 발생")
		void shuffleQueue_NullList_ThrowsException() {
			// given
			given(eventService.getEventEntity(eventId)).willReturn(testEvent);

			// when & then
			assertThatThrownBy(() -> queueShuffleService.shuffleQueue(eventId, null))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", QueueEntryErrorCode.PRE_REGISTERED_USERS_EMPTY);

			then(queueEntryRepository).should(never()).saveAll(any());
		}

		@Test
		@DisplayName("사전 등록 목록이 비어있으면 예외 발생")
		void shuffleQueue_EmptyList_ThrowsException() {
			// given
			given(eventService.getEventEntity(eventId)).willReturn(testEvent);
			List<Long> emptyList = new ArrayList<>();

			// when & then
			assertThatThrownBy(() -> queueShuffleService.shuffleQueue(eventId, emptyList))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", QueueEntryErrorCode.PRE_REGISTERED_USERS_EMPTY);

			then(queueEntryRepository).should(never()).saveAll(any());
		}

		@Test
		@DisplayName("이미 대기열이 존재하면 예외 발생")
		void shuffleQueue_QueueAlreadyExists_ThrowsException() {
			// given
			given(eventService.getEventEntity(eventId)).willReturn(testEvent);
			given(queueEntryRepository.countByEvent_Id(eventId)).willReturn(5L);

			// when & then
			assertThatThrownBy(() -> queueShuffleService.shuffleQueue(eventId, testUserIds))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", QueueEntryErrorCode.QUEUE_ALREADY_EXISTS);

			then(userRepository).should(never()).findAllById(any());
			then(queueEntryRepository).should(never()).saveAll(any());
		}

		@Test
		@DisplayName("사용자 조회 개수와 입력 목록 개수가 다르면 예외 발생")
		void shuffleQueue_UserCountMismatch_ThrowsException() {
			// given
			given(eventService.getEventEntity(eventId)).willReturn(testEvent);
			given(queueEntryRepository.countByEvent_Id(eventId)).willReturn(0L);

			// 일부 사용자만 반환
			List<User> partialUsers = testUsers.subList(0, 5);
			given(userRepository.findAllById(testUserIds)).willReturn(partialUsers);

			// when & then
			assertThatThrownBy(() -> queueShuffleService.shuffleQueue(eventId, testUserIds))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", QueueEntryErrorCode.INVALID_PREREGISTER_LIST);

			then(queueEntryRepository).should(never()).saveAll(any());
		}

		@Test
		@DisplayName("Redis 저장 실패 시 예외 발생")
		void shuffleQueue_RedisFailure_ThrowsException() {
			// given
			given(eventService.getEventEntity(eventId)).willReturn(testEvent);
			given(queueEntryRepository.countByEvent_Id(eventId)).willReturn(0L);
			given(userRepository.findAllById(testUserIds)).willReturn(testUsers);

			willThrow(new RuntimeException("Redis connection error"))
				.given(queueEntryRedisRepository)
				.addToWaitingQueue(anyLong(), anyLong(), anyInt());

			// when & then
			assertThatThrownBy(() -> queueShuffleService.shuffleQueue(eventId, testUserIds))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", QueueEntryErrorCode.REDIS_CONNECTION_FAILED);

			then(queueEntryRepository).should(never()).saveAll(any());
		}
	}

	@Nested
	@DisplayName("Fisher-Yates Shuffle 알고리즘 검증")
	class ShuffleAlgorithmTest {

		@Test
		@DisplayName("셔플 후 모든 요소가 보존됨")
		void shuffle_PreservesAllElements() {
			// given
			given(eventService.getEventEntity(eventId)).willReturn(testEvent);
			given(queueEntryRepository.countByEvent_Id(eventId)).willReturn(0L);
			given(userRepository.findAllById(testUserIds)).willReturn(testUsers);

			ArgumentCaptor<List<QueueEntry>> captor = ArgumentCaptor.forClass(List.class);

			// when
			queueShuffleService.shuffleQueue(eventId, testUserIds);

			// then
			then(queueEntryRepository).should().saveAll(captor.capture());

			List<QueueEntry> savedEntries = captor.getValue();
			List<Long> savedUserIds = savedEntries.stream()
				.map(entry -> entry.getUser().getId())
				.sorted()
				.toList();

			List<Long> originalUserIds = new ArrayList<>(testUserIds);
			originalUserIds.sort(Long::compareTo);

			// 모든 사용자 ID가 그대로 존재하는지 확인
			assertThat(savedUserIds).containsExactlyElementsOf(originalUserIds);
		}

		@Test
		@DisplayName("셔플 후 중복된 순위가 없음")
		void shuffle_NoDuplicateRanks() {
			// given
			given(eventService.getEventEntity(eventId)).willReturn(testEvent);
			given(queueEntryRepository.countByEvent_Id(eventId)).willReturn(0L);
			given(userRepository.findAllById(testUserIds)).willReturn(testUsers);

			ArgumentCaptor<List<QueueEntry>> captor = ArgumentCaptor.forClass(List.class);

			// when
			queueShuffleService.shuffleQueue(eventId, testUserIds);

			// then
			then(queueEntryRepository).should().saveAll(captor.capture());

			List<QueueEntry> savedEntries = captor.getValue();
			List<Integer> ranks = savedEntries.stream()
				.map(QueueEntry::getQueueRank)
				.toList();

			// 중복 없이 모든 순위가 유니크한지 확인
			assertThat(ranks).doesNotHaveDuplicates();
			assertThat(ranks).hasSize(testUserIds.size());
		}

		@Test
		@DisplayName("여러 번 셔플해도 항상 동일한 개수의 엔트리 생성됨")
		void shuffle_ConsistentSize() {
			// given
			given(eventService.getEventEntity(eventId)).willReturn(testEvent);
			given(queueEntryRepository.countByEvent_Id(eventId)).willReturn(0L);
			given(userRepository.findAllById(testUserIds)).willReturn(testUsers);

			ArgumentCaptor<List<QueueEntry>> captor = ArgumentCaptor.forClass(List.class);

			for (int i = 0; i < 3; i++) {
				queueShuffleService.shuffleQueue(eventId, testUserIds);
			}

			// then
			then(queueEntryRepository).should(times(3)).saveAll(captor.capture());

			List<List<QueueEntry>> allSavedEntries = captor.getAllValues();
			for (List<QueueEntry> entries : allSavedEntries) {
				assertThat(entries).hasSize(testUserIds.size());
			}
		}
	}

	@Nested
	@DisplayName("대기열 생성 후 상태 검증")
	class QueueStateValidationTest {

		@Test
		@DisplayName("생성된 모든 QueueEntry의 상태가 WAITING임")
		void createdEntries_AllWaitingStatus() {
			// given
			given(eventService.getEventEntity(eventId)).willReturn(testEvent);
			given(queueEntryRepository.countByEvent_Id(eventId)).willReturn(0L);
			given(userRepository.findAllById(testUserIds)).willReturn(testUsers);

			ArgumentCaptor<List<QueueEntry>> captor = ArgumentCaptor.forClass(List.class);

			// when
			queueShuffleService.shuffleQueue(eventId, testUserIds);

			// then
			then(queueEntryRepository).should().saveAll(captor.capture());

			List<QueueEntry> savedEntries = captor.getValue();
			assertThat(savedEntries)
				.allMatch(entry -> entry.getQueueEntryStatus() == QueueEntryStatus.WAITING);
		}

		@Test
		@DisplayName("생성된 모든 QueueEntry가 동일한 Event를 참조함")
		void createdEntries_SameEventReference() {
			// given
			given(eventService.getEventEntity(eventId)).willReturn(testEvent);
			given(queueEntryRepository.countByEvent_Id(eventId)).willReturn(0L);
			given(userRepository.findAllById(testUserIds)).willReturn(testUsers);

			ArgumentCaptor<List<QueueEntry>> captor = ArgumentCaptor.forClass(List.class);

			// when
			queueShuffleService.shuffleQueue(eventId, testUserIds);

			// then
			then(queueEntryRepository).should().saveAll(captor.capture());

			List<QueueEntry> savedEntries = captor.getValue();
			assertThat(savedEntries)
				.allMatch(entry -> entry.getEvent() == testEvent);
		}

		@Test
		@DisplayName("대기열 생성 후 Event 상태가 QUEUE_READY로 변경됨")
		void afterShuffle_EventStatusChangedToQueueReady() {
			// given
			given(eventService.getEventEntity(eventId)).willReturn(testEvent);
			given(queueEntryRepository.countByEvent_Id(eventId)).willReturn(0L);
			given(userRepository.findAllById(testUserIds)).willReturn(testUsers);

			EventStatus initialStatus = testEvent.getStatus();

			// when
			queueShuffleService.shuffleQueue(eventId, testUserIds);

			// then
			assertThat(testEvent.getStatus()).isEqualTo(EventStatus.QUEUE_READY);
			assertThat(testEvent.getStatus()).isNotEqualTo(initialStatus);
		}
	}

	@Nested
	@DisplayName("Redis 저장 검증")
	class RedisStorageValidationTest {

		@Test
		@DisplayName("모든 사용자가 Redis waiting queue에 저장됨")
		void allUsers_SavedToRedis() {
			// given
			given(eventService.getEventEntity(eventId)).willReturn(testEvent);
			given(queueEntryRepository.countByEvent_Id(eventId)).willReturn(0L);
			given(userRepository.findAllById(testUserIds)).willReturn(testUsers);

			// when
			queueShuffleService.shuffleQueue(eventId, testUserIds);

			// then
			then(queueEntryRedisRepository).should(times(testUserIds.size()))
				.addToWaitingQueue(eq(eventId), anyLong(), anyInt());
		}

		@Test
		@DisplayName("Redis에 순위와 함께 저장됨")
		void savedToRedis_WithRank() {
			// given
			given(eventService.getEventEntity(eventId)).willReturn(testEvent);
			given(queueEntryRepository.countByEvent_Id(eventId)).willReturn(0L);
			given(userRepository.findAllById(testUserIds)).willReturn(testUsers);

			ArgumentCaptor<Integer> rankCaptor = ArgumentCaptor.forClass(Integer.class);

			// when
			queueShuffleService.shuffleQueue(eventId, testUserIds);

			// then
			then(queueEntryRedisRepository).should(times(testUserIds.size()))
				.addToWaitingQueue(eq(eventId), anyLong(), rankCaptor.capture());

			List<Integer> capturedRanks = rankCaptor.getAllValues();

			// 순위가 1부터 시작하는지 확인
			assertThat(capturedRanks).containsAll(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
			assertThat(capturedRanks).doesNotHaveDuplicates();
		}
	}
}
