package com.back.support.factory;

import java.time.LocalDateTime;

import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventCategory;
import com.back.domain.event.entity.EventStatus;

public class EventFactory extends BaseFactory {

	public static Event fakeEvent() {
		return Event.builder()
			.title(faker.book().title())
			.category(EventCategory.CONCERT)
			.description(faker.lorem().sentence())
			.place(faker.address().city())
			.imageUrl(faker.internet().image())
			.minPrice(faker.number().numberBetween(10000, 50000))
			.maxPrice(faker.number().numberBetween(100000, 200000))
			.preOpenAt(LocalDateTime.now().minusDays(7))
			.preCloseAt(LocalDateTime.now().minusDays(5))
			.ticketOpenAt(LocalDateTime.now().minusDays(3))
			.ticketCloseAt(LocalDateTime.now().plusDays(30))
			.maxTicketAmount(faker.number().numberBetween(1000, 10000))
			.status(EventStatus.OPEN)
			.build();
	}

	public static Event fakeEvent(String title) {
		return Event.builder()
			.title(title)
			.category(EventCategory.CONCERT)
			.description(faker.lorem().sentence())
			.place(faker.address().city())
			.imageUrl(faker.internet().image())
			.minPrice(faker.number().numberBetween(10000, 50000))
			.maxPrice(faker.number().numberBetween(100000, 200000))
			.preOpenAt(LocalDateTime.now().minusDays(7))
			.preCloseAt(LocalDateTime.now().minusDays(5))
			.ticketOpenAt(LocalDateTime.now().minusDays(3))
			.ticketCloseAt(LocalDateTime.now().plusDays(30))
			.maxTicketAmount(faker.number().numberBetween(1000, 10000))
			.status(EventStatus.OPEN)
			.build();
	}

	public static Event fakeEvent(EventCategory category, EventStatus status) {
		return Event.builder()
			.title(faker.book().title())
			.category(category)
			.description(faker.lorem().sentence())
			.place(faker.address().city())
			.imageUrl(faker.internet().image())
			.minPrice(faker.number().numberBetween(10000, 50000))
			.maxPrice(faker.number().numberBetween(100000, 200000))
			.preOpenAt(LocalDateTime.now().minusDays(7))
			.preCloseAt(LocalDateTime.now().minusDays(5))
			.ticketOpenAt(LocalDateTime.now().minusDays(3))
			.ticketCloseAt(LocalDateTime.now().plusDays(30))
			.maxTicketAmount(faker.number().numberBetween(1000, 10000))
			.status(status)
			.build();
	}
}