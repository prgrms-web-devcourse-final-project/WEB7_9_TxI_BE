package com.back.domain.ticket.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.domain.ticket.entity.Ticket;
import com.back.domain.ticket.entity.TicketStatus;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

	List<Ticket> findByOwnerId(Long userId);

	boolean existsBySeatIdAndTicketStatus(Long seatId, TicketStatus status);
}
