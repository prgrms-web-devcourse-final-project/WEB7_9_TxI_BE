package com.back.api.seat.dto;

public record SelectSeatRequest(
	Long userId  // TODO: Security Context에서 가져오도록 변경 필요
) {
}