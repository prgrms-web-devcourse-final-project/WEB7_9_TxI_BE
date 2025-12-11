package com.back.api.queue.event;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.back.api.queue.dto.response.QueueEntryStatusResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QueueEntryWebSocketPublisher {

	private final SimpMessagingTemplate messagingTemplate;

	//사용자에게 WebSocket 메시지 전송
	public void publisherToUser(QueueEntryStatusResponse response) {
		String destination = "/topic/users/" + response.userId() + "/queue";

		//기존 response DTO를 JSON으로 변환해서 전달
		messagingTemplate.convertAndSend(destination, response);
	}
}
