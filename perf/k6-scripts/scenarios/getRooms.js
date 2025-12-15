import http from "k6/http";
import { check } from "k6";

/**
 * GET /api/v1/challenge/rooms?page={page}&size={size}
 * 특정 사용자(jwt) 기준으로 방 목록을 조회하는 시나리오
 *
 * @param {string} baseUrl - API 기본 주소
 * @param {string} jwt - 사용자별 JWT 토큰
 * @param {string} testId - 테스트 실행 ID (로그/메트릭 태깅용)
 * @param {number} [page=0] - 페이지 번호
 * @param {number} [size=10] - 페이지당 아이템 수
 */
export function getRooms(baseUrl, jwt, testId, page = 0, size = 10) {
  const url = `${baseUrl}/api/v1/challenge/rooms?page=${page}&size=${size}`;
  const params = {
    headers: {
      Authorization: `Bearer ${jwt}`,
      Accept: "application/json",
    },
    tags: { api: "getRooms", test_id: testId },
  };

  const res = http.get(url, params);

  let json;
  try {
    json = res.json();
  } catch {
    console.error("❌ JSON parse error:", res.body);
    return res;
  }

  // 서버 응답 구조 예시:
  // { data: { rooms: [...], total: number } }
  const rooms = json?.data?.rooms ?? [];
  const total = json?.data?.total ?? -1;

  check(res, {
    "status 200": (r) => r.status === 200,
    "data exists": () => json?.data !== undefined && json?.data !== null,
    "rooms is array": () => Array.isArray(rooms),
    "total valid number": () => typeof total === "number" && total >= 0,
    "rooms length ≥ 0": () => rooms.length >= 0,
  });

  if (!Array.isArray(rooms)) {
    console.warn("⚠️ response shape mismatch:", JSON.stringify(json));
  }

  return res;
}