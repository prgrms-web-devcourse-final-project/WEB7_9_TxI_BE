package com.back.api.ticket.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.back.domain.event.entity.Event;
import com.back.domain.store.entity.Store;
import com.back.domain.ticket.entity.Ticket;
import com.back.domain.ticket.entity.TicketStatus;
import com.back.domain.ticket.repository.TicketRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.support.helper.EventHelper;
import com.back.support.helper.StoreHelper;
import com.back.support.helper.UserHelper;

@SpringBootTest
@ActiveProfiles("test")
// @Transactional을 제거하여 프로덕션 환경 시뮬레이션
@DisplayName("TicketService seat=null NPE 버그 검증")
class TicketServiceBugTest {

	@Autowired
	private TicketService ticketService;

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private UserHelper userHelper;

	@Autowired
	private EventHelper eventHelper;

	@Autowired
	private StoreHelper storeHelper;

	@Test
	@DisplayName("seat=null인 티켓에 failPayment() 호출 시 정상적으로 FAILED로 변경")
	void failPayment_withNullSeat_success() {
		Store store = storeHelper.createStore();
		// given: seat=null인 Draft 티켓 생성
		User user = userHelper.createUser(UserRole.NORMAL, null).user();
		Event event = eventHelper.createEvent(store);

		Ticket ticket = Ticket.builder()
			.owner(user)
			.event(event)
			.seat(null)  // seat이 null
			.ticketStatus(TicketStatus.DRAFT)
			.build();
		Ticket saved = ticketRepository.save(ticket);

		// when: failPayment() 호출
		ticketService.failPayment(saved.getId());

		// then: 티켓이 FAILED 상태로 변경됨
		Ticket result = ticketRepository.findById(saved.getId()).orElseThrow();
		assertThat(result.getTicketStatus())
			.as("seat=null이어도 안전하게 FAILED로 변경됨")
			.isEqualTo(TicketStatus.FAILED);
	}
}
