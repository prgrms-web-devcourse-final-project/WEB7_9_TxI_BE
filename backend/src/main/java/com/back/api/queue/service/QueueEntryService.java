package com.back.api.queue.service;

import org.springframework.stereotype.Service;

import com.back.domain.queue.repository.QueueEntryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QueueEntryService {

	private final QueueEntryRepository queueEntryRepository;


}
