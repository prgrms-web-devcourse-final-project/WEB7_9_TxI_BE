package com.back.api.ticket.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.back.config.TestRedisConfig;
import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventCategory;
import com.back.domain.event.entity.EventStatus;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.entity.SeatGrade;
import com.back.domain.store.entity.Store;
import com.back.domain.ticket.entity.Ticket;
import com.back.domain.ticket.entity.TicketStatus;
import com.back.domain.ticket.repository.TicketRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.support.data.TestUser;
import com.back.support.helper.SeatHelper;
import com.back.support.helper.StoreHelper;
import com.back.support.helper.TestAuthHelper;
import com.back.support.helper.TicketHelper;
import com.back.support.helper.UserHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
@Import(TestRedisConfig.class)
@DisplayName("QrController 통합 테스트")
public class QrControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TestAuthHelper testAuthHelper;

	@Autowired
	private UserHelper userHelper;

	@Autowired
	private SeatHelper seatHelper;

	@Autowired
	private TicketHelper ticketHelper;

	@Autowired
	private StoreHelper storeHelper;

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private StringRedisTemplate redisTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	private Store store;
	private User testUser;
	private Event testEvent;
	private Seat testSeat;
	private Ticket testTicket;

	@BeforeEach
	void setUp() {
		store = storeHelper.createStore();
		TestUser user = userHelper.createUser(UserRole.NORMAL, null);
		testUser = user.user();
		testAuthHelper.authenticate(testUser);

		LocalDateTime now = LocalDateTime.now();
		testEvent = Event.builder()
			.title("QR 테스트 이벤트")
			.category(EventCategory.CONCERT)
			.description("QR 테스트용 이벤트입니다")
			.place("테스트 장소")
			.imageUrl("https://example.com/image.jpg")
			.minPrice(10000)
			.maxPrice(50000)
			.preOpenAt(now.minusDays(10))
			.preCloseAt(now.minusDays(8))
			.ticketOpenAt(now.minusDays(5))
			.ticketCloseAt(now.plusDays(1))
			.eventDate(now.minusHours(1)) // 이벤트가 이미 시작됨
			.maxTicketAmount(1000)
			.status(EventStatus.OPEN)
			.store(store)
			.build();
		eventRepository.save(testEvent);

		// 좌석 생성
		testSeat = seatHelper.createSeat(testEvent, "A1", SeatGrade.VIP);

		// ISSUED 티켓 생성
		testTicket = ticketHelper.createIssuedTicket(testUser, testSeat, testEvent);
	}


	@Nested
	@DisplayName("QR 토큰 발급 API (POST /api/v1/tickets/{ticketId}/qr-token)")
	class GenerateQrToken {

		@Test
		@DisplayName("ISSUED 상태 티켓으로 QR 토큰 발급 성공")
		void generateQrToken_Success() throws Exception {
			// when & then
			mockMvc.perform(post("/api/v1/tickets/{ticketId}/qr-token", testTicket.getId())
					.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("QR 토큰 발급 성공"))
				.andExpect(jsonPath("$.data.qrToken").isNotEmpty())
				.andExpect(jsonPath("$.data.expirationSecond").value(60))
				.andExpect(jsonPath("$.data.refreshIntervalSecond").value(30))
				.andExpect(jsonPath("$.data.qrUrl").isNotEmpty());
		}

		@Test
		@DisplayName("이벤트 시작 전에는 QR 토큰 발급 불가")
		void generateQrToken_Fail_EventNotStarted() throws Exception {
			// given

			LocalDateTime now = LocalDateTime.now();
			Event futureEvent =Event.builder()
				.title("QR 테스트 이벤트")
				.category(EventCategory.CONCERT)
				.description("QR 테스트용 이벤트입니다")
				.place("테스트 장소")
				.imageUrl("https://example.com/image.jpg")
				.minPrice(10000)
				.maxPrice(50000)
				.preOpenAt(now.minusDays(10))
				.preCloseAt(now.minusDays(8))
				.ticketOpenAt(now.minusDays(5))
				.ticketCloseAt(now.plusDays(1))
				.eventDate(now.plusDays(10))
				.maxTicketAmount(1000)
				.status(EventStatus.OPEN)
				.store(store)
				.build();

			eventRepository.save(futureEvent);

			Seat futureSeat = seatHelper.createSeat(futureEvent, "B1");
			Ticket futureTicket = ticketHelper.createIssuedTicket(testUser, futureSeat, futureEvent);

			// when & then
			mockMvc.perform(post("/api/v1/tickets/{ticketId}/qr-token", futureTicket.getId())
					.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("ISSUED 상태가 아닌 티켓은 QR 발급 불가 - USED")
		void generateQrToken_Fail_UsedTicket() throws Exception {
			// given
			testTicket.markAsUsed();
			ticketRepository.saveAndFlush(testTicket);

			// when & then
			mockMvc.perform(post("/api/v1/tickets/{ticketId}/qr-token", testTicket.getId())
					.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("ISSUED 상태가 아닌 티켓은 QR 발급 불가 - DRAFT")
		void generateQrToken_Fail_DraftTicket() throws Exception {
			// given
			Seat seat2 = seatHelper.createSeat(testEvent, "B1");
			Ticket draftTicket = ticketHelper.createDraftTicket(testUser, seat2, testEvent);

			// when & then
			mockMvc.perform(post("/api/v1/tickets/{ticketId}/qr-token", draftTicket.getId())
					.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("다른 사용자의 티켓으로는 QR 발급 불가")
		void generateQrToken_Fail_UnauthorizedAccess() throws Exception {
			// given
			TestUser otherUser = userHelper.createUser(UserRole.NORMAL, null);
			testAuthHelper.authenticate(otherUser.user());

			// when & then
			mockMvc.perform(post("/api/v1/tickets/{ticketId}/qr-token", testTicket.getId())
					.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("존재하지 않는 티켓으로 QR 발급 시도 시 404")
		void generateQrToken_Fail_TicketNotFound() throws Exception {
			// when & then
			mockMvc.perform(post("/api/v1/tickets/{ticketId}/qr-token", 999L)
					.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("QR 코드 검증 API (POST /api/v1/tickets/entry/verify)")
	class ValidateQrCode {

		@Test
		@DisplayName("유효한 QR 토큰으로 입장 검증 성공")
		void validateQrCode_Success() throws Exception {
			// given
			String qrToken = generateValidQrToken();

			// when & then
			mockMvc.perform(post("/api/v1/tickets/entry/verify")
					.param("token", qrToken)
					.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("QR 코드 검증 & 사용 처리 성공"))
				.andExpect(jsonPath("$.data.isValid").value(true))
				.andExpect(jsonPath("$.data.message").value("QR 코드가 유효합니다."))
				.andExpect(jsonPath("$.data.ticketId").value(testTicket.getId()))
				.andExpect(jsonPath("$.data.eventId").value(testEvent.getId()))
				.andExpect(jsonPath("$.data.eventTitle").value(testEvent.getTitle()))
				.andExpect(jsonPath("$.data.seatCode").value("A1"))
				.andExpect(jsonPath("$.data.ownerNickname").value(testUser.getNickname()))
				.andExpect(jsonPath("$.data.eventDate").isNotEmpty())
				.andExpect(jsonPath("$.data.qrIssuedAt").isNotEmpty());

			// Redis 입장 기록 확인
			String redisKey = "entry:ticket:" + testTicket.getId();
			String entryRecord = redisTemplate.opsForValue().get(redisKey);
			assertThat(entryRecord).isNotNull();

			// 티켓 상태 변경 확인
			Ticket updatedTicket = ticketRepository.findById(testTicket.getId()).orElseThrow();
			assertThat(updatedTicket.getTicketStatus()).isEqualTo(TicketStatus.USED);
		}

		@Test
		@DisplayName("좌석이 없는 티켓도 입장 처리 가능")
		void validateQrCode_Success_NoSeat() throws Exception {
			// given
			Ticket noSeatTicket = ticketRepository.save(
				Ticket.builder()
					.owner(testUser)
					.seat(null) // 좌석 없음
					.event(testEvent)
					.ticketStatus(TicketStatus.ISSUED)
					.build()
			);

			String qrToken = generateValidQrTokenForTicket(noSeatTicket.getId());

			// when & then
			mockMvc.perform(post("/api/v1/tickets/entry/verify")
					.param("token", qrToken)
					.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.isValid").value(true))
				.andExpect(jsonPath("$.data.seatCode").isEmpty());
		}

		@Test
		@DisplayName("이미 입장 처리된 티켓은 재입장 불가")
		void validateQrCode_Fail_AlreadyEntered() throws Exception {
			// given
			String qrToken = generateValidQrToken();

			// 첫 번째 입장 처리
			mockMvc.perform(post("/api/v1/tickets/entry/verify")
					.param("token", qrToken)
					.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.isValid").value(true));

			testTicket.changeStatus(TicketStatus.ISSUED);
			ticketRepository.saveAndFlush(testTicket);

			// 새로운 QR 토큰 발급
			String newQrToken = generateValidQrToken();

			// when & then - 두 번째 입장 시도
			mockMvc.perform(post("/api/v1/tickets/entry/verify")
					.param("token", newQrToken)
					.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.isValid").value(false))
				.andExpect(jsonPath("$.data.message").value("이미 입장 처리된 티켓입니다."));
		}

		@Test
		@DisplayName("유효하지 않은 QR 토큰은 검증 실패")
		void validateQrCode_Fail_InvalidToken() throws Exception {
			// given
			String invalidToken = "invalid.token.here";

			// when & then
			mockMvc.perform(post("/api/v1/tickets/entry/verify")
					.param("token", invalidToken)
					.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("USED 상태 티켓의 QR은 검증 실패")
		void validateQrCode_Fail_UsedTicket() throws Exception {
			// given
			String qrToken = generateValidQrToken();

			// 티켓 상태를 USED로 변경
			testTicket.markAsUsed();
			ticketRepository.saveAndFlush(testTicket);

			// when & then
			mockMvc.perform(post("/api/v1/tickets/entry/verify")
					.param("token", qrToken)
					.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.isValid").value(false))
				.andExpect(jsonPath("$.data.message").value("유효하지 않은 티켓 상태입니다."));
		}

		private String generateValidQrToken() throws Exception {
			return generateValidQrTokenForTicket(testTicket.getId());
		}

		private String generateValidQrTokenForTicket(Long ticketId) throws Exception {
			MvcResult result = mockMvc.perform(post("/api/v1/tickets/{ticketId}/qr-token", ticketId)
					.contentType(MediaType.APPLICATION_JSON))
				.andReturn();

			String responseBody = result.getResponse().getContentAsString();
			JsonNode jsonNode = objectMapper.readTree(responseBody);
			return jsonNode.get("data").get("qrToken").asText();
		}
	}

}
