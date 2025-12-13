package com.back.api.selection.controller;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.back.api.seat.dto.response.SeatStatusMessage;
import com.back.config.TestSeatStatusEventListener;
import com.back.domain.event.entity.Event;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.queue.repository.QueueEntryRedisRepository;
import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.entity.SeatGrade;
import com.back.domain.seat.entity.SeatStatus;
import com.back.domain.seat.repository.SeatRepository;
import com.back.domain.ticket.repository.TicketRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.domain.user.repository.UserRepository;
import com.back.global.error.code.SeatErrorCode;
import com.back.support.factory.EventFactory;
import com.back.support.factory.UserFactory;
import com.back.support.helper.TestAuthHelper;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("SeatSelectionController 통합 테스트")
class SeatSelectionControllerTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private EventRepository eventRepository;
	@Autowired
	private SeatRepository seatRepository;
	@Autowired
	private TicketRepository ticketRepository;
	@Autowired
	private QueueEntryRedisRepository queueEntryRedisRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private TestSeatStatusEventListener testEventListener;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private TestAuthHelper authHelper;

	private Event event;
	private User user;

	@BeforeEach
	void setUp() {
		// 이벤트 생성
		event = EventFactory.fakeEvent("테스트 선택 이벤트");
		eventRepository.save(event);

		// 테스트 유저 직접 생성
		user = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
		userRepository.save(user);

		authHelper.authenticate(user);

		// Redis 및 WebSocket 초기화
		queueEntryRedisRepository.clearAll(event.getId());
		testEventListener.clear();
	}

	// 공통: 큐 입장
	private void enterQueue() {
		queueEntryRedisRepository.moveToEnteredQueue(event.getId(), user.getId());
	}

	// 공통: 좌석 생성
	private Seat createSeat(String code, SeatGrade grade, int price) {
		Seat seat = Seat.createSeat(event, code, grade, price);
		return seatRepository.save(seat);
	}

	@Nested
	@DisplayName("좌석 선택 API")
	class SelectSeatTests {

		@Test
		@DisplayName("큐에 입장한 사용자는 좌석 선택에 성공한다")
		void selectSeat_Success() throws Exception {
			Seat seat = createSeat("A1", SeatGrade.VIP, 150000);

			enterQueue();

			mockMvc.perform(post("/api/v1/events/{eventId}/seats/{seatId}/select",
					event.getId(), seat.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("좌석을 선택했습니다."))
				.andExpect(jsonPath("$.data.seatStatus").value("RESERVED"));
		}

		@Test
		@DisplayName("큐 미입장 사용자는 좌석 선택에 실패한다")
		void selectSeat_NotInQueue_Fail() throws Exception {
			Seat seat = createSeat("A1", SeatGrade.VIP, 150000);

			mockMvc.perform(post("/api/v1/events/{eventId}/seats/{seatId}/select",
					event.getId(), seat.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
					.value(containsString(SeatErrorCode.NOT_IN_QUEUE.getMessage())));
		}

		@Test
		@DisplayName("이미 RESERVED 좌석이면 선택 실패한다")
		void selectSeat_AlreadyReserved_Fail() throws Exception {
			Seat seat = createSeat("A1", SeatGrade.VIP, 150000);
			seat.markAsReserved();
			seatRepository.save(seat);

			enterQueue();

			mockMvc.perform(post("/api/v1/events/{eventId}/seats/{seatId}/select",
					event.getId(), seat.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
					.value(containsString(SeatErrorCode.SEAT_ALREADY_RESERVED.getMessage())));
		}
	}

	@Nested
	@DisplayName("좌석 상태 전이 검증")
	class SeatStatusTransitionTests {

		@Test
		@DisplayName("AVAILABLE → RESERVED 전이 성공")
		void availableToReserved_Success() throws Exception {
			Seat seat = createSeat("A1", SeatGrade.VIP, 150000);

			enterQueue();

			mockMvc.perform(post("/api/v1/events/{eventId}/seats/{seatId}/select",
					event.getId(), seat.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andExpect(status().isOk());

			Seat updated = seatRepository.findById(seat.getId()).orElseThrow();
			assertThat(updated.getSeatStatus()).isEqualTo(SeatStatus.RESERVED);
		}

		@Test
		@DisplayName("이미 RESERVED 좌석은 상태 변화 없이 실패")
		void reservedSeat_NoChange() throws Exception {
			Seat seat = createSeat("A1", SeatGrade.VIP, 150000);
			seat.markAsReserved();
			seatRepository.save(seat);

			enterQueue();

			mockMvc.perform(post("/api/v1/events/{eventId}/seats/{seatId}/select",
					event.getId(), seat.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andExpect(status().isBadRequest());

			Seat updated = seatRepository.findById(seat.getId()).orElseThrow();
			assertThat(updated.getSeatStatus()).isEqualTo(SeatStatus.RESERVED);
		}
	}

	@Nested
	@DisplayName("좌석 선택 시 WebSocket SeatStatusMessage 발행")
	class WebSocketEventTests {

		@BeforeEach
		void clean() {
			testEventListener.clear();
		}

		@Test
		@DisplayName("좌석 선택 성공 시 SeatStatusMessage 발행")
		void publishesEvent_OnSeatSelect() throws Exception {
			Seat seat = createSeat("A1", SeatGrade.VIP, 150000);

			enterQueue();

			mockMvc.perform(post("/api/v1/events/{eventId}/seats/{seatId}/select",
					event.getId(), seat.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andExpect(status().isOk());

			List<SeatStatusMessage> events = testEventListener.getEvents();
			assertThat(events).hasSize(1);
		}

		@Test
		@DisplayName("좌석 선택 실패 시 이벤트 미발행")
		void doesNotPublishEvent_WhenFail() throws Exception {
			Seat seat = createSeat("A1", SeatGrade.VIP, 150000);

			mockMvc.perform(post("/api/v1/events/{eventId}/seats/{seatId}/select",
					event.getId(), seat.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andExpect(status().isBadRequest());

			assertThat(testEventListener.getEvents()).isEmpty();
		}
	}
}
