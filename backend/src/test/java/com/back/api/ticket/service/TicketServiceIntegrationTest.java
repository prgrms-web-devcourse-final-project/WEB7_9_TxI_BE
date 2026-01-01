package com.back.api.ticket.service;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.ticket.dto.response.TicketResponse;
import com.back.domain.event.entity.Event;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.entity.SeatGrade;
import com.back.domain.seat.entity.SeatStatus;
import com.back.domain.seat.repository.SeatRepository;
import com.back.domain.store.entity.Store;
import com.back.domain.ticket.entity.Ticket;
import com.back.domain.ticket.entity.TicketStatus;
import com.back.domain.ticket.repository.TicketRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.global.error.exception.ErrorException;
import com.back.support.factory.EventFactory;
import com.back.support.helper.SeatHelper;
import com.back.support.helper.StoreHelper;
import com.back.support.helper.TicketHelper;
import com.back.support.helper.UserHelper;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
@DisplayName("TicketService 통합 테스트")
class TicketServiceIntegrationTest {

	@Autowired
	private TicketService ticketService;
	@Autowired
	private TicketRepository ticketRepository;
	@Autowired
	private UserHelper userHelper;
	@Autowired
	private EventRepository eventRepository;
	@Autowired
	private SeatHelper seatHelper;
	@Autowired
	private TicketHelper ticketHelper;
	@Autowired
	private StoreHelper storeHelper;
	@Autowired
	private SeatRepository seatRepository;

	private User user;
	private Event event;
	private Seat seat;

	@BeforeEach
	void setUp() {
		Store store = storeHelper.createStore();
		user = userHelper.createUser(UserRole.NORMAL, null).user();
		event = eventRepository.save(EventFactory.fakeEvent(store, "통합 테스트 이벤트"));
		seat = seatHelper.createSeat(event, "A1", SeatGrade.VIP);
	}

	@Test
	@DisplayName("Draft Ticket 조회 또는 생성 - 신규 생성")
	void getOrCreateDraft_create_success() {

		Ticket draft = ticketService.getOrCreateDraft(event.getId(), user.getId());

		assertThat(draft.getTicketStatus()).isEqualTo(TicketStatus.DRAFT);
		assertThat(draft.getOwner().getId()).isEqualTo(user.getId());
		assertThat(draft.getEvent().getId()).isEqualTo(event.getId());
		assertThat(draft.getSeat()).isNull();  // 좌석 없이 생성됨
	}

	@Test
	@DisplayName("Draft Ticket 조회 또는 생성 - 기존 티켓 재사용")
	void getOrCreateDraft_reuse_success() {

		// 첫 번째 호출: 생성
		Ticket firstCall = ticketService.getOrCreateDraft(event.getId(), user.getId());
		Long firstId = firstCall.getId();

		// 두 번째 호출: 재사용 (새로 생성하지 않음)
		Ticket secondCall = ticketService.getOrCreateDraft(event.getId(), user.getId());

		assertThat(secondCall.getId()).isEqualTo(firstId);  // 같은 티켓 반환

		// 해당 이벤트/유저의 Draft Ticket이 1개만 존재하는지 검증
		long draftCount = ticketRepository.findByOwnerId(user.getId()).stream()
			.filter(t -> t.getEvent().getId().equals(event.getId()))
			.filter(t -> t.getTicketStatus() == TicketStatus.DRAFT)
			.count();
		assertThat(draftCount).isEqualTo(1);
	}

	@Test
	@DisplayName("Draft Ticket에 좌석 할당 - 성공")
	void assignSeat_success() {

		Ticket draft = ticketService.getOrCreateDraft(event.getId(), user.getId());
		draft.assignSeat(seat);
		ticketRepository.save(draft);

		Ticket found = ticketRepository.findById(draft.getId()).get();
		assertThat(found.getSeat().getId()).isEqualTo(seat.getId());
	}

	@Test
	@DisplayName("Draft Ticket 조회 - 성공")
	void getDraftTicket_success() {

		Ticket created = ticketService.getOrCreateDraft(event.getId(), user.getId());
		created.assignSeat(seat);
		ticketRepository.save(created);

		Ticket draft = ticketService.getDraftTicket(seat.getEvent().getId(), seat.getId(), user.getId());

		assertThat(draft.getId()).isEqualTo(created.getId());
		assertThat(draft.getTicketStatus()).isEqualTo(TicketStatus.DRAFT);
	}

	@Test
	@DisplayName("결제 성공 처리 → DRAFT → PAID → ISSUED + Seat SOLD")
	void confirmPayment_success() {

		// 1. Draft 생성 및 좌석 할당
		Ticket draft = ticketService.getOrCreateDraft(event.getId(), user.getId());
		draft.assignSeat(seat);
		ticketRepository.save(draft);

		// 2. 좌석 RESERVED 필요
		seat.markAsReserved();
		seatRepository.save(seat);

		// 3. 결제 성공 처리
		Ticket issued = ticketService.confirmPayment(draft.getId(), user.getId());

		assertThat(issued.getTicketStatus()).isEqualTo(TicketStatus.ISSUED);
		assertThat(issued.getIssuedAt()).isNotNull();

		// 좌석 SOLD 검증
		Seat updatedSeat = seatRepository.findById(seat.getId()).get();
		assertThat(updatedSeat.getSeatStatus()).isEqualTo(SeatStatus.SOLD);
	}

	@Test
	@DisplayName("결제 실패 처리 → DRAFT → FAILED + Seat AVAILABLE")
	void failPayment_success() {

		// 1. Draft 생성 및 좌석 할당
		Ticket draft = ticketService.getOrCreateDraft(event.getId(), user.getId());
		draft.assignSeat(seat);
		ticketRepository.save(draft);

		// 2. 좌석을 RESERVED 상태로 만듦
		seat.markAsReserved();
		seatRepository.save(seat);

		// 3. 결제 실패 처리
		ticketService.failPayment(draft.getId());

		Ticket failed = ticketRepository.findById(draft.getId()).get();
		assertThat(failed.getTicketStatus()).isEqualTo(TicketStatus.FAILED);

		Seat updatedSeat = seatRepository.findById(seat.getId()).get();
		assertThat(updatedSeat.getSeatStatus()).isEqualTo(SeatStatus.AVAILABLE);
	}

	@Test
	@DisplayName("내 티켓 조회 - 성공")
	void getMyTickets_success() {

		// 좌석 2개 생성
		Seat seat2 = seatHelper.createSeat(event, "A2", SeatGrade.R);

		// helper 사용하여 issued 티켓 생성
		ticketHelper.createIssuedTicket(user, seat, event);
		ticketHelper.createIssuedTicket(user, seat2, event);

		List<TicketResponse> myTickets = ticketService.getMyTickets(user.getId());

		assertThat(myTickets).hasSize(2);
	}

	@Test
	@DisplayName("티켓 상세 조회 - 성공")
	void getTicketDetail_success() {

		Ticket saved = ticketHelper.createIssuedTicket(user, seat, event);

		Ticket found = ticketService.getTicketDetail(saved.getId(), user.getId());

		assertThat(found.getId()).isEqualTo(saved.getId());
	}

	// ===== 인증/인가 실패 테스트 =====

	@Test
	@DisplayName("다른 사용자의 티켓으로 결제 확정 시도 - 실패")
	void confirmPayment_unauthorizedUser_fail() {

		User otherUser = userHelper.createUser(UserRole.NORMAL, null).user();

		Ticket draft = ticketService.getOrCreateDraft(event.getId(), user.getId());
		draft.assignSeat(seat);
		ticketRepository.save(draft);

		seat.markAsReserved();
		seatRepository.save(seat);

		assertThatThrownBy(() ->
			ticketService.confirmPayment(draft.getId(), otherUser.getId())
		)
			.isInstanceOf(ErrorException.class)
			.hasMessageContaining("티켓에 대한 접근 권한이 없습니다");
	}

	@Test
	@DisplayName("다른 사용자의 티켓 상세 조회 시도 - 실패")
	void getTicketDetail_unauthorizedUser_fail() {

		User otherUser = userHelper.createUser(UserRole.NORMAL, null).user();
		Ticket saved = ticketHelper.createIssuedTicket(user, seat, event);

		assertThatThrownBy(() ->
			ticketService.getTicketDetail(saved.getId(), otherUser.getId())
		)
			.isInstanceOf(ErrorException.class)
			.hasMessageContaining("티켓에 대한 접근 권한이 없습니다");
	}

	// ===== 존재하지 않는 리소스 테스트 =====

	@Test
	@DisplayName("존재하지 않는 이벤트로 Draft Ticket 생성 - 실패")
	void getOrCreateDraft_notFoundEvent_fail() {

		Long invalidEventId = 999999L;

		assertThatThrownBy(() ->
			ticketService.getOrCreateDraft(invalidEventId, user.getId())
		)
			.isInstanceOf(ErrorException.class)
			.hasMessageContaining("이벤트를 찾을 수 없습니다");
	}

	@Test
	@DisplayName("존재하지 않는 사용자로 Draft Ticket 생성 - 실패")
	void getOrCreateDraft_notFoundUser_fail() {

		Long invalidUserId = 999999L;

		assertThatThrownBy(() ->
			ticketService.getOrCreateDraft(event.getId(), invalidUserId)
		)
			.isInstanceOf(ErrorException.class)
			.hasMessageContaining("유저를 찾을 수 없습니다");
	}

	@Test
	@DisplayName("존재하지 않는 티켓으로 결제 확정 시도 - 실패")
	void confirmPayment_notFoundTicket_fail() {

		Long invalidTicketId = 999999L;

		assertThatThrownBy(() ->
			ticketService.confirmPayment(invalidTicketId, user.getId())
		)
			.isInstanceOf(ErrorException.class)
			.hasMessageContaining("티켓을 찾을 수 없습니다");
	}

	@Test
	@DisplayName("존재하지 않는 티켓으로 결제 실패 처리 시도 - 실패")
	void failPayment_notFoundTicket_fail() {

		Long invalidTicketId = 999999L;

		assertThatThrownBy(() ->
			ticketService.failPayment(invalidTicketId)
		)
			.isInstanceOf(ErrorException.class)
			.hasMessageContaining("티켓을 찾을 수 없습니다");
	}

	// ===== 좌석 할당 관련 테스트 =====

	@Test
	@DisplayName("Draft Ticket의 좌석 변경 - 성공")
	void changeSeat_success() {

		Seat seat2 = seatHelper.createSeat(event, "A2", SeatGrade.R);

		// Draft 생성 후 첫 번째 좌석 할당
		Ticket draft = ticketService.getOrCreateDraft(event.getId(), user.getId());
		draft.assignSeat(seat);
		ticketRepository.save(draft);

		assertThat(draft.getSeat().getId()).isEqualTo(seat.getId());

		// 두 번째 좌석으로 변경
		draft.clearSeat();
		draft.assignSeat(seat2);
		ticketRepository.save(draft);

		Ticket updated = ticketRepository.findById(draft.getId()).get();
		assertThat(updated.getSeat().getId()).isEqualTo(seat2.getId());
	}

	@Test
	@DisplayName("Draft Ticket의 좌석 해제 - 성공")
	void clearSeat_success() {

		// Draft 생성 후 좌석 할당
		Ticket draft = ticketService.getOrCreateDraft(event.getId(), user.getId());
		draft.assignSeat(seat);
		ticketRepository.save(draft);

		assertThat(draft.getSeat()).isNotNull();

		// 좌석 해제
		draft.clearSeat();
		ticketRepository.save(draft);

		Ticket updated = ticketRepository.findById(draft.getId()).get();
		assertThat(updated.getSeat()).isNull();
	}

	// ===== 잘못된 상태 전이 테스트 =====

	@Test
	@DisplayName("ISSUED 상태 티켓을 결제 확정 시도 - 실패 (상태 전이 오류)")
	void confirmPayment_invalidTicketState_fail() {

		// ISSUED 상태 티켓 생성
		Ticket issuedTicket = ticketHelper.createIssuedTicket(user, seat, event);

		assertThatThrownBy(() ->
			ticketService.confirmPayment(issuedTicket.getId(), user.getId())
		)
			.isInstanceOf(ErrorException.class)
			.hasMessageContaining("티켓 상태가 유효하지 않습니다");
	}

	@Test
	@DisplayName("ISSUED 상태 티켓을 결제 실패 처리 시도 - 실패 (상태 전이 오류)")
	void failPayment_invalidTicketState_fail() {

		// ISSUED 상태 티켓 생성
		Ticket issuedTicket = ticketHelper.createIssuedTicket(user, seat, event);

		assertThatThrownBy(() ->
			ticketService.failPayment(issuedTicket.getId())
		)
			.isInstanceOf(ErrorException.class)
			.hasMessageContaining("티켓 상태가 유효하지 않습니다");
	}

	@Test
	@DisplayName("FAILED 상태 티켓을 결제 확정 시도 - 실패 (상태 전이 오류)")
	void confirmPayment_failedTicket_fail() {

		// DRAFT 티켓 생성 후 좌석 할당
		Ticket draft = ticketService.getOrCreateDraft(event.getId(), user.getId());
		draft.assignSeat(seat);
		ticketRepository.save(draft);

		// 결제 실패 처리
		seat.markAsReserved();
		seatRepository.save(seat);
		ticketService.failPayment(draft.getId());

		assertThatThrownBy(() ->
			ticketService.confirmPayment(draft.getId(), user.getId())
		)
			.isInstanceOf(ErrorException.class)
			.hasMessageContaining("티켓 상태가 유효하지 않습니다");
	}
}