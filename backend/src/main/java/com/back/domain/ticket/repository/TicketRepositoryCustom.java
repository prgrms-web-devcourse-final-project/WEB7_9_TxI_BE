package com.back.domain.ticket.repository;

import java.util.List;

import com.back.api.ticket.dto.response.TicketResponse;

public interface TicketRepositoryCustom {

	List<TicketResponse> findMyTicketDto(Long userId);

}