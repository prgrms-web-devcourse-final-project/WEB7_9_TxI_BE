package com.back.domain.ticket.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.back.api.ticket.dto.response.TicketResponse;
import com.back.domain.ticket.entity.Ticket;

public interface TicketRepositoryCustom {

	List<TicketResponse> findMyTicketDto(Long userId);

	List<Ticket> findIssuedOrPaidBeforeEvent(Long userId, LocalDateTime now);
}
