import http from "k6/http";
import { check } from "k6";

/**
 * GET /api/v1/events/{eventId}
 * 이벤트 단건 조회 시나리오 (공개 API, 인증 불필요)
 *
 * @param {string} baseUrl - API 기본 주소
 * @param {string} testId - 테스트 실행 ID (로그/메트릭 태깅용)
 * @param {number} eventId - 조회할 이벤트 ID
 */
export function getEvent(baseUrl, testId, eventId) {
  const url = `${baseUrl}/api/v1/events/${eventId}`;

  const params = {
    headers: {
      Accept: "application/json",
    },
    tags: {
      api: "getEvent",
      test_id: testId,
    },
  };

  const res = http.get(url, params);

  let json;
  try {
    json = res.json();
  } catch {
    console.error("❌ JSON parse error:", res.body);
    return res;
  }

  // 서버 응답 구조: { message: string, data: EventResponse }
  const data = json?.data ?? null;

  check(res, {
    "status 200": (r) => r.status === 200,
    "data exists": () => data !== null,
    "has id": () => typeof data?.id === "number",
    "has title": () => typeof data?.title === "string",
    "has category": () => data?.category !== undefined,
    "has place": () => typeof data?.place === "string",
    "has status": () => data?.status !== undefined,
    "has description": () => typeof data?.description === "string",
  });

  if (res.status !== 200) {
    console.error(`❌ getEvent failed [${res.status}]:`, JSON.stringify({
      status: res.status,
      message: json?.message,
      eventId,
    }));
  }

  return res;
}