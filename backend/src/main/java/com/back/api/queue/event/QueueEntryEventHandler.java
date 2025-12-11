package com.back.api.queue.event;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.back.api.queue.dto.event.WaitingQueueBatchEvent;
import com.back.api.queue.dto.response.QueueEntryStatusResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class QueueEntryEventHandler {

	private final QueueEntryWebSocketPublisher publisher;

	//Spring ApplicationEvent 수신
	//@EventListener 이벤트 감지
	@EventListener
	@Async
	public void handleQueueStatus(QueueEntryStatusResponse response) {
		publisher.publisherToUser(response);
	}

	@EventListener
	@Async
	public void handleQueueBatchUpdate(WaitingQueueBatchEvent event) {
		publisher.publishBatchUpdate(event);
	}

}
