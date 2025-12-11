package com.back.api.queue.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.back.api.queue.dto.response.QueueEntryStatusResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueEntryEventHandler {

	private final QueueEntryWebSocketPublisher publisher;

	//Spring ApplicationEvent 수신
	//@EventListenr가 QueueEntryStatusResponse 타입의 이벤트 감지
	@EventListener
	//@Async 비동기처리가 필요할것인가?
	public void handleQueueStatus(QueueEntryStatusResponse response) {
		//publisher.publisherToUser(response);

		//테스트용 로그
		log.info("대기열 이벤트 수신 - userId: {}, eventId: {}, type: {}",
			response.userId(), response.eventId(), response.getClass().getSimpleName());

		try {
			publisher.publisherToUser(response);
		} catch (Exception e) {
			log.error("대기열 이벤트 처리 실패 - userId: {}, error: {}",
				response.userId(), e.getMessage(), e);
		}
	}

}
