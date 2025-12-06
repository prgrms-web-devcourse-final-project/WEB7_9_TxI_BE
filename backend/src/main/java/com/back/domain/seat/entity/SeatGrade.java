package com.back.domain.seat.entity;

import lombok.Getter;

@Getter
public enum SeatGrade {

	VIP("VIP"),
	R("R"),
	S("S"),
	A("A");

	private final String displayName;

	SeatGrade(String displayName) {
		this.displayName = displayName;
	}
}
