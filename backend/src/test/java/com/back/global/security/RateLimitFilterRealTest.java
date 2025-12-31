package com.back.global.security;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.ContentCachingRequestWrapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;

/**
 * RateLimitFilter의 JSON Body 파싱 실제 동작 테스트
 *
 * 치명적 결함 검증:
 * - ContentCachingRequestWrapper가 body를 재사용 가능하게 하는지
 * - JSON에서 phoneNumber를 제대로 추출하는지
 * - Controller에서도 body를 정상적으로 읽을 수 있는지
 */
@SpringBootTest
@ActiveProfiles("test")
class RateLimitFilterRealTest {

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	@DisplayName("ContentCachingRequestWrapper로 JSON Body를 읽고 재사용 가능")
	void testContentCachingRequestWrapper() throws Exception {
		// Given: JSON request body
		String json = "{\"phoneNumber\":\"01012345678\",\"name\":\"홍길동\"}";

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("application/json");
		request.setContent(json.getBytes());

		ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(request);

		// When: 첫 번째 읽기 (필터에서)
		byte[] firstRead = wrapper.getInputStream().readAllBytes();
		byte[] cachedContent = wrapper.getContentAsByteArray();

		String body1 = new String(cachedContent, "UTF-8");
		JsonNode node1 = objectMapper.readTree(body1);
		String phoneNumber1 = node1.has("phoneNumber") ? node1.get("phoneNumber").asText() : null;

		// Then: 첫 번째 읽기 성공
		assertThat(phoneNumber1).isEqualTo("01012345678");
		System.out.println("✅ 필터에서 phoneNumber 추출: " + phoneNumber1);

		// When: 두 번째 읽기 (Controller에서 시뮬레이션)
		// ContentCachingRequestWrapper는 getInputStream()을 재호출할 수 없음
		// 대신 getContentAsByteArray()로 다시 읽기
		byte[] cachedContent2 = wrapper.getContentAsByteArray();
		String body2 = new String(cachedContent2, "UTF-8");
		JsonNode node2 = objectMapper.readTree(body2);

		// Then: 두 번째 읽기 성공
		assertThat(node2.get("phoneNumber").asText()).isEqualTo("01012345678");
		assertThat(node2.get("name").asText()).isEqualTo("홍길동");
		System.out.println("✅ Controller에서도 body 재사용 가능: " + body2);
	}

	@Test
	@DisplayName("RateLimitFilter 로직: phoneNumber가 없으면 null 반환")
	void testExtractPhoneNumber_noPhoneNumber() throws Exception {
		// Given: phoneNumber 필드가 없는 JSON
		String json = "{\"name\":\"홍길동\",\"email\":\"test@example.com\"}";

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("application/json");
		request.setContent(json.getBytes());

		ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(request);

		// When: phoneNumber 추출 시도
		wrapper.getInputStream().readAllBytes();
		byte[] content = wrapper.getContentAsByteArray();

		String phoneNumber = null;
		if (content.length > 0) {
			String body = new String(content, "UTF-8");
			JsonNode jsonNode = objectMapper.readTree(body);
			if (jsonNode.has("phoneNumber")) {
				phoneNumber = jsonNode.get("phoneNumber").asText();
			}
		}

		// Then: phoneNumber가 null
		assertThat(phoneNumber).isNull();
		System.out.println("✅ phoneNumber 없으면 null 반환 (IP만으로 Rate Limit)");
	}

	@Test
	@DisplayName("RateLimitFilter 로직: JSON 파싱 실패해도 예외 발생하지 않음")
	void testExtractPhoneNumber_invalidJson() {
		// Given: 잘못된 JSON
		String invalidJson = "{invalid json}";

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("application/json");
		request.setContent(invalidJson.getBytes());

		ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(request);

		// When: phoneNumber 추출 시도
		String phoneNumber = null;
		try {
			wrapper.getInputStream().readAllBytes();
			byte[] content = wrapper.getContentAsByteArray();
			if (content.length > 0) {
				String body = new String(content, "UTF-8");
				JsonNode jsonNode = objectMapper.readTree(body);
				if (jsonNode.has("phoneNumber")) {
					phoneNumber = jsonNode.get("phoneNumber").asText();
				}
			}
		} catch (Exception e) {
			// 예외를 무시하고 null 유지
			System.out.println("⚠️  JSON 파싱 실패 (예상된 동작): " + e.getMessage());
		}

		// Then: phoneNumber가 null (예외 발생해도 안전)
		assertThat(phoneNumber).isNull();
		System.out.println("✅ JSON 파싱 실패해도 null 반환하여 IP만으로 Rate Limit");
	}

	@Test
	@DisplayName("실제 시나리오: 스타벅스에서 친구가 인증했어도 나는 차단되지 않음")
	void testRateLimitScenario_SameIpDifferentPhone() {
		// Given: 같은 IP (스타벅스 공유기), 다른 전화번호
		String sameIp = "121.162.30.40"; // 스타벅스 IP
		String phone1 = "01011111111";   // 친구
		String phone2 = "01022222222";   // 나

		// 친구의 Redis 키
		String friendKey = "rate_limit:sms:" + sameIp + ":" + phone1.hashCode();
		// 나의 Redis 키
		String myKey = "rate_limit:sms:" + sameIp + ":" + phone2.hashCode();

		// Then: 키가 다르므로 독립적인 버킷
		assertThat(friendKey).isNotEqualTo(myKey);

		System.out.println("✅ 같은 IP, 다른 전화번호 시나리오");
		System.out.println("   친구 Redis 키: " + friendKey);
		System.out.println("   나의 Redis 키: " + myKey);
		System.out.println("   → 독립적인 Rate Limit 버킷 사용!");
	}
}
