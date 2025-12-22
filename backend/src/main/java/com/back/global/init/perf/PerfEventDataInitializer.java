package com.back.global.init.perf;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventCategory;
import com.back.domain.event.entity.EventStatus;
import com.back.domain.event.repository.EventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
@Profile("perf")
public class PerfEventDataInitializer {

	private final EventRepository eventRepository;

	public void init(int eventCount) {
		if (eventRepository.count() > 0) {
			log.info("Event 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
			return;
		}

		log.info("Event 초기 데이터 생성 중: {}개", eventCount);

		LocalDateTime now = LocalDateTime.now();
		List<Event> events = new ArrayList<>();

		// 필수 이벤트 3개 (각 상태별 최소 1개)
		// 1. 사전등록 진행중인 이벤트 (부하 테스트 메인 타겟)
		events.add(Event.builder()
			.title("아이유 2025 HEREH WORLD TOUR - 서울")
			.category(EventCategory.CONCERT)
			.description("아이유의 월드투어 서울 공연입니다. 최고의 무대를 만나보세요!")
			.place("잠실종합운동장 올림픽 주경기장")
			.imageUrl("https://example.com/iu-concert.jpg")
			.minPrice(88000)
			.maxPrice(165000)
			.preOpenAt(now.minusDays(2))
			.preCloseAt(now.plusDays(5))
			.ticketOpenAt(now.plusDays(7))
			.ticketCloseAt(now.plusDays(30))
			.eventDate(now.plusDays(35))
			.maxTicketAmount(500)
			.status(EventStatus.PRE_OPEN)
			.build());

		// 2. 대기열 준비중인 이벤트
		events.add(Event.builder()
			.title("세븐틴 BE THE SUN - 부산")
			.category(EventCategory.CONCERT)
			.description("세븐틴의 2025년 콘서트 투어입니다.")
			.place("부산 아시아드 주경기장")
			.imageUrl("https://example.com/seventeen.jpg")
			.minPrice(99000)
			.maxPrice(154000)
			.preOpenAt(now.minusDays(10))
			.preCloseAt(now.minusDays(3))
			.ticketOpenAt(now.plusMinutes(30))
			.ticketCloseAt(now.plusDays(20))
			.eventDate(now.plusDays(25))
			.maxTicketAmount(500)
			.status(EventStatus.QUEUE_READY)
			.build());

		// 3. 티켓팅 진행중인 이벤트 (부하 테스트 메인 타겟 - 티켓팅 경쟁)
		events.add(Event.builder()
			.title("뉴진스 2025 콘서트 - 서울")
			.category(EventCategory.CONCERT)
			.description("뉴진스의 2025년 서울 콘서트입니다. 최고의 무대를 만나보세요!")
			.place("고척스카이돔")
			.imageUrl("https://example.com/newjeans-concert.jpg")
			.minPrice(99000)
			.maxPrice(165000)
			.preOpenAt(now.minusDays(5))
			.preCloseAt(now.minusDays(2))
			.ticketOpenAt(now.minusHours(1))
			.ticketCloseAt(now.plusDays(3))
			.eventDate(now.plusDays(8))
			.maxTicketAmount(500)
			.status(EventStatus.OPEN)
			.build());

		// 4. 티켓 완판 이벤트 (부하 테스트 타겟 - 티켓 조회/관리)
		if (eventCount >= 4) {
			events.add(Event.builder()
				.title("르세라핌 2025 콘서트 - 서울")
				.category(EventCategory.CONCERT)
				.description("르세라핌의 2025년 서울 콘서트입니다. (완판)")
				.place("KSPO돔")
				.imageUrl("https://example.com/lesserafim-concert.jpg")
				.minPrice(99000)
				.maxPrice(165000)
				.preOpenAt(now.minusDays(30))
				.preCloseAt(now.minusDays(20))
				.ticketOpenAt(now.minusDays(15))
				.ticketCloseAt(now.minusDays(10))
				.eventDate(now.minusDays(5))
				.maxTicketAmount(500)
				.status(EventStatus.CLOSED)
				.build());
		}

		// 추가 이벤트 생성 (Event #5+, 최소 4개 요청 시)
		int additionalCount = Math.max(0, eventCount - 4);

		for (int i = 0; i < additionalCount; i++) {
			if (i % 3 == 0) {
				events.add(createConcertEvent("테스트 콘서트 " + (i + 5), now, 15000, 85000, 10000));
			} else if (i % 3 == 1) {
				events.add(createPopupEvent("테스트 팝업 " + (i + 5), now, 0, 250000, 15000));
			} else if (i % 3 == 2) {
				events.add(createDropEvent("테스트 드롭 " + (i + 5), now, 159000, 159000, 2000));
			}
		}

		eventRepository.saveAll(events);

		log.info("✅ Event 데이터 생성 완료: {}개 (PRE_OPEN:1, QUEUE_READY:1, OPEN:1, CLOSED:1, READY:{})",
			events.size(), Math.max(0, events.size() - 4));
	}

	private Event createConcertEvent(String title, LocalDateTime baseTime,
		int minPrice, int maxPrice, int maxTickets) {
		return Event.builder()
			.title(title)
			.category(EventCategory.CONCERT)
			.description(title + " - 부하테스트용 이벤트")
			.place("공연장")
			.imageUrl("https://example.com/" + title.hashCode() + ".jpg")
			.minPrice(minPrice)
			.maxPrice(maxPrice)
			.preOpenAt(baseTime.plusDays(2))
			.preCloseAt(baseTime.plusDays(9))
			.ticketOpenAt(baseTime.plusDays(12))
			.ticketCloseAt(baseTime.plusDays(30))
			.eventDate(baseTime.plusDays(35))
			.maxTicketAmount(maxTickets)
			.status(EventStatus.READY)
			.build();
	}

	private Event createPopupEvent(String title, LocalDateTime baseTime,
		int minPrice, int maxPrice, int maxTickets) {
		return Event.builder()
			.title(title)
			.category(EventCategory.POPUP)
			.description(title + " - 부하테스트용 이벤트")
			.place("팝업 스토어")
			.imageUrl("https://example.com/" + title.hashCode() + ".jpg")
			.minPrice(minPrice)
			.maxPrice(maxPrice)
			.preOpenAt(baseTime.plusDays(1))
			.preCloseAt(baseTime.plusDays(5))
			.ticketOpenAt(baseTime.plusDays(7))
			.ticketCloseAt(baseTime.plusDays(21))
			.eventDate(baseTime.plusDays(25))
			.maxTicketAmount(maxTickets)
			.status(EventStatus.READY)
			.build();
	}

	private Event createDropEvent(String title, LocalDateTime baseTime,
		int minPrice, int maxPrice, int maxTickets) {
		return Event.builder()
			.title(title)
			.category(EventCategory.DROP)
			.description(title + " - 부하테스트용 이벤트")
			.place("온라인")
			.imageUrl("https://example.com/" + title.hashCode() + ".jpg")
			.minPrice(minPrice)
			.maxPrice(maxPrice)
			.preOpenAt(baseTime.plusDays(1))
			.preCloseAt(baseTime.plusDays(3))
			.ticketOpenAt(baseTime.plusDays(5))
			.ticketCloseAt(baseTime.plusDays(10))
			.eventDate(baseTime.plusDays(12))
			.maxTicketAmount(maxTickets)
			.status(EventStatus.READY)
			.build();
	}
}
