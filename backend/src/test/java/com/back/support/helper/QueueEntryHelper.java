package com.back.support.helper;

import java.util.List;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

import com.back.domain.event.entity.Event;
import com.back.domain.queue.entity.QueueEntry;
import com.back.domain.queue.entity.QueueEntryStatus;
import com.back.domain.queue.repository.QueueEntryRedisRepository;
import com.back.domain.queue.repository.QueueEntryRepository;
import com.back.domain.user.entity.User;
import com.back.support.factory.QueueEntryFactory;


@Component
public class QueueEntryHelper {

	private final QueueEntryRepository queueEntryRepository;
	private final QueueEntryRedisRepository queueEntryRedisRepository;

	public QueueEntryHelper(QueueEntryRepository queueEntryRepository,
		QueueEntryRedisRepository queueEntryRedisRepository) {
		this.queueEntryRepository = queueEntryRepository;
		this.queueEntryRedisRepository = queueEntryRedisRepository;
	}


	public QueueEntry createQueueEntry(Event event, User user, int rank) {
		QueueEntry queueEntry = QueueEntryFactory.fakeQueueEntry(event, user, rank);
		return queueEntryRepository.save(queueEntry);
	}


	public QueueEntry createQueueEntry(Event event, User user, int rank, QueueEntryStatus status) {
		QueueEntry queueEntry = QueueEntryFactory.fakeQueueEntry(event, user, rank, status);
		return queueEntryRepository.save(queueEntry);
	}


	public QueueEntry createEnteredQueueEntry(Event event, User user) {
		QueueEntry queueEntry = QueueEntryFactory.fakeEnteredQueueEntry(event, user);
		return queueEntryRepository.save(queueEntry);
	}


	public QueueEntry createExpiredQueueEntry(Event event, User user) {
		QueueEntry queueEntry = QueueEntryFactory.fakeExpiredQueueEntry(event, user);
		return queueEntryRepository.save(queueEntry);
	}


	public QueueEntry createCompletedQueueEntry(Event event, User user) {
		QueueEntry queueEntry = QueueEntryFactory.fakeCompletedQueueEntry(event, user);
		return queueEntryRepository.save(queueEntry);
	}

	// 여러 사용자의 WAITING 상태 QueueEntry 생성 (순위 자동 할당)
	public List<QueueEntry> createQueueEntries(Event event, List<User> users) {
		List<QueueEntry> queueEntries = IntStream.range(0, users.size())
			.mapToObj(i -> QueueEntryFactory.fakeQueueEntry(event, users.get(i), i + 1))
			.toList();
		return queueEntryRepository.saveAll(queueEntries);
	}

	public QueueEntry createQueueEntryWithRedis(Event event, User user, int rank) {
		QueueEntry queueEntry = createQueueEntry(event, user, rank);
		queueEntryRedisRepository.addToWaitingQueue(event.getId(), user.getId(), rank);
		return queueEntry;
	}

	public List<QueueEntry> createQueueEntriesWithRedis(Event event, List<User> users) {
		List<QueueEntry> queueEntries = createQueueEntries(event, users);
		for (int i = 0; i < users.size(); i++) {
			queueEntryRedisRepository.addToWaitingQueue(event.getId(), users.get(i).getId(), i + 1);
		}
		return queueEntries;
	}

	public QueueEntry createEnteredQueueEntryWithRedis(Event event, User user) {
		QueueEntry queueEntry = createEnteredQueueEntry(event, user);
		queueEntryRedisRepository.moveToEnteredQueue(event.getId(), user.getId());
		return queueEntry;
	}

	public void clearRedis(Long eventId) {
		queueEntryRedisRepository.clearAll(eventId);
	}

	public void clearAll(Long eventId) {
		clearRedis(eventId);
		queueEntryRepository.deleteAll();
	}
}
