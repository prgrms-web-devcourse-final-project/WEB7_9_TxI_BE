package com.back.api.seat.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.back.config.TestSeatStatusEventListener;
import com.back.domain.event.entity.Event;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.queue.repository.QueueEntryRedisRepository;
import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.entity.SeatGrade;
import com.back.domain.seat.repository.SeatRepository;
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
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TestAuthHelper authHelper;

	@Autowired
	private TestSeatStatusEventListener testEventListener;

	private Event testEvent;
	private User user;

	@BeforeEach
	void setUp() {
		// 1. 이벤트 생성
		testEvent = EventFactory.fakeEvent("Test Event");
		eventRepository.save(testEvent);

		// 2. 테스트 유저 생성 & 저장
		user = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
		userRepository.save(user);

		// 3. 인증 흐름 통과시키기
		authHelper.authenticate(user);

		// 4. Redis 및 이벤트 초기화
		queueEntryRedisRepository.clearAll(testEvent.getId());
		testEventListener.clear();
	}

	@Nested
	@DisplayName("좌석 목록 조회 API (/api/v1/events/{eventId}/seats)")
	class GetSeatsByEventTests {

		@Test
		@DisplayName("큐에 입장한 사용자는 좌석 목록을 정상 조회한다")
		void getSeatsByEvent_Success() throws Exception {
			// GIVEN: 좌석 3개 생성
			seatRepository.save(Seat.createSeat(testEvent, "A1", SeatGrade.R, 100000));
			seatRepository.save(Seat.createSeat(testEvent, "A1", SeatGrade.VIP, 150000));
			seatRepository.save(Seat.createSeat(testEvent, "B2", SeatGrade.VIP, 150000));

			// ★ 실제 로그인된 user.getId()로 큐 입장 처리
			queueEntryRedisRepository.moveToEnteredQueue(testEvent.getId(), user.getId());

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
			// GIVEN: 좌석만 존재, 큐 미입장 상태

			mockMvc.perform(get("/api/v1/events/{eventId}/seats", testEvent.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
					.value(containsString(SeatErrorCode.NOT_IN_QUEUE.getMessage())))
				.andDo(print());
		}
	}
}
