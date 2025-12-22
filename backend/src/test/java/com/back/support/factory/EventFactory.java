package com.back.support.factory;

import java.time.LocalDateTime;

import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventCategory;
import com.back.domain.event.entity.EventStatus;

public class EventFactory extends BaseFactory {

	public static Event fakeEvent() {
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
			.build();
	}

	public static Event fakeEvent(String title) {
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
			.build();
	}

	public static Event fakeEvent(EventCategory category, EventStatus status) {
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
			.build();
	}

	public static Event fakePreOpenEvent() {
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
			.build();
	}

	public static Event fakeReadyEvent() {
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
			.build();
	}

	public static Event fakePreClosedEvent() {
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
			.build();
	}
}
