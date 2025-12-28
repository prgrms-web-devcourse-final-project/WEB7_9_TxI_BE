package com.back.api.seed.dto.response;

public record SeedResult(
	String scenario,
	Long eventId,
	int users,
	int preRegisteredUsers,
	boolean queueShuffled,
	String note
) {
}
