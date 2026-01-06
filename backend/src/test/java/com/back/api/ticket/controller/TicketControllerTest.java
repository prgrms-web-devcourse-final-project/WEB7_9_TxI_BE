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
import org.springframework.transaction.annotation.Transactional;

import com.back.domain.event.entity.Event;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.queue.repository.QueueEntryRedisRepository;
import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.entity.SeatGrade;
import com.back.domain.store.entity.Store;
import com.back.domain.ticket.entity.Ticket;
import com.back.domain.ticket.repository.TicketRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.support.factory.EventFactory;
import com.back.support.helper.SeatHelper;
import com.back.support.helper.StoreHelper;
import com.back.support.helper.TestAuthHelper;
import com.back.support.helper.TicketHelper;
import com.back.support.helper.UserHelper;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
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
	@Autowired
	private StoreHelper storeHelper;

	private Event testEvent;
	private User testUser;
	private Seat testSeat;
	@Autowired
	private QueueEntryRedisRepository queueEntryRedisRepository;

	@BeforeEach
	void setUp() {
		Store store = storeHelper.createStore();
		// 이벤트 생성
		testEvent = EventFactory.fakeEvent(store, "테스트 이벤트");
		eventRepository.save(testEvent);

		// 좌석 1개 생성
		testSeat = seatHelper.createSeat(testEvent, "A1", SeatGrade.VIP);

		// 유저 생성 + 인증
		testUser = userHelper.createUser(UserRole.NORMAL, null).user();
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
		User otherUser = userHelper.createUser(UserRole.NORMAL, null).user();
		ticketHelper.createIssuedTicket(otherUser, testSeat, testEvent);

		// when & then: 로그인한 testUser는 티켓이 없어야 함
		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/tickets/my")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(0));
	}

	// ===== 티켓 양도 API 테스트 =====

	@Test
	@DisplayName("티켓 양도 - 성공")
	void transferTicket_success() throws Exception {

		// given: ISSUED 티켓 생성, 양도 대상 유저 생성
		Ticket ticket = ticketHelper.createIssuedTicket(testUser, testSeat, testEvent);
		User targetUser = userHelper.createUser(UserRole.NORMAL, null).user();

		String requestBody = """
			{
				"targetNickname": "%s"
			}
			""".formatted(targetUser.getNickname());

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/tickets/my/{ticketId}/transfer", ticket.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isNoContent());

		// 양도 후 소유자 변경 확인
		Ticket updatedTicket = ticketRepository.findById(ticket.getId()).get();
		org.assertj.core.api.Assertions.assertThat(updatedTicket.getOwner().getId()).isEqualTo(targetUser.getId());
	}

	@Test
	@DisplayName("티켓 양도 - 실패 (빈 닉네임으로 요청)")
	void transferTicket_emptyNickname_fail() throws Exception {

		// given: ISSUED 티켓 생성
		Ticket ticket = ticketHelper.createIssuedTicket(testUser, testSeat, testEvent);

		String requestBody = """
			{
				"targetNickname": ""
			}
			""";

		// when & then: validation 실패
		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/tickets/my/{ticketId}/transfer", ticket.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("티켓 양도 - 실패 (존재하지 않는 대상 유저)")
	void transferTicket_targetNotFound_fail() throws Exception {

		// given: ISSUED 티켓 생성
		Ticket ticket = ticketHelper.createIssuedTicket(testUser, testSeat, testEvent);

		String requestBody = """
			{
				"targetNickname": "nonExistentUser123"
			}
			""";

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/tickets/my/{ticketId}/transfer", ticket.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("양도 대상 유저를 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("티켓 양도 - 실패 (다른 사용자의 티켓 양도 시도)")
	void transferTicket_unauthorized_fail() throws Exception {

		// given: 다른 유저의 ISSUED 티켓 생성
		User otherUser = userHelper.createUser(UserRole.NORMAL, null).user();
		Ticket otherTicket = ticketHelper.createIssuedTicket(otherUser, testSeat, testEvent);
		User targetUser = userHelper.createUser(UserRole.NORMAL, null).user();

		String requestBody = """
			{
				"targetNickname": "%s"
			}
			""".formatted(targetUser.getNickname());

		// when & then: 로그인한 testUser가 다른 유저의 티켓 양도 시도
		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/tickets/my/{ticketId}/transfer", otherTicket.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("티켓에 대한 접근 권한이 없습니다."));
	}

	@Test
	@DisplayName("티켓 양도 - 실패 (자기 자신에게 양도)")
	void transferTicket_toSelf_fail() throws Exception {

		// given: ISSUED 티켓 생성
		Ticket ticket = ticketHelper.createIssuedTicket(testUser, testSeat, testEvent);

		String requestBody = """
			{
				"targetNickname": "%s"
			}
			""".formatted(testUser.getNickname());

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/tickets/my/{ticketId}/transfer", ticket.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("자기자신에게 양도는 불가합니다."));
	}

	@Test
	@DisplayName("티켓 양도 - 실패 (발급되지 않은 티켓)")
	void transferTicket_notIssued_fail() throws Exception {

		// given: DRAFT 티켓 생성
		Ticket draftTicket = ticketHelper.createDraftTicket(testUser, testSeat, testEvent);
		User targetUser = userHelper.createUser(UserRole.NORMAL, null).user();

		String requestBody = """
			{
				"targetNickname": "%s"
			}
			""".formatted(targetUser.getNickname());

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/tickets/my/{ticketId}/transfer", draftTicket.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("아직 발급되지 않은 티켓입니다. 양도가 불가합니다."));
	}
}
