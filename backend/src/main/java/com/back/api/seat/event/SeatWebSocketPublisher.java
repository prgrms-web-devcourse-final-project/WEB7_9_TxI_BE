package com.back.api.seat.event;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.back.api.seat.dto.response.SeatStatusMessage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatWebSocketPublisher {

	private final SimpMessagingTemplate messagingTemplate;

	public void publish(SeatStatusMessage msg) {
		messagingTemplate.convertAndSend(
			"/topic/events/" + msg.eventId() + "/seats",
			msg
		);
	}
}
