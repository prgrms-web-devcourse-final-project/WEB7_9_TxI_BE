package com.back.api.seat.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.back.api.seat.dto.response.SeatStatusMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatEventHandler {

	private final SeatWebSocketPublisher publisher;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleSeatStatus(SeatStatusMessage msg) {
		log.debug("SEAT_EVENT_RECEIVED eventId={} seatId={} currentStatus={}", msg.eventId(), msg.seatId(),
			msg.currentStatus());
		publisher.publish(msg);
	}
}
