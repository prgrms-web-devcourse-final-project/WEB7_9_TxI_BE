package com.back.api.event.scheduler;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventStatus;
import com.back.domain.event.repository.EventRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventOpenScheduler 단위 테스트 - 예외 시나리오")
class EventOpenSchedulerUnitTest {

	@Mock
	private EventRepository eventRepository;

	@InjectMocks
	private EventOpenScheduler scheduler;

	@Test
	@DisplayName("processStatusTransition - 이벤트 저장 실패 시 다른 이벤트 처리 계속")
	void processStatusTransition_eventSaveFails_continuesWithOthers() {
		// given
		LocalDateTime now = LocalDateTime.now();
		Event failingEvent = mock(Event.class);
		Event successEvent = mock(Event.class);

		when(failingEvent.getId()).thenReturn(1L);
		when(failingEvent.getPreOpenAt()).thenReturn(now.minusMinutes(1));
		doThrow(new RuntimeException("DB Error")).when(failingEvent).changeStatus(any());

		when(successEvent.getId()).thenReturn(2L);
		when(successEvent.getPreOpenAt()).thenReturn(now.minusMinutes(1));

		when(eventRepository.findByStatus(EventStatus.READY))
			.thenReturn(List.of(failingEvent, successEvent));

		// when
		scheduler.openPreRegistration();

		// then
		verify(eventRepository).findByStatus(EventStatus.READY);
		verify(failingEvent).changeStatus(EventStatus.PRE_OPEN);
		verify(successEvent).changeStatus(EventStatus.PRE_OPEN);
		verify(eventRepository).save(successEvent);
	}

	@Test
	@DisplayName("processStatusTransition - repository.findByStatus() 실패 시 예외 로그 기록")
	void processStatusTransition_findByStatusFails_logsError() {
		// given
		when(eventRepository.findByStatus(EventStatus.READY))
			.thenThrow(new RuntimeException("DB Connection Error"));

		// when
		scheduler.openPreRegistration();

		// then: 예외가 로그되고 정상 종료
		verify(eventRepository).findByStatus(EventStatus.READY);
	}

	@Test
	@DisplayName("closePreRegistration - repository 오류 시에도 정상 종료")
	void closePreRegistration_repositoryError_handlesGracefully() {
		// given
		when(eventRepository.findByStatus(EventStatus.PRE_OPEN))
			.thenThrow(new RuntimeException("Database error"));

		// when
		scheduler.closePreRegistration();

		// then
		verify(eventRepository).findByStatus(EventStatus.PRE_OPEN);
	}


	@Test
	@DisplayName("closeTicketing - repository.save() 실패 시 다음 이벤트 계속 처리")
	void closeTicketing_saveFails_continuesWithOthers() {
		// given
		LocalDateTime now = LocalDateTime.now();
		Event failingEvent = mock(Event.class);
		Event successEvent = mock(Event.class);

		when(failingEvent.getId()).thenReturn(1L);
		when(failingEvent.getTicketCloseAt()).thenReturn(now.minusMinutes(1));
		when(eventRepository.save(failingEvent)).thenThrow(new RuntimeException("Save failed"));

		when(successEvent.getId()).thenReturn(2L);
		when(successEvent.getTicketCloseAt()).thenReturn(now.minusMinutes(1));

		when(eventRepository.findByStatus(EventStatus.OPEN))
			.thenReturn(List.of(failingEvent, successEvent));

		// when
		scheduler.closeTicketing();

		// then
		verify(failingEvent).changeStatus(EventStatus.CLOSED);
		verify(successEvent).changeStatus(EventStatus.CLOSED);
		verify(eventRepository).save(failingEvent);
		verify(eventRepository).save(successEvent);
	}

	// ========== openTicketing() 메서드 Exception 커버리지 테스트 ==========

	@Test
	@DisplayName("openTicketing - 이벤트 처리 중 예외 발생 시 다음 이벤트 계속 처리")
	void openTicketing_eventProcessingFails_continuesWithOthers() {
		// given
		LocalDateTime now = LocalDateTime.now();
		Event failingEvent = mock(Event.class);
		Event successEvent = mock(Event.class);

		when(failingEvent.getId()).thenReturn(1L);
		when(failingEvent.getTicketOpenAt()).thenReturn(now.minusMinutes(1));
		doThrow(new RuntimeException("Event processing error")).when(failingEvent).changeStatus(any());

		when(successEvent.getId()).thenReturn(2L);
		when(successEvent.getTicketOpenAt()).thenReturn(now.minusMinutes(1));

		when(eventRepository.findByStatus(EventStatus.QUEUE_READY))
			.thenReturn(List.of(failingEvent, successEvent));

		// when
		scheduler.openTicketing();

		// then
		verify(failingEvent).changeStatus(EventStatus.OPEN);
		verify(successEvent).changeStatus(EventStatus.OPEN);
		verify(eventRepository).save(successEvent);
	}

	@Test
	@DisplayName("openTicketing - repository.findByStatus() 실패 시 예외 로그 기록")
	void openTicketing_findByStatusFails_logsError() {
		// given
		when(eventRepository.findByStatus(EventStatus.QUEUE_READY))
			.thenThrow(new RuntimeException("DB Connection Error"));

		// when
		scheduler.openTicketing();

		// then: 예외가 로그되고 정상 종료
		verify(eventRepository).findByStatus(EventStatus.QUEUE_READY);
	}
}
