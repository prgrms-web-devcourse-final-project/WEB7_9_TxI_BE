package com.back.api.ticket.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.back.domain.event.entity.Event;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.queue.repository.QueueEntryRedisRepository;
import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.entity.SeatGrade;
import com.back.domain.ticket.repository.TicketRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.support.factory.EventFactory;
import com.back.support.helper.SeatHelper;
import com.back.support.helper.TestAuthHelper;
import com.back.support.helper.TicketHelper;
import com.back.support.helper.UserHelper;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("TicketController 통합 테스트")
class TicketControllerTest {
	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private TicketRepository ticketRepository;
	@Autowired
	private EventRepository eventRepository;
	@Autowired
	private TestAuthHelper testAuthHelper;
	@Autowired
	private TicketHelper ticketHelper;
	@Autowired
	private UserHelper userHelper;
	@Autowired
	private SeatHelper seatHelper;
	private Event testEvent;
	private User testUser;
	private Seat testSeat;
	@Autowired
	private QueueEntryRedisRepository queueEntryRedisRepository;

	@BeforeEach
	void setUp() {
		// 이벤트 생성
		testEvent = EventFactory.fakeEvent("테스트 이벤트");
		eventRepository.save(testEvent);

		// 좌석 1개 생성
		testSeat = seatHelper.createSeat(testEvent, "A1", SeatGrade.VIP);

		// 유저 생성 + 인증
		testUser = userHelper.createUser(UserRole.NORMAL).user();
		testAuthHelper.authenticate(testUser);

		// Redis 초기화
		queueEntryRedisRepository.clearAll(testEvent.getId());
	}

	@Test
	@DisplayName("내 티켓 조회 - 성공 (발급된 티켓이 존재할 때)")
	void getMyTickets_success() throws Exception {

		// given: 테스트 유저에게 티켓 두 개 발급
		Seat seat2 = seatHelper.createSeat(testEvent, "A2", SeatGrade.R);

		ticketHelper.createIssuedTicket(testUser, testSeat, testEvent);
		ticketHelper.createIssuedTicket(testUser, seat2, testEvent);

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/tickets/my")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("사용자의 티켓 목록 조회 성공"))
			.andExpect(jsonPath("$.data.length()").value(2))
			.andExpect(jsonPath("$.data[0].ticketStatus").value("ISSUED"));
	}

	@Test
	@DisplayName("내 티켓 조회 - 빈 배열 반환")
	void getMyTickets_empty() throws Exception {

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/tickets/my")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(0));
	}

	@Test
	@DisplayName("내 티켓 조회 - 다른 유저 티켓은 보이지 않아야 함")
	void getMyTickets_filtering() throws Exception {

		// given
		User otherUser = userHelper.createUser(UserRole.NORMAL).user();
		ticketHelper.createIssuedTicket(otherUser, testSeat, testEvent);

		// when & then: 로그인한 testUser는 티켓이 없어야 함
		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/tickets/my")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(0));
	}
}
