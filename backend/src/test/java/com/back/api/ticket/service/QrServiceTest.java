package com.back.api.ticket.service;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.ticket.dto.response.QrTokenResponse;
import com.back.api.ticket.dto.response.QrValidationResponse;
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
import com.back.global.error.code.TicketErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.utils.JwtUtil;
import com.back.support.data.TestUser;
import com.back.support.helper.SeatHelper;
import com.back.support.helper.StoreHelper;
import com.back.support.helper.TicketHelper;
import com.back.support.helper.UserHelper;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
@Import(TestRedisConfig.class)
@DisplayName("QrService 통합 테스트")
public class QrServiceTest {

	@Autowired
	private QrService qrService;

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

	@Value("${custom.jwt.qr-secret}")
	private String qrSecret;

	private Store store;
	private User testUser;
	private Event testEvent;
	private Seat testSeat;
	private Ticket testTicket;

	@BeforeEach
	void setUp() {
		store = storeHelper.createStore();

		// 유저 생성
		TestUser user = userHelper.createUser(UserRole.NORMAL, null);
		testUser = user.user();

		// 이벤트 생성 (현재 시점으로 설정 - QR 발급 가능)
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

	@AfterEach
	void tearDown() {
		// Redis 초기화
		String redisKey = "entry:ticket:" + testTicket.getId();
		redisTemplate.delete(redisKey);
	}

	@Nested
	@DisplayName("QR 토큰 발급")
	class GenerateQrTokenResponse {

		@Test
		@DisplayName("ISSUED 상태 티켓으로 QR 토큰 발급 성공")
		void generateQrTokenSuccess() {
			// when
			QrTokenResponse response = qrService.generateQrTokenResponse(testTicket.getId(), testUser.getId());

			// then
			assertThat(response).isNotNull();
			assertThat(response.qrToken()).isNotEmpty();
			assertThat(response.expirationSecond()).isEqualTo(60);
			assertThat(response.refreshIntervalSecond()).isEqualTo(30);
			assertThat(response.qrUrl()).contains("/tickets/verify?token=");

			// JWT 토큰 검증
			Map<String, Object> payload = JwtUtil.payloadOrNull(response.qrToken(), qrSecret);
			assertThat(payload).isNotNull();
			assertThat(payload.get("ticketId")).isEqualTo(testTicket.getId().intValue());
			assertThat(payload.get("eventId")).isEqualTo(testEvent.getId().intValue());
			assertThat(payload.get("userId")).isEqualTo(testUser.getId().intValue());
			assertThat(payload.get("iat")).isNotNull();
		}

		@Test
		@DisplayName("QR URL 형식이 올바르게 생성됨")
		void generateQrTokenWithCorrectUrlFormat() {
			// when
			QrTokenResponse response = qrService.generateQrTokenResponse(testTicket.getId(), testUser.getId());

			// then
			assertThat(response.qrUrl()).startsWith("http");
			assertThat(response.qrUrl()).contains("/tickets/verify?token=");
			assertThat(response.qrUrl()).contains(response.qrToken());
		}

		@Test
		@DisplayName("ISSUED 상태가 아닌 티켓은 QR 발급 불가 - USED")
		void generateQrTokenFailWhenUsedStatus() {
			// given
			testTicket.markAsUsed();
			ticketRepository.saveAndFlush(testTicket);

			// when & then
			assertThatThrownBy(() -> qrService.generateQrTokenResponse(testTicket.getId(), testUser.getId()))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", TicketErrorCode.INVALID_TICKET_STATE);
		}

		@Test
		@DisplayName("ISSUED 상태가 아닌 티켓은 QR 발급 불가 - DRAFT")
		void generateQrTokenFailWhenDraftStatus() {
			// given
			Seat seat2 = seatHelper.createSeat(testEvent, "B1");
			Ticket draftTicket = ticketHelper.createDraftTicket(testUser, seat2, testEvent);

			// when & then
			assertThatThrownBy(() -> qrService.generateQrTokenResponse(draftTicket.getId(), testUser.getId()))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", TicketErrorCode.INVALID_TICKET_STATE);
		}

		@Test
		@DisplayName("ISSUED 상태가 아닌 티켓은 QR 발급 불가 - CANCELLED")
		void generateQrTokenFailWhenCancelledStatus() {
			// given
			testTicket.changeStatus(TicketStatus.CANCELLED);
			ticketRepository.saveAndFlush(testTicket);

			// when & then
			assertThatThrownBy(() -> qrService.generateQrTokenResponse(testTicket.getId(), testUser.getId()))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", TicketErrorCode.INVALID_TICKET_STATE);
		}

		@Test
		@DisplayName("ISSUED 상태가 아닌 티켓은 QR 발급 불가 - FAILED")
		void generateQrTokenFailWhenFailedStatus() {
			// given
			Seat seat2 = seatHelper.createSeat(testEvent, "C1");
			Ticket failedTicket = ticketHelper.createFailedTicket(testUser, seat2, testEvent);

			// when & then
			assertThatThrownBy(() -> qrService.generateQrTokenResponse(failedTicket.getId(), testUser.getId()))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", TicketErrorCode.INVALID_TICKET_STATE);
		}

		@Test
		@DisplayName("이벤트 시작 전에는 QR 발급 불가")
		void generateQrTokenFailWhenEventNotStarted() {
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

			Seat futureSeat = seatHelper.createSeat(futureEvent, "Z1");
			Ticket futureTicket = ticketHelper.createIssuedTicket(testUser, futureSeat, futureEvent);

			// when & then
			assertThatThrownBy(() -> qrService.generateQrTokenResponse(futureTicket.getId(), testUser.getId()))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", TicketErrorCode.EVENT_NOT_STARTED);
		}

		@Test
		@DisplayName("다른 사용자의 티켓으로는 QR 발급 불가")
		void generateQrTokenFailWhenUnauthorizedUser() {
			// given
			TestUser otherUser = userHelper.createUser(UserRole.NORMAL, null);

			// when & then
			assertThatThrownBy(() -> qrService.generateQrTokenResponse(testTicket.getId(), otherUser.user().getId()))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", TicketErrorCode.UNAUTHORIZED_TICKET_ACCESS);
		}

		@Test
		@DisplayName("동일 티켓으로 여러 번 QR 발급 가능 (토큰은 매번 다름)")
		void generateQrTokenMultipleTimes() throws InterruptedException {
			// when
			QrTokenResponse response1 = qrService.generateQrTokenResponse(testTicket.getId(), testUser.getId());
			Thread.sleep(1000); // 1초 대기 (iat 값이 다르도록)
			QrTokenResponse response2 = qrService.generateQrTokenResponse(testTicket.getId(), testUser.getId());

			// then
			assertThat(response1.qrToken()).isNotEqualTo(response2.qrToken());
		}
	}

	@Nested
	@DisplayName("QR 입장 검증 및 처리")
	class ValidateAndProcessEntry {

		@Test
		@DisplayName("유효한 QR 토큰으로 입장 처리 성공")
		void validateAndProcessEntrySuccess() {
			// given
			String qrToken = generateValidQrToken();

			// when
			QrValidationResponse response = qrService.validateAndProcessEntry(qrToken);

			// then
			assertThat(response.isValid()).isTrue();
			assertThat(response.message()).isEqualTo("QR 코드가 유효합니다.");
			assertThat(response.ticketId()).isEqualTo(testTicket.getId());
			assertThat(response.eventId()).isEqualTo(testEvent.getId());
			assertThat(response.eventTitle()).isEqualTo(testEvent.getTitle());
			assertThat(response.seatCode()).isEqualTo("A1");
			assertThat(response.ownerNickname()).isEqualTo(testUser.getNickname());
			assertThat(response.eventDate()).isNotNull();
			assertThat(response.qrIssuedAt()).isNotNull();

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
		void validateAndProcessEntryWithNoSeat() {
			// given
			Ticket noSeatTicket = ticketRepository.save(
				Ticket.builder()
					.owner(testUser)
					.seat(null) // 좌석 없음
					.event(testEvent)
					.ticketStatus(TicketStatus.ISSUED)
					.build()
			);

			QrTokenResponse tokenResponse = qrService.generateQrTokenResponse(noSeatTicket.getId(), testUser.getId());

			// when
			QrValidationResponse response = qrService.validateAndProcessEntry(tokenResponse.qrToken());

			// then
			assertThat(response.isValid()).isTrue();
			assertThat(response.seatCode()).isNull();
		}

		@Test
		@DisplayName("이미 입장 처리된 티켓은 재입장 불가")
		void validateAndProcessEntryFailWhenAlreadyEntered() {
			// given
			String qrToken = generateValidQrToken();

			// 첫 번째 입장 처리
			qrService.validateAndProcessEntry(qrToken);

			// 티켓 상태를 ISSUED로 복원하고 새로운 QR 발급
			testTicket.changeStatus(TicketStatus.ISSUED);
			ticketRepository.saveAndFlush(testTicket);

			String newQrToken = generateValidQrToken();

			// when
			QrValidationResponse response = qrService.validateAndProcessEntry(newQrToken);

			// then
			assertThat(response.isValid()).isFalse();
			assertThat(response.message()).isEqualTo("이미 입장 처리된 티켓입니다.");
		}

		@Test
		@DisplayName("유효하지 않은 티켓 상태면 입장 불가 - USED")
		void validateAndProcessEntryFailWhenUsedTicket() {
			// given
			String qrToken = generateValidQrToken();

			// 티켓 상태를 USED로 변경
			testTicket.markAsUsed();
			ticketRepository.saveAndFlush(testTicket);

			// when
			QrValidationResponse response = qrService.validateAndProcessEntry(qrToken);

			// then
			assertThat(response.isValid()).isFalse();
			assertThat(response.message()).isEqualTo("유효하지 않은 티켓 상태입니다.");
		}

		@Test
		@DisplayName("유효하지 않은 티켓 상태면 입장 불가 - CANCELLED")
		void validateAndProcessEntryFailWhenCancelledTicket() {
			// given
			String qrToken = generateValidQrToken();

			// 티켓 상태를 CANCELLED로 변경
			testTicket.changeStatus(TicketStatus.CANCELLED);
			ticketRepository.saveAndFlush(testTicket);

			// when
			QrValidationResponse response = qrService.validateAndProcessEntry(qrToken);

			// then
			assertThat(response.isValid()).isFalse();
			assertThat(response.message()).isEqualTo("유효하지 않은 티켓 상태입니다.");
		}

		@Test
		@DisplayName("유효하지 않은 티켓 상태면 입장 불가 - DRAFT")
		void validateAndProcessEntryFailWhenDraftTicket() {
			// given
			String qrToken = generateValidQrToken();

			// 티켓 상태를 DRAFT로 변경
			testTicket.changeStatus(TicketStatus.DRAFT);
			ticketRepository.saveAndFlush(testTicket);

			// when
			QrValidationResponse response = qrService.validateAndProcessEntry(qrToken);

			// then
			assertThat(response.isValid()).isFalse();
			assertThat(response.message()).isEqualTo("유효하지 않은 티켓 상태입니다.");
		}

		@Test
		@DisplayName("유효하지 않은 QR 토큰은 검증 실패")
		void validateAndProcessEntryFailWhenInvalidToken() {
			// given
			String invalidToken = "invalid.token.here";

			// when & then
			assertThatThrownBy(() -> qrService.validateAndProcessEntry(invalidToken))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", TicketErrorCode.INVALID_TICKET_QR_TOKEN);
		}

		@Test
		@DisplayName("빈 토큰으로 검증 시도 시 예외 발생")
		void validateAndProcessEntryFailWhenEmptyToken() {
			// given
			String emptyToken = "";

			// when & then
			assertThatThrownBy(() -> qrService.validateAndProcessEntry(emptyToken))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", TicketErrorCode.INVALID_TICKET_QR_TOKEN);
		}

		// Helper methods
		private String generateValidQrToken() {
			QrTokenResponse response = qrService.generateQrTokenResponse(testTicket.getId(), testUser.getId());
			return response.qrToken();
		}

		private String generateExpiredQrToken() {
			// 70초 전 시간으로 토큰 생성 (만료 시간 60초 초과)
			long expiredIat = Instant.now().getEpochSecond() - 70L;

			Map<String, Object> claims = new HashMap<>();
			claims.put("ticketId", testTicket.getId());
			claims.put("eventId", testEvent.getId());
			claims.put("userId", testUser.getId());
			claims.put("iat", expiredIat);

			return JwtUtil.sign(qrSecret, 60L, claims);
		}
	}

	@Nested
	@DisplayName("Redis 입장 기록 관리")
	class EntryRecordManagement {

		@Test
		@DisplayName("입장 기록이 Redis에 24시간 TTL로 저장됨")
		void entryRecordStoredWithTTL() {
			// given
			String qrToken = generateValidQrToken();

			// when
			qrService.validateAndProcessEntry(qrToken);

			// then
			String redisKey = "entry:ticket:" + testTicket.getId();
			String entryRecord = redisTemplate.opsForValue().get(redisKey);

			assertThat(entryRecord).isNotNull();

			// TTL 확인 (약 24시간 = 86400초)
			Long ttl = redisTemplate.getExpire(redisKey);
			assertThat(ttl).isGreaterThan(86000L); // 약간의 오차 허용
			assertThat(ttl).isLessThanOrEqualTo(86400L);
		}

		@Test
		@DisplayName("Redis 입장 기록이 존재하면 티켓 상태와 무관하게 재입장 불가")
		void entryRecordPreventsReentry() {
			// given
			String qrToken = generateValidQrToken();
			qrService.validateAndProcessEntry(qrToken);

			// Redis에 직접 입장 기록이 있는지 확인
			String redisKey = "entry:ticket:" + testTicket.getId();
			assertThat(redisTemplate.opsForValue().get(redisKey)).isNotNull();

			// 티켓 상태를 다시 ISSUED로 변경하고 재시도
			testTicket.changeStatus(TicketStatus.ISSUED);
			ticketRepository.saveAndFlush(testTicket);

			String newQrToken = generateValidQrToken();

			// when
			QrValidationResponse response = qrService.validateAndProcessEntry(newQrToken);

			// then
			assertThat(response.isValid()).isFalse();
			assertThat(response.message()).isEqualTo("이미 입장 처리된 티켓입니다.");
		}

		@Test
		@DisplayName("서로 다른 티켓의 입장 기록은 독립적으로 관리됨")
		void separateEntryRecordsForDifferentTickets() {
			// given
			Seat seat2 = seatHelper.createSeat(testEvent, "B1");
			Ticket ticket2 = ticketHelper.createIssuedTicket(testUser, seat2, testEvent);

			String qrToken1 = generateValidQrToken();
			QrTokenResponse tokenResponse2 = qrService.generateQrTokenResponse(ticket2.getId(), testUser.getId());

			// when
			qrService.validateAndProcessEntry(qrToken1);

			// then
			String redisKey1 = "entry:ticket:" + testTicket.getId();
			String redisKey2 = "entry:ticket:" + ticket2.getId();

			assertThat(redisTemplate.opsForValue().get(redisKey1)).isNotNull();
			assertThat(redisTemplate.opsForValue().get(redisKey2)).isNull();

			// ticket2는 여전히 입장 가능
			QrValidationResponse response2 = qrService.validateAndProcessEntry(tokenResponse2.qrToken());
			assertThat(response2.isValid()).isTrue();
		}

		private String generateValidQrToken() {
			QrTokenResponse response = qrService.generateQrTokenResponse(testTicket.getId(), testUser.getId());
			return response.qrToken();
		}
	}
}
