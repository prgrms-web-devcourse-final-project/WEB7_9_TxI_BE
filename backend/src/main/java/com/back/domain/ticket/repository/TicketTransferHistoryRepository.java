package com.back.domain.ticket.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.domain.ticket.entity.TicketTransferHistory;

public interface TicketTransferHistoryRepository extends JpaRepository<TicketTransferHistory, Long> {

	List<TicketTransferHistory> findByTicketIdOrderByTransferredAtDesc(Long ticketId);
}
