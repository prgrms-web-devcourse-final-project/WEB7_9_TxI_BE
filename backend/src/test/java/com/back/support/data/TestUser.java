package com.back.support.data;

import com.back.domain.user.entity.User;

public record TestUser(
	User user,
	String rawPassword
) {
}
