package com.back.global.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EventPublisher {
	private final ApplicationEventPublisher publisher;

	public <T> void publishEvent(T event) {
		publisher.publishEvent(event);
	}
}