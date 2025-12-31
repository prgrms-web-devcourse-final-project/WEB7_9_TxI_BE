package com.back.global.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QrTokenClaims {
	private final Long ticketId;
	private final Long userId;
	private final Long eventId;
	private final Long issuedAt;
}
