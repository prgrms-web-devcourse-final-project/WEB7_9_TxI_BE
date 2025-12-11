package com.back.config;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.back.api.seat.dto.response.SeatStatusMessage;

@Component
public class TestSeatStatusEventListener {

	private final List<SeatStatusMessage> events = new CopyOnWriteArrayList<>();

	@EventListener
	public void listen(SeatStatusMessage message) {
		events.add(message);
	}

	public List<SeatStatusMessage> getEvents() {
		return events;
	}

	public void clear() {
		events.clear();
	}
}
