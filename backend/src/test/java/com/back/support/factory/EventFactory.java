package com.back.support.factory;

import java.time.LocalDateTime;

import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventCategory;
import com.back.domain.event.entity.EventStatus;
import com.back.domain.store.entity.Store;

public class EventFactory extends BaseFactory {

	public static Event fakeEvent(Store store) {
		int minPrice = faker.number().numberBetween(10000, 50000);
		int maxPrice = faker.number().numberBetween(minPrice, 200000);

		return Event.builder()
			.title(faker.book().title())
			.category(EventCategory.CONCERT)
			.description(faker.lorem().sentence())
			.place(faker.address().city())
			.imageUrl(faker.internet().image())
			.minPrice(minPrice)
			.maxPrice(maxPrice)
			.preOpenAt(LocalDateTime.now().minusDays(7))
			.preCloseAt(LocalDateTime.now().minusDays(5))
			.ticketOpenAt(LocalDateTime.now().minusDays(3))
			.ticketCloseAt(LocalDateTime.now().plusDays(30))
			.eventDate(LocalDateTime.now().plusDays(35))
			.maxTicketAmount(faker.number().numberBetween(1000, 10000))
			.status(EventStatus.OPEN)
			.store(store)
			.build();
	}

	public static Event fakeEvent(Store store, String title) {
		int minPrice = faker.number().numberBetween(10000, 50000);
		int maxPrice = faker.number().numberBetween(minPrice, 200000);

		return Event.builder()
			.title(title)
			.category(EventCategory.CONCERT)
			.description(faker.lorem().sentence())
			.place(faker.address().city())
			.imageUrl(faker.internet().image())
			.minPrice(minPrice)
			.maxPrice(maxPrice)
			.preOpenAt(LocalDateTime.now().minusDays(7))
			.preCloseAt(LocalDateTime.now().minusDays(5))
			.ticketOpenAt(LocalDateTime.now().minusDays(3))
			.ticketCloseAt(LocalDateTime.now().plusDays(30))
			.eventDate(LocalDateTime.now().plusDays(35))
			.maxTicketAmount(faker.number().numberBetween(1000, 10000))
			.status(EventStatus.OPEN)
			.store(store)
			.build();
	}

	public static Event fakeEvent(Store store, EventCategory category, EventStatus status) {
		int minPrice = faker.number().numberBetween(10000, 50000);
		int maxPrice = faker.number().numberBetween(minPrice, 200000);

		return Event.builder()
			.title(faker.book().title())
			.category(category)
			.description(faker.lorem().sentence())
			.place(faker.address().city())
			.imageUrl(faker.internet().image())
			.minPrice(minPrice)
			.maxPrice(maxPrice)
			.preOpenAt(LocalDateTime.now().minusDays(7))
			.preCloseAt(LocalDateTime.now().minusDays(5))
			.ticketOpenAt(LocalDateTime.now().minusDays(3))
			.ticketCloseAt(LocalDateTime.now().plusDays(30))
			.eventDate(LocalDateTime.now().plusDays(35))
			.maxTicketAmount(faker.number().numberBetween(1000, 10000))
			.status(status)
			.store(store)
			.build();
	}

	public static Event fakePreOpenEvent(Store store) {
		int minPrice = faker.number().numberBetween(10000, 50000);
		int maxPrice = faker.number().numberBetween(minPrice, 200000);

		return Event.builder()
			.title(faker.book().title())
			.category(EventCategory.CONCERT)
			.description(faker.lorem().sentence())
			.place(faker.address().city())
			.imageUrl(faker.internet().image())
			.minPrice(minPrice)
			.maxPrice(maxPrice)
			.preOpenAt(LocalDateTime.now().minusDays(1))
			.preCloseAt(LocalDateTime.now().plusDays(7))
			.ticketOpenAt(LocalDateTime.now().plusDays(10))
			.ticketCloseAt(LocalDateTime.now().plusDays(30))
			.eventDate(LocalDateTime.now().plusDays(35))
			.maxTicketAmount(faker.number().numberBetween(1000, 10000))
			.status(EventStatus.PRE_OPEN)
			.store(store)
			.build();
	}

	public static Event fakeReadyEvent(Store store) {
		int minPrice = faker.number().numberBetween(10000, 50000);
		int maxPrice = faker.number().numberBetween(minPrice, 200000);

		return Event.builder()
			.title(faker.book().title())
			.category(EventCategory.CONCERT)
			.description(faker.lorem().sentence())
			.place(faker.address().city())
			.imageUrl(faker.internet().image())
			.minPrice(minPrice)
			.maxPrice(maxPrice)
			.preOpenAt(LocalDateTime.now().plusDays(5))
			.preCloseAt(LocalDateTime.now().plusDays(10))
			.ticketOpenAt(LocalDateTime.now().plusDays(15))
			.ticketCloseAt(LocalDateTime.now().plusDays(30))
			.eventDate(LocalDateTime.now().plusDays(35))
			.maxTicketAmount(faker.number().numberBetween(1000, 10000))
			.status(EventStatus.READY)
			.store(store)
			.build();
	}

	public static Event fakePreClosedEvent(Store store) {
		int minPrice = faker.number().numberBetween(10000, 50000);
		int maxPrice = faker.number().numberBetween(minPrice, 200000);

		return Event.builder()
			.title(faker.book().title())
			.category(EventCategory.CONCERT)
			.description(faker.lorem().sentence())
			.place(faker.address().city())
			.imageUrl(faker.internet().image())
			.minPrice(minPrice)
			.maxPrice(maxPrice)
			.preOpenAt(LocalDateTime.now().minusDays(10))
			.preCloseAt(LocalDateTime.now().minusDays(8))
			.ticketOpenAt(LocalDateTime.now().minusDays(5))
			.ticketCloseAt(LocalDateTime.now().plusDays(10))
			.eventDate(LocalDateTime.now().plusDays(15))
			.maxTicketAmount(faker.number().numberBetween(1000, 10000))
			.status(EventStatus.OPEN)
			.store(store)
			.build();
	}

	public static Event fakePastEvent(Store store, String title) {
		int minPrice = faker.number().numberBetween(10000, 50000);
		int maxPrice = faker.number().numberBetween(minPrice, 200000);

		return Event.builder()
			.title(title)
			.category(EventCategory.CONCERT)
			.description(faker.lorem().sentence())
			.place(faker.address().city())
			.imageUrl(faker.internet().image())
			.minPrice(minPrice)
			.maxPrice(maxPrice)
			.preOpenAt(LocalDateTime.now().minusDays(40))
			.preCloseAt(LocalDateTime.now().minusDays(38))
			.ticketOpenAt(LocalDateTime.now().minusDays(37))
			.ticketCloseAt(LocalDateTime.now().minusDays(10))
			.eventDate(LocalDateTime.now().minusDays(1)) // 이미 지난 날짜
			.maxTicketAmount(faker.number().numberBetween(1000, 10000))
			.status(EventStatus.OPEN)
			.store(store)
			.build();
	}
}
