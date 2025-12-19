import http from "k6/http";
import { check } from "k6";

/**
 * PATCH /api/v1/notifications/read-all
 * 전체 알림 읽음 처리 시나리오 (인증 필요)
 *
 * 테스트 데이터:
 * - 사용자별 알림이 생성되어 있어야 함
 * - 각 사용자는 읽지 않은 알림을 가지고 있음 (약 30%)
 *
 * @param {string} baseUrl - API 기본 주소
 * @param {string} jwt - 사용자 JWT 토큰
 * @param {string} testId - 테스트 실행 ID (로그/메트릭 태깅용)
 */
export function markAllAsRead(baseUrl, jwt, testId) {
  const url = `${baseUrl}/api/v1/notifications/read-all`;

  const params = {
    headers: {
      Authorization: `Bearer ${jwt}`,
      Accept: "application/json",
    },
    tags: {
      api: "markAllAsRead",
      test_id: testId,
    },
  };

  const res = http.patch(url, null, params);

  let json;
  try {
    json = res.json();
  } catch {
    console.error("❌ JSON parse error:", res.body);
    return res;
  }

  // 서버 응답 구조: { message: string, data: null }
  const data = json?.data ?? null;

  check(res, {
    "status 200": (r) => r.status === 200,
    "data is null": () => data === null,
    "has success message": () => typeof json?.message === "string",
  });

  if (res.status !== 200) {
    console.error(`❌ markAllAsRead failed [${res.status}]:`, JSON.stringify({
      status: res.status,
      message: json?.message,
    }));
  } else {
    // 성공 시 메시지 로깅 (디버깅용)
    if (__ENV.DEBUG === "true") {
      console.log(`✅ All notifications marked as read: ${json?.message}`);
    }
  }

  return res;
}
