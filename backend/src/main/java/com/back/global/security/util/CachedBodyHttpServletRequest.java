package com.back.global.security.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.springframework.util.StreamUtils;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * Request Body를 여러 번 읽을 수 있도록 캐싱하는 HttpServletRequest Wrapper
 *
 * 문제점:
 * - HttpServletRequest의 InputStream은 한 번만 읽을 수 있음
 * - Filter에서 Body를 읽으면 Controller에서 읽을 수 없음
 *
 * 해결책:
 * - 생성자에서 Request Body를 바이트 배열로 미리 읽어서 캐싱
 * - getInputStream(), getReader() 호출 시마다 캐시된 바이트에서 새 스트림 생성
 *
 * 사용처:
 * - RateLimitFilter: SMS 요청에서 전화번호 추출 후 Controller에서도 Body 읽기
 * - FingerprintFilter: Fingerprint 추출 후 Controller에서도 Body 읽기
 *
 * 보안 고려사항:
 * - DoS 공격 방지를 위해 Body 크기 제한 (기본: 1MB)
 * - Tomcat의 max-http-post-size와 별개로 2차 방어선 역할
 * - 대용량 요청 시 메모리 폭파 방지
 *
 * 주의사항:
 * - 대용량 파일 업로드 시 메모리 문제 발생 가능 (필요한 경로에만 적용)
 * - JSON API 요청(수 KB 이하)에만 사용 권장
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

	/**
	 * 최대 허용 Request Body 크기 (1MB)
	 * SMS/사전등록 API는 JSON만 받으므로 1MB면 충분
	 */
	private static final int MAX_BODY_SIZE = 1024 * 1024; // 1MB

	private final byte[] cachedBody;

	/**
	 * Request Body를 미리 읽어서 캐싱
	 *
	 * DoS 공격 방지:
	 * - Content-Length 헤더를 먼저 확인하여 크기 제한 초과 시 예외 발생
	 * - 악의적인 대용량 요청으로부터 메모리 보호
	 *
	 * @param request 원본 HttpServletRequest
	 * @throws IOException Body 읽기 실패 시 또는 크기 제한 초과 시
	 */
	public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
		super(request);

		// DoS 공격 방지: Content-Length 체크 (1차 방어)
		int contentLength = request.getContentLength();
		if (contentLength > MAX_BODY_SIZE) {
			throw new IOException(
				String.format("Request body too large: %d bytes (max: %d bytes)",
					contentLength, MAX_BODY_SIZE)
			);
		}

		// StreamUtils.copyToByteArray: Spring 유틸리티로 InputStream을 바이트 배열로 복사
		// 2차 방어: 실제 읽는 과정에서도 크기 체크 (Content-Length 헤더 조작 대응)
		byte[] body = StreamUtils.copyToByteArray(request.getInputStream());

		if (body.length > MAX_BODY_SIZE) {
			throw new IOException(
				String.format("Request body too large: %d bytes (max: %d bytes)",
					body.length, MAX_BODY_SIZE)
			);
		}

		this.cachedBody = body;
	}

	/**
	 * 캐시된 Body로부터 새로운 ServletInputStream 반환
	 *
	 * 호출 시마다 새로운 스트림을 생성하므로 여러 번 읽기 가능
	 *
	 * @return 캐시된 Body의 ServletInputStream
	 */
	@Override
	public ServletInputStream getInputStream() {
		return new CachedBodyServletInputStream(this.cachedBody);
	}

	/**
	 * 캐시된 Body로부터 새로운 BufferedReader 반환
	 *
	 * 호출 시마다 새로운 Reader를 생성하므로 여러 번 읽기 가능
	 * UTF-8 인코딩 명시적 지정
	 *
	 * @return 캐시된 Body의 BufferedReader
	 */
	@Override
	public BufferedReader getReader() throws IOException {
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.cachedBody);
		return new BufferedReader(new InputStreamReader(byteArrayInputStream, StandardCharsets.UTF_8));
	}

	/**
	 * 캐시된 Body 바이트 배열 반환 (직접 접근용)
	 *
	 * Filter에서 JSON 파싱 등에 활용
	 *
	 * @return 캐시된 Request Body 바이트 배열
	 */
	public byte[] getCachedBody() {
		return this.cachedBody;
	}
}
