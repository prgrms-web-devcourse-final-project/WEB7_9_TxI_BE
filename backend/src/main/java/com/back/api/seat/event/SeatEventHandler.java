package com.back.api.seat.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.back.api.seat.dto.response.SeatStatusMessage;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SeatEventHandler {

	private final SeatWebSocketPublisher publisher;

	@EventListener
	public void handleSeatStatus(SeatStatusMessage msg) {
		publisher.publish(msg);
	}
}
