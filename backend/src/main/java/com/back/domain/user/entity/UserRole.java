package com.back.domain.user.entity;

public enum UserRole {
	NORMAL,
	ADMIN;

	public String toAuthority() {
		return "ROLE_" + this.name();
	}
}
