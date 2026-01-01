package com.back.api.queue.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.back.api.queue.dto.response.CompletedQueueResponse;
import com.back.api.queue.dto.response.EnteredQueueResponse;
import com.back.api.queue.dto.response.ExpiredQueueResponse;
import com.back.api.queue.dto.response.QueueEntryStatusResponse;
import com.back.api.queue.dto.response.WaitingQueueResponse;
import com.back.config.TestRedisConfig;
import com.back.domain.event.entity.Event;
import com.back.domain.queue.entity.QueueEntry;
import com.back.domain.queue.entity.QueueEntryStatus;
import com.back.domain.queue.repository.QueueEntryRedisRepository;
import com.back.domain.queue.repository.QueueEntryRepository;
import com.back.domain.store.entity.Store;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.global.error.code.QueueEntryErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.support.factory.EventFactory;
import com.back.support.factory.StoreFactory;
import com.back.support.factory.UserFactory;

@ExtendWith(MockitoExtension.class)
@Import(TestRedisConfig.class)
@DisplayName("QueueEntryReadService 단위 테스트")
class QueueEntryReadServiceTest {

	@InjectMocks
	private QueueEntryReadService queueEntryReadService;

	@Mock
	private QueueEntryRepository queueEntryRepository;

	@Mock
	private QueueEntryRedisRepository queueEntryRedisRepository;

	private Event testEvent;
	private User testUser;
	private QueueEntry testQueueEntry;
	private Long eventId;
	private Long userId;
	private PasswordEncoder passwordEncoder;

	private final Store store = StoreFactory.fakeStore(1L);

	@BeforeEach
	void setUp() {
		eventId = 1L;
		userId = 100L;
		passwordEncoder = new BCryptPasswordEncoder();

		testEvent = EventFactory.fakeEvent(store, "Test Event");
		testUser = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null).user();

		ReflectionTestUtils.setField(testEvent, "id", eventId);
		ReflectionTestUtils.setField(testUser, "id", userId);

		testQueueEntry = new QueueEntry(testUser, testEvent, 5);
	}

	@Nested
	@DisplayName("getMyQueueStatus 테스트")
	class GetMyQueueStatusTest {

		@Test
		@DisplayName("WAITING 상태의 대기열 정보 정상 조회")
		void getMyQueueStatus_Waiting_Success() {
			// given
			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));
			given(queueEntryRedisRepository.getMyRankInWaitingQueue(eventId, userId))
				.willReturn(5L);
			given(queueEntryRedisRepository.getWaitingAheadCount(eventId, userId))
				.willReturn(4L);
			given(queueEntryRedisRepository.getTotalWaitingCount(eventId))
				.willReturn(10L);

			// when
			QueueEntryStatusResponse response = queueEntryReadService.getMyQueueStatus(eventId, userId);

			// then
			assertThat(response).isInstanceOf(WaitingQueueResponse.class);
			WaitingQueueResponse waitingResponse = (WaitingQueueResponse)response;
			assertThat(waitingResponse.userId()).isEqualTo(userId);
			assertThat(waitingResponse.eventId()).isEqualTo(eventId);
			assertThat(waitingResponse.status()).isEqualTo(QueueEntryStatus.WAITING);
			assertThat(waitingResponse.queueRank()).isEqualTo(5);
			assertThat(waitingResponse.waitingAhead()).isEqualTo(4);

			then(queueEntryRepository).should().findByEvent_IdAndUser_Id(eventId, userId);
			then(queueEntryRedisRepository).should().getMyRankInWaitingQueue(eventId, userId);
		}

		@Test
		@DisplayName("ENTERED 상태의 대기열 정보 정상 조회")
		void getMyQueueStatus_Entered_Success() {
			// given
			testQueueEntry.enterQueue();

			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));

			// when
			QueueEntryStatusResponse response = queueEntryReadService.getMyQueueStatus(eventId, userId);

			// then
			assertThat(response).isInstanceOf(EnteredQueueResponse.class);
			EnteredQueueResponse enteredResponse = (EnteredQueueResponse)response;
			assertThat(enteredResponse.userId()).isEqualTo(userId);
			assertThat(enteredResponse.eventId()).isEqualTo(eventId);
			assertThat(enteredResponse.status()).isEqualTo(QueueEntryStatus.ENTERED);
			assertThat(enteredResponse.enteredAt()).isNotNull();
			assertThat(enteredResponse.expiredAt()).isNotNull();

			then(queueEntryRepository).should().findByEvent_IdAndUser_Id(eventId, userId);
		}

		@Test
		@DisplayName("EXPIRED 상태의 대기열 정보 정상 조회")
		void getMyQueueStatus_Expired_Success() {
			// given
			testQueueEntry.enterQueue();
			testQueueEntry.expire();

			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));

			// when
			QueueEntryStatusResponse response = queueEntryReadService.getMyQueueStatus(eventId, userId);

			// then
			assertThat(response).isInstanceOf(ExpiredQueueResponse.class);
			ExpiredQueueResponse expiredResponse = (ExpiredQueueResponse)response;
			assertThat(expiredResponse.userId()).isEqualTo(userId);
			assertThat(expiredResponse.status()).isEqualTo(QueueEntryStatus.EXPIRED);

			then(queueEntryRepository).should().findByEvent_IdAndUser_Id(eventId, userId);
		}

		@Test
		@DisplayName("COMPLETED 상태의 대기열 정보 정상 조회")
		void getMyQueueStatus_Completed_Success() {
			// given
			testQueueEntry.enterQueue();
			testQueueEntry.completePayment();

			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));

			// when
			QueueEntryStatusResponse response = queueEntryReadService.getMyQueueStatus(eventId, userId);

			// then
			assertThat(response).isInstanceOf(CompletedQueueResponse.class);
			CompletedQueueResponse completedResponse = (CompletedQueueResponse)response;
			assertThat(completedResponse.userId()).isEqualTo(userId);
			assertThat(completedResponse.status()).isEqualTo(QueueEntryStatus.COMPLETED);

			then(queueEntryRepository).should().findByEvent_IdAndUser_Id(eventId, userId);
		}

		@Test
		@DisplayName("존재하지 않는 대기열 조회 시 예외 발생")
		void getMyQueueStatus_NotFound_ThrowsException() {
			// given
			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> queueEntryReadService.getMyQueueStatus(eventId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", QueueEntryErrorCode.NOT_FOUND_QUEUE_ENTRY);

			then(queueEntryRepository).should().findByEvent_IdAndUser_Id(eventId, userId);
		}

		@Test
		@DisplayName("Redis 데이터가 없으면 DB 데이터 조회")
		void getMyQueueStatus_RedisEmpty_FallbackToDb() {
			// given
			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));
			given(queueEntryRedisRepository.getMyRankInWaitingQueue(eventId, userId))
				.willReturn(null); // Redis 데이터 없음
			given(queueEntryRepository.countByEvent_IdAndQueueRankLessThan(eventId, 5))
				.willReturn(4L);
			given(queueEntryRepository.countByEvent_IdAndQueueEntryStatus(eventId, QueueEntryStatus.WAITING))
				.willReturn(10L);

			// when
			QueueEntryStatusResponse response = queueEntryReadService.getMyQueueStatus(eventId, userId);

			// then
			assertThat(response).isInstanceOf(WaitingQueueResponse.class);
			WaitingQueueResponse waitingResponse = (WaitingQueueResponse)response;
			assertThat(waitingResponse.queueRank()).isEqualTo(5);
			assertThat(waitingResponse.waitingAhead()).isEqualTo(4);

			then(queueEntryRedisRepository).should().getMyRankInWaitingQueue(eventId, userId);
			then(queueEntryRepository).should().countByEvent_IdAndQueueRankLessThan(eventId, 5);
		}
	}

	@Nested
	@DisplayName("existsInWaitingQueue 테스트")
	class ExistsInWaitingQueueTest {

		@Test
		@DisplayName("Redis waiting queue에 있으면 true 반환")
		void existsInWaitingQueue_InWaitingQueue_ReturnsTrue() {
			// given
			given(queueEntryRedisRepository.isInWaitingQueue(eventId, userId))
				.willReturn(true);

			// when
			boolean result = queueEntryReadService.existsInWaitingQueue(eventId, userId);

			// then
			assertThat(result).isTrue();
			then(queueEntryRedisRepository).should().isInWaitingQueue(eventId, userId);
			then(queueEntryRepository).should(never()).existsByEvent_IdAndUser_Id(any(), any());
		}

		@Test
		@DisplayName("Redis entered queue에 있으면 true 반환")
		void existsInWaitingQueue_InEnteredQueue_ReturnsTrue() {
			// given
			given(queueEntryRedisRepository.isInWaitingQueue(eventId, userId))
				.willReturn(false);
			given(queueEntryRedisRepository.isInEnteredQueue(eventId, userId))
				.willReturn(true);

			// when
			boolean result = queueEntryReadService.existsInWaitingQueue(eventId, userId);

			// then
			assertThat(result).isTrue();
			then(queueEntryRedisRepository).should().isInWaitingQueue(eventId, userId);
			then(queueEntryRedisRepository).should().isInEnteredQueue(eventId, userId);
		}

		@Test
		@DisplayName("Redis에 없으면 DB 조회")
		void existsInWaitingQueue_NotInRedis_ChecksDatabase() {
			// given
			given(queueEntryRedisRepository.isInWaitingQueue(eventId, userId))
				.willReturn(false);
			given(queueEntryRedisRepository.isInEnteredQueue(eventId, userId))
				.willReturn(false);
			given(queueEntryRepository.existsByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(true);

			// when
			boolean result = queueEntryReadService.existsInWaitingQueue(eventId, userId);

			// then
			assertThat(result).isTrue();
			then(queueEntryRepository).should().existsByEvent_IdAndUser_Id(eventId, userId);
		}

		@Test
		@DisplayName("Redis와 DB 모두 없으면 false 반환")
		void existsInWaitingQueue_NotFound_ReturnsFalse() {
			// given
			given(queueEntryRedisRepository.isInWaitingQueue(eventId, userId))
				.willReturn(false);
			given(queueEntryRedisRepository.isInEnteredQueue(eventId, userId))
				.willReturn(false);
			given(queueEntryRepository.existsByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(false);

			// when
			boolean result = queueEntryReadService.existsInWaitingQueue(eventId, userId);

			// then
			assertThat(result).isFalse();
			then(queueEntryRepository).should().existsByEvent_IdAndUser_Id(eventId, userId);
		}

		@Test
		@DisplayName("Redis 조회 실패 시 DB fallback")
		void existsInWaitingQueue_RedisFailure_FallbackToDb() {
			// given
			given(queueEntryRedisRepository.isInWaitingQueue(eventId, userId))
				.willThrow(new RuntimeException("Redis connection failed"));
			given(queueEntryRepository.existsByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(true);

			// when
			boolean result = queueEntryReadService.existsInWaitingQueue(eventId, userId);

			// then
			assertThat(result).isTrue();
			then(queueEntryRepository).should().existsByEvent_IdAndUser_Id(eventId, userId);
		}
	}

	@Nested
	@DisplayName("isUserEntered 테스트")
	class IsUserEnteredTest {

		@Test
		@DisplayName("Redis entered queue에 있으면 true 반환")
		void isUserEntered_InRedis_ReturnsTrue() {
			// given
			given(queueEntryRedisRepository.isInEnteredQueue(eventId, userId))
				.willReturn(true);

			// when
			boolean result = queueEntryReadService.isUserEntered(eventId, userId);

			// then
			assertThat(result).isTrue();
			then(queueEntryRedisRepository).should().isInEnteredQueue(eventId, userId);
			then(queueEntryRepository).should(never()).findByEvent_IdAndUser_Id(any(), any());
		}

		@Test
		@DisplayName("Redis 조회 실패 시 DB의 ENTERED 상태 확인")
		void isUserEntered_RedisFailure_ChecksDatabase() {
			// given
			testQueueEntry.enterQueue();

			given(queueEntryRedisRepository.isInEnteredQueue(eventId, userId))
				.willThrow(new RuntimeException("Redis failure"));
			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));

			// when
			boolean result = queueEntryReadService.isUserEntered(eventId, userId);

			// then
			assertThat(result).isTrue();
			then(queueEntryRepository).should().findByEvent_IdAndUser_Id(eventId, userId);
		}

		@Test
		@DisplayName("DB에 없거나 ENTERED가 아니면 false 반환")
		void isUserEntered_NotEntered_ReturnsFalse() {
			// given - testQueueEntry는 WAITING 상태

			given(queueEntryRedisRepository.isInEnteredQueue(eventId, userId))
				.willThrow(new RuntimeException("Redis failure"));
			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.of(testQueueEntry));

			// when
			boolean result = queueEntryReadService.isUserEntered(eventId, userId);

			// then
			assertThat(result).isFalse();
		}

		@Test
		@DisplayName("DB에 QueueEntry가 없으면 false 반환")
		void isUserEntered_NotFoundInDb_ReturnsFalse() {
			// given
			given(queueEntryRedisRepository.isInEnteredQueue(eventId, userId))
				.willThrow(new RuntimeException("Redis failure"));
			given(queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId))
				.willReturn(Optional.empty());

			// when
			boolean result = queueEntryReadService.isUserEntered(eventId, userId);

			// then
			assertThat(result).isFalse();
		}
	}

	@Nested
	@DisplayName("buildWaitingQueueResponseFromRank 테스트")
	class BuildWaitingQueueResponseFromRankTest {

		@Test
		@DisplayName("1순위 사용자는 대기 인원 0명, 예상 대기 시간 3분으로 설정")
		void buildWaitingQueueResponse_FirstRank() {
			// given
			int rank = 1;
			int waitingAhead = 0;
			int totalWaitingCount = 10;

			// when
			WaitingQueueResponse response = queueEntryReadService.buildWaitingQueueResponseFromRank(
				userId, eventId, rank, waitingAhead, totalWaitingCount
			);

			// then
			assertThat(response.queueRank()).isEqualTo(1);
			assertThat(response.waitingAhead()).isEqualTo(0);
			assertThat(response.estimatedWaitTime()).isEqualTo(1);
			assertThat(response.progress()).isEqualTo(99);
		}

		@Test
		@DisplayName("예상 대기 시간 계산")
		void buildWaitingQueueResponse_WithWaitingAhead() {
			// given
			int rank = 5;
			int waitingAhead = 4;
			int totalWaitingCount = 10;

			// when
			WaitingQueueResponse response = queueEntryReadService.buildWaitingQueueResponseFromRank(
				userId, eventId, rank, waitingAhead, totalWaitingCount
			);

			// then
			assertThat(response.waitingAhead()).isEqualTo(4);
			assertThat(response.estimatedWaitTime()).isEqualTo(8); // 4 * 2분
			assertThat(response.progress()).isEqualTo(60); // (10-4)*100/10
		}

		@Test
		@DisplayName("마지막 순위 사용자")
		void buildWaitingQueueResponse_LastRank() {
			// given
			int rank = 100;
			int waitingAhead = 99;
			int totalWaitingCount = 100;

			// when
			WaitingQueueResponse response = queueEntryReadService.buildWaitingQueueResponseFromRank(
				userId, eventId, rank, waitingAhead, totalWaitingCount
			);

			// then
			assertThat(response.waitingAhead()).isEqualTo(99);
			assertThat(response.estimatedWaitTime()).isEqualTo(198); // 99 * 2분
			assertThat(response.progress()).isEqualTo(1); // (100-99)*100/100
		}
	}

	@Nested
	@DisplayName("getQueueStatistics 테스트")
	class GetQueueStatisticsTest {

		@Test
		@DisplayName("대기열 통계 정상 조회")
		void getQueueStatistics_Success() {
			// given
			given(queueEntryRepository.countByEvent_Id(eventId)).willReturn(10L);
			given(queueEntryRepository.countByEvent_IdAndQueueEntryStatus(eventId, QueueEntryStatus.WAITING))
				.willReturn(4L);
			given(queueEntryRepository.countByEvent_IdAndQueueEntryStatus(eventId, QueueEntryStatus.ENTERED))
				.willReturn(3L);
			given(queueEntryRepository.countByEvent_IdAndQueueEntryStatus(eventId, QueueEntryStatus.EXPIRED))
				.willReturn(2L);

			// when
			var response = queueEntryReadService.getQueueStatistics(eventId);

			// then
			assertThat(response.eventId()).isEqualTo(eventId);
			assertThat(response.totalCount()).isEqualTo(10L);
			assertThat(response.waitingCount()).isEqualTo(4L);
			assertThat(response.enteredCount()).isEqualTo(3L);
			assertThat(response.expiredCount()).isEqualTo(2L);

			then(queueEntryRepository).should().countByEvent_Id(eventId);
		}

		@Test
		@DisplayName("대기열이 없으면 예외 발생")
		void getQueueStatistics_NoQueue_ThrowsException() {
			// given
			given(queueEntryRepository.countByEvent_Id(eventId)).willReturn(0L);

			// when & then
			assertThatThrownBy(() -> queueEntryReadService.getQueueStatistics(eventId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", QueueEntryErrorCode.NOT_FOUND_QUEUE_ENTRY);

			then(queueEntryRepository).should().countByEvent_Id(eventId);
			then(queueEntryRepository).should(never())
				.countByEvent_IdAndQueueEntryStatus(any(), any());
		}
	}

	@Nested
	@DisplayName("대기열 상태 전환 검증 테스트")
	class QueueEntryStatusTransitionTest {

		@Test
		@DisplayName("WAITING -> ENTERED 상태 상태 전환 성공")
		void queueEntryStatus_WaitingToEntered_Success() {
			// given
			assertThat(testQueueEntry.getQueueEntryStatus()).isEqualTo(QueueEntryStatus.WAITING);

			// when
			testQueueEntry.enterQueue();

			// then
			assertThat(testQueueEntry.getQueueEntryStatus()).isEqualTo(QueueEntryStatus.ENTERED);
			assertThat(testQueueEntry.getEnteredAt()).isNotNull();
			assertThat(testQueueEntry.getExpiredAt()).isNotNull();
		}

		@Test
		@DisplayName("ENTERED -> EXPIRED 상태 전환 성공")
		void queueEntryStatus_EnteredToExpired_Success() {
			// given
			testQueueEntry.enterQueue();
			assertThat(testQueueEntry.getQueueEntryStatus()).isEqualTo(QueueEntryStatus.ENTERED);

			// when
			testQueueEntry.expire();

			// then
			assertThat(testQueueEntry.getQueueEntryStatus()).isEqualTo(QueueEntryStatus.EXPIRED);
		}

		@Test
		@DisplayName("ENTERED -> COMPLETED 상태 전환 성공")
		void queueEntryStatus_EnteredToCompleted_Success() {
			// given
			testQueueEntry.enterQueue();
			assertThat(testQueueEntry.getQueueEntryStatus()).isEqualTo(QueueEntryStatus.ENTERED);

			// when
			testQueueEntry.completePayment();

			// then
			assertThat(testQueueEntry.getQueueEntryStatus()).isEqualTo(QueueEntryStatus.COMPLETED);
		}
	}
}
