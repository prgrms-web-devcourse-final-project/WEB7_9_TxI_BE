package com.back.api.seat.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.back.api.seat.dto.response.SeatStatusMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatEventHandler {

	private final SeatWebSocketPublisher publisher;

	@EventListener
	public void handleSeatStatus(SeatStatusMessage msg) {
		log.debug("SEAT_EVENT_RECEIVED eventId={} seatId={} status={}", msg.eventId(), msg.seatId(), msg.status());
		publisher.publish(msg);
	}
}
