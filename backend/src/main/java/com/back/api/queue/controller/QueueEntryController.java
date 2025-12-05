package com.back.api.queue.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.queue.service.QueueEntryService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "QueueEntry API", description = "큐 대기열 API")
@RestController
@RequestMapping("/api/v1/queues")
@RequiredArgsConstructor
public class QueueEntryController {

	private final QueueEntryService queueEntryService;
}
