package com.back.api.queue.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.back.api.queue.dto.response.QueueEntryStatusResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class QueueEntryEventHandler {

	private final QueueEntryWebSocketPublisher publisher;

	//Spring ApplicationEvent 수신
	//@EventListenr가 QueueEntryStatusResponse 타입의 이벤트 감지
	@EventListener
	//@Async 비동기처리가 필요할것인가?
	public void handleQueueStatus(QueueEntryStatusResponse response) {
		publisher.publisherToUser(response);
	}

}
