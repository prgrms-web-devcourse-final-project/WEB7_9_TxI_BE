package com.back.api.queue.service;

import org.springframework.stereotype.Service;

import com.back.domain.queue.repository.QueueEntryRedisRepository;
import com.back.domain.queue.repository.QueueEntryRepository;

import lombok.RequiredArgsConstructor;

/*
 * 대기열 처리 로직
 * QueueEntry 입장 처리
 * 스케줄러, 배치 활용
 * 대기중 -> 입장 완료
 */

@Service
@RequiredArgsConstructor
public class QueueEntryService {

	private final QueueEntryRepository queueEntryRepository;
	private final QueueEntryRedisRepository queueEntryRedisRepository;


}
