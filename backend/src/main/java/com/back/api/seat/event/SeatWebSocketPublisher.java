package com.back.api.seat.event;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.back.api.seat.dto.response.SeatStatusMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatWebSocketPublisher {

	private final SimpMessagingTemplate messagingTemplate;

	public void publish(SeatStatusMessage msg) {
		String destination = "/topic/events/" + msg.eventId() + "/seats";
		log.debug("WS_PUBLISH destination={} eventId={} seatId={} currentStatus={}", destination, msg.eventId(),
			msg.seatId(),
			msg.currentStatus());
		messagingTemplate.convertAndSend(destination, msg);
		log.debug("WS_PUBLISH_COMPLETE destination={}", destination);
	}
}
