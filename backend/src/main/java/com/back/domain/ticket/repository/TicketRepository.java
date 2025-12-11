package com.back.domain.ticket.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.back.domain.ticket.entity.Ticket;
import com.back.domain.ticket.entity.TicketStatus;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

	List<Ticket> findByOwnerId(Long userId);

	boolean existsBySeatIdAndTicketStatus(Long seatId, TicketStatus status);

	boolean existsBySeatIdAndTicketStatusIn(Long seatId, List<TicketStatus> paid);

	@Query("SELECT t FROM Ticket t WHERE t.ticketStatus = :status AND t.createAt < :time")
	List<Ticket> findExpiredDraftTickets(TicketStatus status, LocalDateTime time);

	Optional<Ticket> findBySeatIdAndOwnerIdAndTicketStatus(Long seatId, Long userId, TicketStatus ticketStatus);
}
