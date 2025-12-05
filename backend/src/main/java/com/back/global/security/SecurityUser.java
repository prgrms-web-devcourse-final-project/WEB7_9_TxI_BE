package com.back.global.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import lombok.Getter;

@Getter
public class SecurityUser extends User {

	private Long id;
	private String nickname;

	public SecurityUser(
		Long id,
		String password,
		String nickname,
		Collection<? extends GrantedAuthority> authorities
	) {
		super(nickname, password, authorities);
		this.id = id;
		this.nickname = nickname;
	}
}
