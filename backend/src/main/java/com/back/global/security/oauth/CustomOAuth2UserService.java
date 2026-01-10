package com.back.global.security.oauth;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.auth.service.SocialAuthService;
import com.back.domain.auth.entity.ProviderType;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.global.security.SecurityUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

	private final SocialAuthService socialAuthService;

	@Transactional
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User oAuth2User = super.loadUser(userRequest);

		Map<String, Object> attributes = oAuth2User.getAttributes();

		log.debug("Kakao attributes={}", attributes);

		String nickname = "";
		String email = "";

		Object accObj = attributes.get("kakao_account");
		if (accObj instanceof Map<?, ?> acc) {
			Object profileObj = acc.get("profile");
			if (profileObj instanceof Map<?, ?> profile) {
				Object nicknameObj = profile.get("nickname");
				if (nicknameObj != null)
					nickname = nicknameObj.toString();
			}

			Object emailObj = acc.get("email");
			email = emailObj.toString();
		}

		if (nickname == null || nickname.isBlank()) {
			log.error("Kakao nickname is missing");
			throw new OAuth2AuthenticationException("Kakao nickname is missing");
		}

		String kakaoId = oAuth2User.getName();

		User user = socialAuthService.modifyOrJoin(kakaoId, nickname, "", email, ProviderType.KAKAO);

		Collection<GrantedAuthority> authorities =
			List.of(new SimpleGrantedAuthority("ROLE_NORMAL"));

		String userPassword = (user.getPassword() != null && !user.getPassword().isEmpty())
			? user.getPassword()
			: "password123";

		return new SecurityUser(
			user.getId(),
			userPassword,
			user.getNickname(),
			UserRole.NORMAL,
			null,
			authorities
		);
	}
}
