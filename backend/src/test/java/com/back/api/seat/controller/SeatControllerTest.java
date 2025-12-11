package com.back.api.seat.controller;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.seat.dto.response.SeatStatusMessage;
import com.back.config.TestSeatStatusEventListener;
import com.back.domain.event.entity.Event;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.queue.repository.QueueEntryRedisRepository;
import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.entity.SeatGrade;
import com.back.domain.seat.entity.SeatStatus;
import com.back.domain.seat.repository.SeatRepository;
import com.back.global.error.code.SeatErrorCode;
import com.back.support.factory.EventFactory;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("SeatController 통합 테스트")
class SeatControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private SeatRepository seatRepository;

	@Autowired
	private QueueEntryRedisRepository queueEntryRedisRepository;

	@Autowired
	private TestSeatStatusEventListener testEventListener;

	private Event testEvent;

	@BeforeEach
	void setUp() {
		// EventFactory 활용
		testEvent = EventFactory.fakeEvent("Test Event");
		eventRepository.save(testEvent);

		// Redis 초기화
		queueEntryRedisRepository.clearAll(testEvent.getId());

		testEventListener.clear();
	}

	@Nested
	@DisplayName("좌석 목록 조회 API (/api/v1/events/{eventId}/seats)")
	class GetSeatsByEventTests {

		@Test
		@DisplayName("큐에 입장한 사용자는 좌석 목록을 정상 조회한다")
		void getSeatsByEvent_Success() throws Exception {
			// GIVEN
			seatRepository.save(Seat.createSeat(testEvent, "A1", SeatGrade.R, 100000));
			seatRepository.save(Seat.createSeat(testEvent, "A1", SeatGrade.VIP, 150000));
			seatRepository.save(Seat.createSeat(testEvent, "B2", SeatGrade.VIP, 150000));

			Long mockUser = 1L;
			queueEntryRedisRepository.moveToEnteredQueue(testEvent.getId(), mockUser);

			// WHEN & THEN
			mockMvc.perform(get("/api/v1/events/{eventId}/seats", testEvent.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("좌석 목록을 조회했습니다."))
				.andExpect(jsonPath("$.data", hasSize(3)))
				.andExpect(jsonPath("$.data[0].seatCode").value("A1"))
				.andExpect(jsonPath("$.data[1].seatCode").value("A1"))
				.andExpect(jsonPath("$.data[2].seatCode").value("B2"))
				.andDo(print());
		}

		@Test
		@DisplayName("큐에 입장하지 않은 사용자는 좌석 조회 시 400 오류가 발생한다")
		void getSeatsByEvent_NotInQueue_Fail() throws Exception {
			// GIVEN: 좌석은 있어도 큐 미입장

			mockMvc.perform(get("/api/v1/events/{eventId}/seats", testEvent.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
					.value(containsString(SeatErrorCode.NOT_IN_QUEUE.getMessage())))
				.andDo(print());
		}
	}

	@Nested
	@DisplayName("좌석 선택 API (/api/v1/events/{eventId}/seats/{seatId}/select)")
	class SelectSeatTests {

		@Test
		@DisplayName("큐에 입장한 사용자는 좌석 선택에 성공한다")
		void selectSeat_Success() throws Exception {
			// GIVEN
			Seat seat = Seat.createSeat(testEvent, "A1", SeatGrade.VIP, 150000);
			seatRepository.save(seat);

			Long mockUser = 1L;
			queueEntryRedisRepository.moveToEnteredQueue(testEvent.getId(), mockUser);

			// WHEN & THEN
			mockMvc.perform(post("/api/v1/events/{eventId}/seats/{seatId}/select",
					testEvent.getId(), seat.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{ }"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("좌석을 선택했습니다."))
				.andExpect(jsonPath("$.data.seatStatus").value("RESERVED"))
				.andDo(print());
		}

		@Test
		@DisplayName("큐 미입장 사용자는 좌석 선택 시 실패한다")
		void selectSeat_NotInQueue_Fail() throws Exception {
			Seat seat = Seat.createSeat(testEvent, "A1", SeatGrade.VIP, 150000);
			seatRepository.save(seat);

			mockMvc.perform(post("/api/v1/events/{eventId}/seats/{seatId}/select",
					testEvent.getId(), seat.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{ }"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
					.value(containsString(SeatErrorCode.NOT_IN_QUEUE.getMessage())))
				.andDo(print());
		}

		@Test
		@DisplayName("이미 RESERVED 좌석을 선택하면 실패한다")
		void selectSeat_AlreadyReserved_Fail() throws Exception {
			// GIVEN
			Seat seat = Seat.createSeat(testEvent, "A1", SeatGrade.VIP, 150000);
			seat.markAsReserved(); // 이미 예약 상태
			seatRepository.save(seat);

			Long mockUser = 1L;
			queueEntryRedisRepository.moveToEnteredQueue(testEvent.getId(), mockUser);

			// WHEN & THEN
			mockMvc.perform(post("/api/v1/events/{eventId}/seats/{seatId}/select",
					testEvent.getId(), seat.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{ }"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
					.value(containsString(SeatErrorCode.SEAT_ALREADY_RESERVED.getMessage())))
				.andDo(print());
		}
	}

	@Nested
	@DisplayName("좌석 상태 전이 검증")
	class SeatStatusTransitionTests {

		@Test
		@DisplayName("AVAILABLE → RESERVED 상태 전이가 성공하고, DB에 RESERVED로 반영된다")
		void statusTransition_AvailableToReserved_Success() throws Exception {
			// GIVEN
			Seat seat = Seat.createSeat(testEvent, "A1", SeatGrade.VIP, 150000);
			seatRepository.save(seat);

			Long mockUser = 1L;
			queueEntryRedisRepository.moveToEnteredQueue(testEvent.getId(), mockUser);

			// WHEN: 좌석 선택 API 호출
			mockMvc.perform(post("/api/v1/events/{eventId}/seats/{seatId}/select",
					testEvent.getId(), seat.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{ }"))
				.andExpect(status().isOk());

			// THEN: DB에서 다시 조회하면 RESERVED
			Seat updated = seatRepository.findById(seat.getId())
				.orElseThrow();

			assertThat(updated.getSeatStatus())
				.isEqualTo(SeatStatus.RESERVED);
		}

		@Test
		@DisplayName("이미 RESERVED 상태에서 다시 선택 요청 시 상태는 그대로 RESERVED 유지된다")
		void statusTransition_Reserved_SecondSelect_Fail_ButStatusUnchanged() throws Exception {
			// GIVEN
			Seat seat = Seat.createSeat(testEvent, "A1", SeatGrade.VIP, 150000);
			seat.markAsReserved();
			seatRepository.save(seat);

			Long mockUser = 1L;
			queueEntryRedisRepository.moveToEnteredQueue(testEvent.getId(), mockUser);

			// WHEN: 다시 선택 요청 (실패 예상)
			mockMvc.perform(post("/api/v1/events/{eventId}/seats/{seatId}/select",
					testEvent.getId(), seat.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{ }"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
					.value(containsString(SeatErrorCode.SEAT_ALREADY_RESERVED.getMessage())));

			// THEN: DB 상태는 여전히 RESERVED
			Seat updated = seatRepository.findById(seat.getId())
				.orElseThrow();
			assertThat(updated.getSeatStatus())
				.isEqualTo(SeatStatus.RESERVED);
		}
	}

	@Nested
	@DisplayName("좌석 선택 시 WebSocket용 SeatStatusMessage 이벤트 발행")
	class WebSocketEventTests {

		@Test
		@DisplayName("좌석 선택 시 SeatStatusMessage가 EventPublisher를 통해 발행된다")
		void selectSeat_PublishesSeatStatusMessage() throws Exception {
			// GIVEN
			Seat seat = Seat.createSeat(testEvent, "A1", SeatGrade.VIP, 150000);
			seatRepository.save(seat);

			Long mockUser = 1L;
			queueEntryRedisRepository.moveToEnteredQueue(testEvent.getId(), mockUser);

			// WHEN
			mockMvc.perform(post("/api/v1/events/{eventId}/seats/{seatId}/select",
					testEvent.getId(), seat.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{ }"))
				.andExpect(status().isOk());

			// THEN: EventPublisher를 통해 SeatStatusMessage가 발행되었는지 검증
			List<SeatStatusMessage> events = testEventListener.getEvents();

			assertThat(events).hasSize(1);

			SeatStatusMessage msg = events.get(0);

			assertThat(msg.eventId()).isEqualTo(testEvent.getId());
			assertThat(msg.seatId()).isEqualTo(seat.getId());
			assertThat(msg.status()).isEqualTo("RESERVED");
			assertThat(msg.grade()).isEqualTo(SeatGrade.VIP.name());
			assertThat(msg.price()).isEqualTo(150000);
		}

		@Test
		@DisplayName("좌석 선택 실패 시 SeatStatusMessage는 발행되지 않는다 (큐 미입장)")
		void selectSeat_Fail_DoesNotPublishSeatStatusMessage() throws Exception {
			// GIVEN
			Seat seat = Seat.createSeat(testEvent, "A1", SeatGrade.VIP, 150000);
			seatRepository.save(seat);

			// WHEN: 큐에 입장하지 않고 바로 선택 요청
			mockMvc.perform(post("/api/v1/events/{eventId}/seats/{seatId}/select",
					testEvent.getId(), seat.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{ }"))
				.andExpect(status().isBadRequest());

			// THEN: publishEvent가 호출되지 않았거나 SeatStatusMessage는 발행되지 않아야 함
			assertThat(testEventListener.getEvents()).isEmpty();
		}
	}
}
