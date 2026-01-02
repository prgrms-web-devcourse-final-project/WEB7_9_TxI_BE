package com.back.api.ticket.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.ticket.dto.response.QrTokenResponse;
import com.back.api.ticket.dto.response.QrValidationResponse;
import com.back.domain.ticket.entity.Ticket;
import com.back.domain.ticket.entity.TicketStatus;
import com.back.global.error.code.TicketErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.properties.SiteProperties;
import com.back.global.security.QrTokenClaims;
import com.back.global.utils.JwtUtil;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class QrService {

	private final RedisTemplate<String, String> redisTemplate;

	private final SiteProperties siteProperties;

	private final TicketService ticketService;

	public QrService(
		@Qualifier("stringTemplate")
		RedisTemplate<String, String> redisTemplate,
		SiteProperties siteProperties,
		TicketService ticketService
	) {
		this.redisTemplate = redisTemplate;
		this.siteProperties = siteProperties;
		this.ticketService = ticketService;
	}

	@Value("${custom.jwt.qr-secret}")
	private String qrSecret;

	private static final long QR_TOKEN_VALIDATE_SECEONDS = 60L; //60초

	private static final String CLAIM_TICKET_ID = "ticketId";
	private static final String CLAIM_EVENT_ID = "eventId";
	private static final String CLAIM_USER_ID = "userId";
	private static final String CLAIM_IAT = "iat";

	// QR 토큰 발급
	@Transactional(readOnly = true)
	public QrTokenResponse generateQrTokenResponse(Long ticketId, Long userId) {
		Ticket ticket = ticketService.getTicketDetail(ticketId, userId);

		// ISSUED 상태에서만 QR 발급
		if(ticket.getTicketStatus() != TicketStatus.ISSUED) {
			throw new ErrorException(TicketErrorCode.INVALID_TICKET_STATE);
		}

		LocalDateTime now = LocalDateTime.now();
		LocalDateTime eventDate = ticket.getEvent().getEventDate();

		// 이벤트 시작 전에는 QR 발급 불가
		if(now.isBefore(eventDate)) {
			throw new ErrorException(TicketErrorCode.EVENT_NOT_STARTED);
		}

		String qrToken = generateQrToken(ticket, userId);
		String qrUrl = buildQrUrl(qrToken);

		return new QrTokenResponse(qrToken, 60, 30, qrUrl);

	}

	// QR 입장 검증 및 처리
	@Transactional
	public QrValidationResponse validateAndProcessEntry(String qrToken) {

		QrTokenClaims claims = validateAndParseQrToken(qrToken);

		Ticket ticket = ticketService.findById(claims.getTicketId());

		if(ticket.getTicketStatus() != TicketStatus.ISSUED) {
			return buildInvalidResponse(
				ticket,
				"유효하지 않은 티켓 상태입니다.",
				claims.getIssuedAt()
			);
		}

		if(isEntered(ticket.getId())) {
			return buildInvalidResponse(
				ticket,
				"이미 입장 처리된 티켓입니다.",
				claims.getIssuedAt()
			);
		}

		markAsEntered(ticket.getId());
		ticket.markAsUsed();

		return buildValidResponse(ticket, claims.getIssuedAt());

	}


	private String generateQrToken(Ticket ticket, Long userId) {
		if(!ticket.getOwner().getId().equals(userId)) {
			throw new ErrorException(TicketErrorCode.UNAUTHORIZED_TICKET_ACCESS);
		}

		long now = Instant.now().getEpochSecond(); //1970-01-01 00:00:00 UTC 기준 초 단위

		// JWT 토큰 생성
		Map<String, Object> claims = new HashMap<>();
		claims.put(CLAIM_TICKET_ID, ticket.getId());
		claims.put(CLAIM_EVENT_ID, ticket.getEvent().getId());
		claims.put(CLAIM_USER_ID, userId);
		claims.put(CLAIM_IAT, now);

		return JwtUtil.sign(qrSecret,QR_TOKEN_VALIDATE_SECEONDS,claims);
	}

	private QrTokenClaims validateAndParseQrToken(String qrToken) {
		try {
			Map<String, Object> payload = JwtUtil.payloadOrNull(qrToken, qrSecret);

			if(payload == null) {
				log.warn("Invalid QR token: payload is null");
				throw new ErrorException(TicketErrorCode.INVALID_TICKET_QR_TOKEN);
			}

			Long ticketId = getLongValue(payload, CLAIM_TICKET_ID);
			Long eventId = getLongValue(payload, CLAIM_EVENT_ID);
			Long userId = getLongValue(payload, CLAIM_USER_ID);
			Long issuedAt = getLongValue(payload, CLAIM_IAT);

			// TODO -> 2중 체크 로직 제거 고려
			// 토큰 기간 확인 -> 60초 이상이면 만료
			long now = Instant.now().getEpochSecond();
			if(now - issuedAt > QR_TOKEN_VALIDATE_SECEONDS) {
				throw new ErrorException(TicketErrorCode.TICKET_QR_TOKEN_EXPIRED);
			}

			return new QrTokenClaims(ticketId, eventId, userId, issuedAt);

		} catch (ExpiredJwtException e) {
			log.warn("QR token expired");
			throw new ErrorException(TicketErrorCode.TICKET_QR_TOKEN_EXPIRED);

		} catch (JwtException e) {
			log.warn("Invalid QR token");
			throw new ErrorException(TicketErrorCode.INVALID_TICKET_QR_TOKEN);
		}
	}

	// 입장 여부 확인
	private boolean isEntered(Long ticketId) {
		String redisKey = buildRedisKey(ticketId);
		return redisTemplate.opsForValue().get(redisKey) != null;
	}

	// QR URL 생성
	private String buildQrUrl(String qrToken) {
		return siteProperties.getFrontUrl() + "/tickets/verify?token=" + qrToken;
	}

	// 입장 처리 기록 -> Redis
	private void markAsEntered(Long ticketId) {
		String redisKey = buildRedisKey(ticketId);
		String existingEntry = redisTemplate.opsForValue().get(redisKey);

		if(existingEntry != null) {
			throw new ErrorException(TicketErrorCode.TICKET_ALREADY_ENTERED);
		}

		// 입장 기록 TTL 24시간
		redisTemplate.opsForValue().set(
			redisKey,
			String.valueOf(Instant.now().getEpochSecond()),
			Duration.ofHours(24)
		);

		log.info("Marked ticket {} as entered in Redis", ticketId);
	}

	private Long getLongValue(Map<String, Object> payload, String key) {
		Object value = payload.get(key);

		if (value instanceof Number number) {
			return number.longValue();
		} else {
			throw new ErrorException(TicketErrorCode.INVALID_TICKET_QR_TOKEN);
		}
	}

	private String buildRedisKey(Long ticketId) {
		return "entry:ticket:" + ticketId;
	}


	// 유효한 입장 응답 생성
	private QrValidationResponse buildValidResponse(Ticket ticket, Long issuedAtEpoch) {
		LocalDateTime qrIssuedAt = LocalDateTime.ofInstant(
			Instant.ofEpochSecond(issuedAtEpoch),
			ZoneId.systemDefault()
		);

		return new QrValidationResponse(
			true,
			"QR 코드가 유효합니다.",
			ticket.getId(),
			ticket.getEvent().getId(),
			ticket.getEvent().getTitle(),
			ticket.getSeat() != null ? ticket.getSeat().getSeatCode() : null,
			ticket.getOwner().getNickname(),
			ticket.getEvent().getEventDate(),
			qrIssuedAt
		);
	}

	// 유효하지 않은 입장 응답 생성
	private QrValidationResponse buildInvalidResponse(
		Ticket ticket,
		String message,
		Long issuedAtEpoch
	) {
		LocalDateTime qrIssuedAt = LocalDateTime.ofInstant(
			Instant.ofEpochSecond(issuedAtEpoch),
			ZoneId.systemDefault()
		);

		return new QrValidationResponse(
			false,
			message,
			ticket.getId(),
			ticket.getEvent().getId(),
			ticket.getEvent().getTitle(),
			ticket.getSeat() != null ? ticket.getSeat().getSeatCode() : null,
			ticket.getOwner().getNickname(),
			ticket.getEvent().getEventDate(),
			qrIssuedAt
		);
	}
}
