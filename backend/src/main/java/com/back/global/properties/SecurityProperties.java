package com.back.global.properties;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * 보안 관련 설정 프로퍼티
 *
 * application.yml에서 security.bot-protection 하위 설정을 바인딩
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "security.bot-protection")
public class SecurityProperties {

	/**
	 * 화이트리스트 IP 목록 (개발/테스트 환경)
	 * - 이 IP들은 모든 보안 필터를 우회
	 * - Rate Limit, IDC 차단, Fingerprint 체크 등 모두 건너뜀
	 */
	private List<String> whitelistIps = new ArrayList<>();

	/**
	 * Rate Limit 설정
	 */
	private RateLimit rateLimit = new RateLimit();

	/**
	 * IDC IP 차단 설정
	 */
	private IdcBlock idcBlock = new IdcBlock();

	/**
	 * Device Fingerprint 설정
	 */
	private Fingerprint fingerprint = new Fingerprint();

	/**
	 * 신뢰할 수 있는 프록시 개수 (X-Forwarded-For IP 추출용)
	 * - 0: 직접 연결 (X-Forwarded-For를 신뢰하지 않음, RemoteAddr 사용)
	 * - 1: ALB만 있음 (마지막 IP 사용)
	 * - 2: CloudFront + ALB (마지막에서 2번째 IP 사용)
	 */
	private int trustedProxyCount = 1;

	@Getter
	@Setter
	public static class RateLimit {
		/**
		 * SMS/사전등록 API Rate Limit (분당 요청 수)
		 * IP + 전화번호 조합으로 제한
		 */
		private int smsPerMinute = 7;

		/**
		 * Rate Limit 활성화 여부
		 */
		private boolean enabled = true;
	}

	@Getter
	@Setter
	public static class IdcBlock {
		/**
		 * IDC IP 차단 활성화 여부
		 */
		private boolean enabled = true;

		/**
		 * IDC IP 리스트 갱신 주기 (cron 표현식)
		 * 기본값: 매주 월요일 새벽 3시
		 */
		private String refreshCron = "0 0 3 * * MON";

		/**
		 * IDC IP 리스트 URL (GitHub 또는 공식 소스)
		 */
		private List<String> ipListUrls = new ArrayList<>();
	}

	@Getter
	@Setter
	public static class Fingerprint {
		/**
		 * Device Fingerprint 검증 활성화 여부
		 */
		private boolean enabled = true;

		/**
		 * 차단 기준: 최소 시도 횟수
		 */
		private int minAttempts = 5;

		/**
		 * 차단 기준: 최대 시도 횟수
		 * 성공/실패 관계없이 총 시도 횟수가 이 값을 초과하면 차단
		 * 시나리오 A (성공 폭탄 공격) 방어용
		 */
		private int maxAttempts = 10;

		/**
		 * 차단 기준: 실패율 (0.0 ~ 1.0)
		 * 예: 0.8 = 80%
		 */
		private double failureRateThreshold = 0.8;

		/**
		 * Fingerprint 데이터 TTL (초)
		 */
		private long ttlSeconds = 300; // 24시간 (테스트일때 5분)
	}
}
