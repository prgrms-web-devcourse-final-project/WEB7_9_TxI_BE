package com.back.domain.queue.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.domain.queue.entity.QueueEntry;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {


}
