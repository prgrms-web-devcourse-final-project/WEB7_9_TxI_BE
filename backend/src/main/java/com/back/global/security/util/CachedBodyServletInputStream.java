package com.back.global.security.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

/**
 * 캐시된 Body를 읽기 위한 ServletInputStream
 *
 * 목적:
 * - Request Body를 여러 번 읽을 수 있도록 바이트 배열로부터 스트림 생성
 * - CachedBodyHttpServletRequest와 함께 사용
 *
 * 동작 방식:
 * - 생성자에서 받은 바이트 배열로부터 ByteArrayInputStream 생성
 * - getInputStream() 호출 시마다 새로운 스트림 반환
 */
public class CachedBodyServletInputStream extends ServletInputStream {

	private final InputStream cachedBodyInputStream;

	/**
	 * 캐시된 Body로부터 InputStream 생성
	 *
	 * @param cachedBody 캐시된 Request Body 바이트 배열
	 */
	public CachedBodyServletInputStream(byte[] cachedBody) {
		this.cachedBodyInputStream = new ByteArrayInputStream(cachedBody);
	}

	@Override
	public boolean isFinished() {
		try {
			return cachedBodyInputStream.available() == 0;
		} catch (IOException e) {
			return true;
		}
	}

	@Override
	public boolean isReady() {
		return true;
	}

	@Override
	public void setReadListener(ReadListener listener) {
		throw new UnsupportedOperationException("ReadListener는 지원하지 않습니다.");
	}

	@Override
	public int read() throws IOException {
		return cachedBodyInputStream.read();
	}
}
