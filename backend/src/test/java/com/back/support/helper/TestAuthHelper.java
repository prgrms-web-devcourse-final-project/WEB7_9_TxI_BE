package com.back.support.helper;

import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.back.domain.user.entity.User;
import com.back.global.security.SecurityUser;

@Component
public class TestAuthHelper {
	public void authenticate(User user) {

		List<GrantedAuthority> authorities =
			List.of(new SimpleGrantedAuthority("ROLE_USER"));

		SecurityUser securityUser = new SecurityUser(
			user.getId(),
			user.getEmail(),
			user.getPassword(),
			user.getRole(),
			authorities
		);

		Authentication authentication = new UsernamePasswordAuthenticationToken(
			securityUser,
			null,
			securityUser.getAuthorities()
		);

		SecurityContextHolder.getContext().setAuthentication(authentication);
	}
}
