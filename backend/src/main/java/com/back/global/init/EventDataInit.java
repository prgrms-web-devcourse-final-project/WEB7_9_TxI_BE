package com.back.global.init;

import java.time.LocalDateTime;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventCategory;
import com.back.domain.event.entity.EventStatus;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.store.entity.Store;
import com.back.domain.store.repository.StoreRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
@Order(2)
public class EventDataInit implements ApplicationRunner {

	private final EventRepository eventRepository;
	private final StoreRepository storeRepository;

	@Override
	public void run(ApplicationArguments args) {
		if (eventRepository.count() > 0) {
			log.info("Event 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
			return;
		}

		log.info("Event 초기 데이터를 생성합니다.");

		Store store = storeRepository.findAll().getFirst();

		LocalDateTime now = LocalDateTime.now();

		Event event1 = Event.builder()
			.title("아이유 2025 콘서트 HEREH WORLD TOUR")
			.category(EventCategory.CONCERT)
			.description("아이유의 월드투어 서울 공연입니다. 최고의 무대를 만나보세요!")
			.place("올림픽공원 체조경기장")
			.imageUrl("https://example.com/iu-concert.jpg")
			.minPrice(99000)
			.maxPrice(154000)
			.preOpenAt(now.minusDays(2))
			.preCloseAt(now.minusDays(1))
			.ticketOpenAt(now.plusMinutes(10))
			.ticketCloseAt(now.plusDays(30))
			.eventDate(now.plusDays(40))
			.maxTicketAmount(5000)
			.status(EventStatus.PRE_OPEN)
			.store(store)
			.build();

		Event event2 = Event.builder()
			.title("나이키 에어맥스 한정판 드롭")
			.category(EventCategory.DROP)
			.description("나이키 에어맥스 한정판 스니커즈 드롭 이벤트입니다.")
			.place("온라인")
			.imageUrl("https://example.com/nike-airmax.jpg")
			.minPrice(189000)
			.maxPrice(189000)
			.preOpenAt(now.plusDays(1))
			.preCloseAt(now.plusDays(3))
			.ticketOpenAt(now.plusDays(5))
			.ticketCloseAt(now.plusDays(7))
			.eventDate(now.plusDays(10))
			.maxTicketAmount(500)
			.status(EventStatus.READY)
			.store(store)
			.build();

		Event event3 = Event.builder()
			.title("디즈니 100주년 팝업 스토어")
			.category(EventCategory.POPUP)
			.description("디즈니 100주년 기념 팝업스토어에서 특별한 굿즈를 만나보세요.")
			.place("성수동 S-Factory")
			.imageUrl("https://example.com/disney-popup.jpg")
			.minPrice(15000)
			.maxPrice(250000)
			.preOpenAt(now.minusDays(10))
			.preCloseAt(now.minusDays(8))
			.ticketOpenAt(now.minusDays(5))
			.ticketCloseAt(now.plusDays(14))
			.eventDate(now.plusDays(21))
			.maxTicketAmount(3000)
			.status(EventStatus.PRE_OPEN)
			.store(store)
			.build();

		Event event4 = Event.builder()
			.title("2025 스프링 재즈 나이트")
			.category(EventCategory.CONCERT)
			.description("국내 최고의 재즈 아티스트들과 함께하는 밤, 감성 가득한 스프링 재즈 콘서트를 즐겨보세요.")
			.place("예술의전당 콘서트홀")
			.imageUrl("https://example.com/jazz-night.jpg")
			.minPrice(15000)
			.maxPrice(250000)
			.preOpenAt(now.minusDays(10))
			.preCloseAt(now.minusDays(8))
			.ticketOpenAt(now.plusHours(1).plusMinutes(5))
			.ticketCloseAt(now.plusDays(14))
			.eventDate(now.plusDays(21))
			.maxTicketAmount(3000)
			.status(EventStatus.PRE_OPEN)
			.store(store)
			.build();

		eventRepository.save(event1);
		eventRepository.save(event2);
		eventRepository.save(event3);
		eventRepository.save(event4);

		log.info("Event 초기 데이터 4개가 생성되었습니다.");
	}
}
