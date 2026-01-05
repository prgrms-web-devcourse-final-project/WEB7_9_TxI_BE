package com.back.domain.ticket.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.domain.ticket.entity.Ticket;
import com.back.domain.ticket.entity.TicketStatus;

import jakarta.persistence.LockModeType;

public interface TicketRepository extends JpaRepository<Ticket, Long>, TicketRepositoryCustom {

	List<Ticket> findByOwnerId(Long userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT t FROM Ticket t JOIN FETCH t.owner WHERE t.id = :ticketId")
	Optional<Ticket> findByIdForUpdate(@Param("ticketId") Long ticketId);

	@Query(
		value = """
				SELECT t
				FROM Ticket t
				WHERE t.ticketStatus = :status
				AND t.createAt < :time
				ORDER BY t.id
			""",
		countQuery = """
				SELECT COUNT(t)
				FROM Ticket t
				WHERE t.ticketStatus = :status
				AND t.createAt < :time
			"""
	)
	Page<Ticket> findExpiredDraftTickets(
		@Param("status") TicketStatus status,
		@Param("time") LocalDateTime time,
		Pageable pageable
	);

	Optional<Ticket> findBySeatIdAndOwnerIdAndTicketStatus(Long seatId, Long userId, TicketStatus ticketStatus);

	Optional<Ticket> findByEventIdAndOwnerIdAndTicketStatus(Long eventId, Long userId, TicketStatus ticketStatus);

	@Query("SELECT t FROM Ticket t "
		+ "LEFT JOIN FETCH t.event e "
		+ "LEFT JOIN FETCH t.seat s "
		+ "WHERE t.id = :ticketId")
	Optional<Ticket> findByIdWithDetails(@Param("ticketId") Long ticketId);
}
