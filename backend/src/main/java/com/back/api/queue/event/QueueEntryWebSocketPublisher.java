package com.back.api.queue.event;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.back.api.queue.dto.response.QueueEntryStatusResponse;
import com.back.api.queue.dto.response.WaitingQueueBatchEventResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QueueEntryWebSocketPublisher {

	private final SimpMessagingTemplate messagingTemplate;

	//unicast - 입장/만료/결제 완료 처리
	public void publisherToUser(QueueEntryStatusResponse response) {

		String destination = "/topic/users/" + response.userId() + "/queue";

		//기존 response DTO를 JSON으로 변환해서 전달
		messagingTemplate.convertAndSend(destination, response);

	}

	//broadcast - 대기 상태
	public void publishBatchUpdate(WaitingQueueBatchEventResponse event) {

		String destination = "/topic/events/" + event.eventId() + "/queue";

		// Map<Long, WaitingQueueResponse> 전송
		messagingTemplate.convertAndSend(destination, event.updates());


	}
}
