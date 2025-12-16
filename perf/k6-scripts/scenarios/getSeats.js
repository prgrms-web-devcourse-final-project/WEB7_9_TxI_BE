import http from "k6/http";
import { check } from "k6";

/**
 * GET /api/v1/events/{eventId}/seats
 * 이벤트별 좌석 목록 조회 시나리오 (인증 필요, 큐 입장 필요)
 *
 * @param {string} baseUrl - API 기본 주소
 * @param {string} jwt - 사용자 JWT 토큰
 * @param {string} testId - 테스트 실행 ID (로그/메트릭 태깅용)
 * @param {number} eventId - 조회할 이벤트 ID
 */
export function getSeats(baseUrl, jwt, testId, eventId) {
  const url = `${baseUrl}/api/v1/events/${eventId}/seats`;

  const params = {
    headers: {
      Authorization: `Bearer ${jwt}`,
      Accept: "application/json",
    },
    tags: {
      api: "getSeats",
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

  // 서버 응답 구조: { message: string, data: List<SeatResponse> }
  const data = json?.data ?? null;

  check(res, {
    "status 200": (r) => r.status === 200,
    "data exists": () => data !== null,
    "data is array": () => Array.isArray(data),
    "seats length ≥ 0": () => data.length >= 0,
  });

  // 좌석이 있을 경우 첫 번째 좌석 구조 검증
  if (Array.isArray(data) && data.length > 0) {
    const firstSeat = data[0];
    check(res, {
      "has id": () => typeof firstSeat?.id === "number",
      "has eventId": () => typeof firstSeat?.eventId === "number",
      "has seatCode": () => typeof firstSeat?.seatCode === "string",
      "has grade": () => typeof firstSeat?.grade === "string",
      "has price": () => typeof firstSeat?.price === "number",
      "has seatStatus": () => typeof firstSeat?.seatStatus === "string",
    });
  }

  if (res.status !== 200) {
    console.error(`❌ getSeats failed [${res.status}]:`, JSON.stringify({
      status: res.status,
      message: json?.message,
      eventId,
    }));
  }

  return res;
}
