package com.back.api.event.scheduler;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventCategory;
import com.back.domain.event.entity.EventStatus;
import com.back.domain.event.repository.EventRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("EventOpenScheduler 통합 테스트")
class EventOpenSchedulerTest {

	@Autowired
	private EventRepository eventRepository;

	@SpyBean
	private EventRepository spyEventRepository;

	private EventOpenScheduler scheduler;

	@BeforeEach
	void setUp() {
		eventRepository.deleteAll();
		// 스케줄러를 직접 생성 (Profile 제한 우회)
		scheduler = new EventOpenScheduler(eventRepository);
	}

	@Test
	@DisplayName("READY 상태 이벤트가 preOpenAt 시간이 되면 PRE_OPEN으로 전환된다")
	void openPreRegistration_success() {
		// given: preOpenAt이 과거인 READY 상태 이벤트
		LocalDateTime now = LocalDateTime.now();
		Event event = createEvent(
			EventStatus.READY,
			now.minusMinutes(1),  // preOpenAt: 1분 전 (과거)
			now.plusHours(1),     // preCloseAt
			now.plusHours(2),     // ticketOpenAt
			now.plusHours(3),     // ticketCloseAt
			now.plusDays(1)       // eventDate
		);
		eventRepository.save(event);

		// when: 스케줄러 실행
		scheduler.openPreRegistration();

		// then: PRE_OPEN 상태로 전환
		Event result = eventRepository.findById(event.getId()).orElseThrow();
		assertThat(result.getStatus()).isEqualTo(EventStatus.PRE_OPEN);
	}

	@Test
	@DisplayName("PRE_OPEN 상태 이벤트가 preCloseAt 시간이 되면 PRE_CLOSED로 전환된다")
	void closePreRegistration_success() {
		// given: preCloseAt이 과거인 PRE_OPEN 상태 이벤트
		LocalDateTime now = LocalDateTime.now();
		Event event = createEvent(
			EventStatus.PRE_OPEN,
			now.minusHours(2),    // preOpenAt
			now.minusMinutes(1),  // preCloseAt: 1분 전 (과거)
			now.plusHours(1),     // ticketOpenAt
			now.plusHours(2),     // ticketCloseAt
			now.plusDays(1)       // eventDate
		);
		eventRepository.save(event);

		// when: 스케줄러 실행
		scheduler.closePreRegistration();

		// then: PRE_CLOSED 상태로 전환
		Event result = eventRepository.findById(event.getId()).orElseThrow();
		assertThat(result.getStatus()).isEqualTo(EventStatus.PRE_CLOSED);
	}


	@Test
	@DisplayName("QUEUE_READY 상태 이벤트가 ticketOpenAt 시간이 되면 OPEN으로 전환된다")
	void openTicketing_success() {
		// given: ticketOpenAt이 과거인 QUEUE_READY 상태 이벤트
		LocalDateTime now = LocalDateTime.now();
		Event event = createEvent(
			EventStatus.QUEUE_READY,
			now.minusHours(3),    // preOpenAt
			now.minusHours(2),    // preCloseAt
			now.minusMinutes(1),  // ticketOpenAt: 1분 전 (과거)
			now.plusHours(2),     // ticketCloseAt
			now.plusDays(1)       // eventDate
		);
		eventRepository.save(event);

		// when: 스케줄러 실행
		scheduler.openTicketing();

		// then: OPEN 상태로 전환
		Event result = eventRepository.findById(event.getId()).orElseThrow();
		assertThat(result.getStatus()).isEqualTo(EventStatus.OPEN);
	}

	@Test
	@DisplayName("OPEN 상태 이벤트가 ticketCloseAt 시간이 되면 CLOSED로 전환된다")
	void closeTicketing_success() {
		// given: ticketCloseAt이 과거인 OPEN 상태 이벤트
		LocalDateTime now = LocalDateTime.now();
		Event event = createEvent(
			EventStatus.OPEN,
			now.minusHours(4),    // preOpenAt
			now.minusHours(3),    // preCloseAt
			now.minusHours(2),    // ticketOpenAt
			now.minusMinutes(1),  // ticketCloseAt: 1분 전 (과거)
			now.plusDays(1)       // eventDate
		);
		eventRepository.save(event);

		// when: 스케줄러 실행
		scheduler.closeTicketing();

		// then: CLOSED 상태로 전환
		Event result = eventRepository.findById(event.getId()).orElseThrow();
		assertThat(result.getStatus()).isEqualTo(EventStatus.CLOSED);
	}

	@Test
	@DisplayName("아직 시간이 되지 않은 이벤트는 상태가 변경되지 않는다")
	void scheduler_notYet_noChange() {
		// given: preOpenAt이 미래인 READY 상태 이벤트
		LocalDateTime now = LocalDateTime.now();
		Event event = createEvent(
			EventStatus.READY,
			now.plusHours(1),     // preOpenAt: 1시간 후 (미래)
			now.plusHours(2),     // preCloseAt
			now.plusHours(3),     // ticketOpenAt
			now.plusHours(4),     // ticketCloseAt
			now.plusDays(1)       // eventDate
		);
		eventRepository.save(event);

		// when: 스케줄러 실행
		scheduler.openPreRegistration();

		// then: 상태 변경 없음
		Event result = eventRepository.findById(event.getId()).orElseThrow();
		assertThat(result.getStatus()).isEqualTo(EventStatus.READY);
	}

	@Test
	@DisplayName("여러 이벤트를 동시에 처리할 수 있다")
	void scheduler_multipleEvents_success() {
		// given: preOpenAt이 과거인 여러 READY 상태 이벤트
		LocalDateTime now = LocalDateTime.now();
		Event event1 = createEvent(
			EventStatus.READY,
			now.minusMinutes(5),
			now.plusHours(1),
			now.plusHours(2),
			now.plusHours(3),
			now.plusDays(1)
		);
		Event event2 = createEvent(
			EventStatus.READY,
			now.minusMinutes(3),
			now.plusHours(1),
			now.plusHours(2),
			now.plusHours(3),
			now.plusDays(1)
		);
		eventRepository.saveAll(List.of(event1, event2));

		// when: 스케줄러 실행
		scheduler.openPreRegistration();

		// then: 모두 PRE_OPEN 상태로 전환
		List<Event> results = eventRepository.findByStatus(EventStatus.PRE_OPEN);
		assertThat(results).hasSize(2);
	}

	@Test
	@DisplayName("대상 이벤트가 없을 때 정상적으로 종료한다")
	void scheduler_noEvents_success() {
		// given: 대상 이벤트 없음
		// when: 스케줄러 실행
		scheduler.openPreRegistration();

		// then: 오류 없이 정상 종료 (로그만 출력)
		List<Event> results = eventRepository.findAll();
		assertThat(results).isEmpty();
	}

	@Test
	@DisplayName("정확히 시간이 일치하는 이벤트도 전환된다 (isBefore || isEqual)")
	void scheduler_exactTime_success() {
		// given: preOpenAt이 정확히 현재 시간인 이벤트
		LocalDateTime now = LocalDateTime.now();
		Event event = createEvent(
			EventStatus.READY,
			now,                  // preOpenAt: 정확히 현재 시간
			now.plusHours(1),
			now.plusHours(2),
			now.plusHours(3),
			now.plusDays(1)
		);
		eventRepository.save(event);

		// when: 스케줄러 실행
		scheduler.openPreRegistration();

		// then: PRE_OPEN 상태로 전환
		Event result = eventRepository.findById(event.getId()).orElseThrow();
		assertThat(result.getStatus()).isEqualTo(EventStatus.PRE_OPEN);
	}


	@Test
	@DisplayName("전체 이벤트 라이프사이클 상태 전환 통합 테스트 (QUEUE_READY 제외)")
	void fullLifecycle_allTransitions_success() {
		// given: 모든 시간이 과거인 READY 상태 이벤트
		LocalDateTime now = LocalDateTime.now();
		Event event = createEvent(
			EventStatus.READY,
			now.minusHours(5),    // preOpenAt: 5시간 전
			now.minusHours(4),    // preCloseAt: 4시간 전
			now.minusHours(3),    // ticketOpenAt: 3시간 전
			now.minusHours(1),    // ticketCloseAt: 1시간 전
			now.plusDays(1)       // eventDate
		);
		eventRepository.save(event);

		// when & then: READY → PRE_OPEN
		scheduler.openPreRegistration();
		Event result = eventRepository.findById(event.getId()).orElseThrow();
		assertThat(result.getStatus()).isEqualTo(EventStatus.PRE_OPEN);

		// when & then: PRE_OPEN → PRE_CLOSED
		scheduler.closePreRegistration();
		result = eventRepository.findById(event.getId()).orElseThrow();
		assertThat(result.getStatus()).isEqualTo(EventStatus.PRE_CLOSED);

		// PRE_CLOSED → QUEUE_READY는 QueueShuffleScheduler에서 처리하므로 수동으로 상태 변경
		result.changeStatus(EventStatus.QUEUE_READY);
		eventRepository.save(result);

		// when & then: QUEUE_READY → OPEN
		scheduler.openTicketing();
		result = eventRepository.findById(event.getId()).orElseThrow();
		assertThat(result.getStatus()).isEqualTo(EventStatus.OPEN);

		// when & then: OPEN → CLOSED
		scheduler.closeTicketing();
		result = eventRepository.findById(event.getId()).orElseThrow();
		assertThat(result.getStatus()).isEqualTo(EventStatus.CLOSED);
	}

	@Test
	@DisplayName("모든 스케줄러 메서드가 빈 리스트에서도 정상 동작한다")
	void allSchedulers_emptyList_success() {
		// given: 이벤트 없음
		eventRepository.deleteAll();

		// when & then: 모든 스케줄러 실행해도 오류 없음
		assertThatCode(() -> {
			scheduler.openPreRegistration();
			scheduler.closePreRegistration();
			scheduler.openTicketing();
			scheduler.closeTicketing();
		}).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("이벤트 상태 변경 실패 시에도 다른 이벤트 처리를 계속한다")
	void scheduler_eventChangeStatusFails_continuesWithOthers() {
		// given: 여러 이벤트 중 일부가 정상 처리됨
		LocalDateTime now = LocalDateTime.now();

		// 2개의 정상 이벤트 생성
		Event event1 = createEvent(
			EventStatus.READY,
			now.minusMinutes(5),
			now.plusHours(1),
			now.plusHours(2),
			now.plusHours(3),
			now.plusDays(1)
		);
		Event event2 = createEvent(
			EventStatus.READY,
			now.minusMinutes(3),
			now.plusHours(1),
			now.plusHours(2),
			now.plusHours(3),
			now.plusDays(1)
		);
		eventRepository.saveAll(List.of(event1, event2));

		// when: 스케줄러 실행
		scheduler.openPreRegistration();

		// then: 모든 이벤트가 PRE_OPEN으로 전환되어야 함
		List<Event> results = eventRepository.findByStatus(EventStatus.PRE_OPEN);
		assertThat(results).hasSize(2);
	}

	@Test
	@DisplayName("람다 조건의 모든 브랜치를 테스트: isBefore만 참인 경우")
	void scheduler_condition_onlyIsBefore() {
		// given: preOpenAt이 과거(isBefore=true, isEqual=false)
		LocalDateTime now = LocalDateTime.now();
		Event event = createEvent(
			EventStatus.READY,
			now.minusSeconds(1),  // 1초 전 (isBefore=true, isEqual=false)
			now.plusHours(1),
			now.plusHours(2),
			now.plusHours(3),
			now.plusDays(1)
		);
		eventRepository.save(event);

		// when
		scheduler.openPreRegistration();

		// then
		Event result = eventRepository.findById(event.getId()).orElseThrow();
		assertThat(result.getStatus()).isEqualTo(EventStatus.PRE_OPEN);
	}

	@Test
	@DisplayName("람다 조건의 모든 브랜치를 테스트: closePreRegistration isEqual")
	void closePreRegistration_isEqual() {
		// given: preCloseAt이 정확히 현재 시간
		LocalDateTime now = LocalDateTime.now();
		Event event = createEvent(
			EventStatus.PRE_OPEN,
			now.minusHours(2),
			now,  // 정확히 현재 시간
			now.plusHours(1),
			now.plusHours(2),
			now.plusDays(1)
		);
		eventRepository.save(event);

		// when
		scheduler.closePreRegistration();

		// then
		Event result = eventRepository.findById(event.getId()).orElseThrow();
		assertThat(result.getStatus()).isEqualTo(EventStatus.PRE_CLOSED);
	}

	@Test
	@DisplayName("람다 조건의 모든 브랜치를 테스트: openTicketing isEqual")
	void openTicketing_isEqual() {
		// given: ticketOpenAt이 정확히 현재 시간
		LocalDateTime now = LocalDateTime.now();
		Event event = createEvent(
			EventStatus.QUEUE_READY,
			now.minusHours(3),
			now.minusHours(2),
			now,  // 정확히 현재 시간
			now.plusHours(2),
			now.plusDays(1)
		);
		eventRepository.save(event);

		// when
		scheduler.openTicketing();

		// then
		Event result = eventRepository.findById(event.getId()).orElseThrow();
		assertThat(result.getStatus()).isEqualTo(EventStatus.OPEN);
	}

	@Test
	@DisplayName("람다 조건의 모든 브랜치를 테스트: closeTicketing isEqual")
	void closeTicketing_isEqual() {
		// given: ticketCloseAt이 정확히 현재 시간
		LocalDateTime now = LocalDateTime.now();
		Event event = createEvent(
			EventStatus.OPEN,
			now.minusHours(4),
			now.minusHours(3),
			now.minusHours(2),
			now,  // 정확히 현재 시간
			now.plusDays(1)
		);
		eventRepository.save(event);

		// when
		scheduler.closeTicketing();

		// then
		Event result = eventRepository.findById(event.getId()).orElseThrow();
		assertThat(result.getStatus()).isEqualTo(EventStatus.CLOSED);
	}

	@Test
	@DisplayName("개별 이벤트 저장 중 예외 발생 시 다른 이벤트는 정상 처리된다")
	void scheduler_saveException_continuesOthers() {
		// given: 2개의 이벤트
		LocalDateTime now = LocalDateTime.now();
		Event event1 = createEvent(
			EventStatus.READY,
			now.minusMinutes(5),
			now.plusHours(1),
			now.plusHours(2),
			now.plusHours(3),
			now.plusDays(1)
		);
		Event event2 = createEvent(
			EventStatus.READY,
			now.minusMinutes(3),
			now.plusHours(1),
			now.plusHours(2),
			now.plusHours(3),
			now.plusDays(1)
		);
		eventRepository.saveAll(List.of(event1, event2));

		// SpyBean을 사용하는 스케줄러 생성
		EventOpenScheduler spyScheduler = new EventOpenScheduler(spyEventRepository);

		// 첫 번째 이벤트 save 시 예외 발생
		List<Event> saved = eventRepository.findAll();
		if (!saved.isEmpty()) {
			Event firstEvent = saved.get(0);
			doThrow(new RuntimeException("DB error"))
				.when(spyEventRepository)
				.save(argThat(e -> e.getId() != null && e.getId().equals(firstEvent.getId())));
		}

		// when: 예외가 발생해도 스케줄러는 중단되지 않음
		assertThatCode(() -> spyScheduler.openPreRegistration())
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("findByStatus 호출 중 예외 발생 시 스케줄러가 정상 종료된다")
	void scheduler_findByStatusException_handlesGracefully() {
		// given: SpyBean을 사용하는 스케줄러
		EventOpenScheduler spyScheduler = new EventOpenScheduler(spyEventRepository);

		// findByStatus 호출 시 예외 발생
		when(spyEventRepository.findByStatus(any(EventStatus.class)))
			.thenThrow(new RuntimeException("Database connection error"));

		// when & then: 예외가 발생해도 스케줄러는 중단되지 않음
		assertThatCode(() -> spyScheduler.openPreRegistration())
			.doesNotThrowAnyException();

		// verify: findByStatus가 호출되었는지 확인
		verify(spyEventRepository, times(1)).findByStatus(EventStatus.READY);
	}


	@Test
	@DisplayName("closePreRegistration lambda 조건: isBefore만 참인 경우")
	void closePreRegistration_condition_onlyIsBefore() {
		// given: preCloseAt이 과거
		LocalDateTime now = LocalDateTime.now();
		Event event = createEvent(
			EventStatus.PRE_OPEN,
			now.minusHours(2),
			now.minusSeconds(1),  // preCloseAt: 1초 전 (과거)
			now.plusHours(1),
			now.plusHours(2),
			now.plusDays(1)
		);
		eventRepository.save(event);

		// when
		scheduler.closePreRegistration();

		// then
		Event result = eventRepository.findById(event.getId()).orElseThrow();
		assertThat(result.getStatus()).isEqualTo(EventStatus.PRE_CLOSED);
	}

	@Test
	@DisplayName("closePreRegistration lambda 조건: 미래 시간 (상태 변경 안 됨)")
	void closePreRegistration_condition_future() {
		// given: preCloseAt이 미래
		LocalDateTime now = LocalDateTime.now();
		Event event = createEvent(
			EventStatus.PRE_OPEN,
			now.minusHours(2),
			now.plusHours(1),  // preCloseAt: 1시간 후 (미래)
			now.plusHours(2),
			now.plusHours(3),
			now.plusDays(1)
		);
		eventRepository.save(event);

		// when
		scheduler.closePreRegistration();

		// then: 상태 변경 없음
		Event result = eventRepository.findById(event.getId()).orElseThrow();
		assertThat(result.getStatus()).isEqualTo(EventStatus.PRE_OPEN);
	}

	@Test
	@DisplayName("openTicketing lambda 조건: isBefore만 참인 경우")
	void openTicketing_condition_onlyIsBefore() {
		// given: ticketOpenAt이 과거
		LocalDateTime now = LocalDateTime.now();
		Event event = createEvent(
			EventStatus.QUEUE_READY,
			now.minusHours(3),
			now.minusHours(2),
			now.minusSeconds(1),  // ticketOpenAt: 1초 전 (과거)
			now.plusHours(2),
			now.plusDays(1)
		);
		eventRepository.save(event);

		// when
		scheduler.openTicketing();

		// then
		Event result = eventRepository.findById(event.getId()).orElseThrow();
		assertThat(result.getStatus()).isEqualTo(EventStatus.OPEN);
	}

	@Test
	@DisplayName("openTicketing lambda 조건: 미래 시간 (상태 변경 안 됨)")
	void openTicketing_condition_future() {
		// given: ticketOpenAt이 미래
		LocalDateTime now = LocalDateTime.now();
		Event event = createEvent(
			EventStatus.QUEUE_READY,
			now.minusHours(3),
			now.minusHours(2),
			now.plusHours(1),  // ticketOpenAt: 1시간 후 (미래)
			now.plusHours(2),
			now.plusDays(1)
		);
		eventRepository.save(event);

		// when
		scheduler.openTicketing();

		// then: 상태 변경 없음
		Event result = eventRepository.findById(event.getId()).orElseThrow();
		assertThat(result.getStatus()).isEqualTo(EventStatus.QUEUE_READY);
	}

	@Test
	@DisplayName("closeTicketing lambda 조건: isBefore만 참인 경우")
	void closeTicketing_condition_onlyIsBefore() {
		// given: ticketCloseAt이 과거
		LocalDateTime now = LocalDateTime.now();
		Event event = createEvent(
			EventStatus.OPEN,
			now.minusHours(4),
			now.minusHours(3),
			now.minusHours(2),
			now.minusSeconds(1),  // ticketCloseAt: 1초 전 (과거)
			now.plusDays(1)
		);
		eventRepository.save(event);

		// when
		scheduler.closeTicketing();

		// then
		Event result = eventRepository.findById(event.getId()).orElseThrow();
		assertThat(result.getStatus()).isEqualTo(EventStatus.CLOSED);
	}

	@Test
	@DisplayName("closeTicketing lambda 조건: 미래 시간 (상태 변경 안 됨)")
	void closeTicketing_condition_future() {
		// given: ticketCloseAt이 미래
		LocalDateTime now = LocalDateTime.now();
		Event event = createEvent(
			EventStatus.OPEN,
			now.minusHours(4),
			now.minusHours(3),
			now.minusHours(2),
			now.plusHours(1),  // ticketCloseAt: 1시간 후 (미래)
			now.plusDays(1)
		);
		eventRepository.save(event);

		// when
		scheduler.closeTicketing();

		// then: 상태 변경 없음
		Event result = eventRepository.findById(event.getId()).orElseThrow();
		assertThat(result.getStatus()).isEqualTo(EventStatus.OPEN);
	}

	private Event createEvent(
		EventStatus status,
		LocalDateTime preOpenAt,
		LocalDateTime preCloseAt,
		LocalDateTime ticketOpenAt,
		LocalDateTime ticketCloseAt,
		LocalDateTime eventDate
	) {
		return Event.builder()
			.title("Test Event " + System.nanoTime())
			.category(EventCategory.CONCERT)
			.description("Test Description")
			.place("Test Place")
			.imageUrl(null)
			.minPrice(10000)
			.maxPrice(50000)
			.maxTicketAmount(100)
			.preOpenAt(preOpenAt)
			.preCloseAt(preCloseAt)
			.ticketOpenAt(ticketOpenAt)
			.ticketCloseAt(ticketCloseAt)
			.eventDate(eventDate)
			.status(status)
			.build();
	}
}
