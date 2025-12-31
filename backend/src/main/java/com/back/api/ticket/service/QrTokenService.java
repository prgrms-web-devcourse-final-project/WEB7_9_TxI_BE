package com.back.api.ticket.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.back.domain.ticket.entity.Ticket;
import com.back.global.error.code.TicketErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.properties.SiteProperties;
import com.back.global.utils.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QrTokenService {

	private final RedisTemplate<String, String> redisTemplate;
	private final SiteProperties siteProperties;

	@Value("${custom.jwt.qr-secret}")
	private String qrSecret;

	private static final long QR_TOKEN_VALIDATE_SECEONDS = 60L; //60초

	private static final String CLAIM_TICKET_ID = "ticketId";
	private static final String CLAIM_EVENT_ID = "eventId";
	private static final String CLAIM_USER_ID = "userId";
	private static final String CLAIM_ISSUED_AT = "issuedAt";


	public String generateQrToken(Ticket ticket, Long userId) {
		if(!ticket.getOwner().getId().equals(userId)) {
			throw new ErrorException(TicketErrorCode.UNAUTHORIZED_TICKET_ACCESS);
		}

		long now = Instant.now().getEpochSecond(); //1970-01-01 00:00:00 UTC 기준 초 단위
		Map<String, Object> claims = new HashMap<>();
		claims.put(CLAIM_TICKET_ID, ticket.getId());
		claims.put(CLAIM_EVENT_ID, ticket.getEvent().getId());
		claims.put(CLAIM_USER_ID, userId);
		claims.put(CLAIM_ISSUED_AT, now);

		return JwtUtil.sign(qrSecret,QR_TOKEN_VALIDATE_SECEONDS,claims);
	}


}
