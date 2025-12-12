package com.back.support.factory;

import com.back.domain.event.entity.Event;
import com.back.domain.queue.entity.QueueEntry;
import com.back.domain.queue.entity.QueueEntryStatus;
import com.back.domain.user.entity.User;

public class QueueEntryFactory extends BaseFactory {


	public static QueueEntry fakeQueueEntry(Event event, User user) {
		return QueueEntry.builder()
			.event(event)
			.user(user)
			.queueRank(faker.number().numberBetween(1, 100))
			.queueEntryStatus(QueueEntryStatus.WAITING)
			.build();
	}

	/**
	 * 특정 순위의 WAITING 상태 QueueEntry 생성
	 */
	public static QueueEntry fakeQueueEntry(Event event, User user, int rank) {
		return QueueEntry.builder()
			.event(event)
			.user(user)
			.queueRank(rank)
			.queueEntryStatus(QueueEntryStatus.WAITING)
			.build();
	}

	/**
	 * 특정 상태의 QueueEntry 생성
	 */
	public static QueueEntry fakeQueueEntry(Event event, User user, int rank, QueueEntryStatus status) {
		QueueEntry queueEntry = QueueEntry.builder()
			.event(event)
			.user(user)
			.queueRank(rank)
			.queueEntryStatus(QueueEntryStatus.WAITING)
			.build();

		// 상태에 따라 적절한 메서드 호출
		switch (status) {
			case ENTERED -> queueEntry.enterQueue();
			case EXPIRED -> {
				queueEntry.enterQueue();
				queueEntry.expire();
			}
			case COMPLETED -> {
				queueEntry.enterQueue();
				queueEntry.completePayment();
			}
		}

		return queueEntry;
	}

	/**
	 * ENTERED 상태의 QueueEntry 생성
	 */
	public static QueueEntry fakeEnteredQueueEntry(Event event, User user) {
		QueueEntry queueEntry = QueueEntry.builder()
			.event(event)
			.user(user)
			.queueRank(faker.number().numberBetween(1, 10))
			.queueEntryStatus(QueueEntryStatus.WAITING)
			.build();
		queueEntry.enterQueue();
		return queueEntry;
	}

	/**
	 * EXPIRED 상태의 QueueEntry 생성
	 */
	public static QueueEntry fakeExpiredQueueEntry(Event event, User user) {
		QueueEntry queueEntry = QueueEntry.builder()
			.event(event)
			.user(user)
			.queueRank(faker.number().numberBetween(1, 10))
			.queueEntryStatus(QueueEntryStatus.ENTERED)
			.build();
		queueEntry.enterQueue();
		queueEntry.expire();
		return queueEntry;
	}

	/**
	 * COMPLETED 상태의 QueueEntry 생성
	 */
	public static QueueEntry fakeCompletedQueueEntry(Event event, User user) {
		QueueEntry queueEntry = QueueEntry.builder()
			.event(event)
			.user(user)
			.queueRank(faker.number().numberBetween(1, 10))
			.queueEntryStatus(QueueEntryStatus.ENTERED)
			.build();
		queueEntry.enterQueue();
		queueEntry.completePayment();
		return queueEntry;
	}
}
