import http from "k6/http";
import { check } from "k6";

/**
 * POST /api/v1/events/{eventId}/seats/{seatId}/select
 * 좌석 선택 시나리오 (인증 필요)
 *
 * @param {string} baseUrl - API 기본 주소
 * @param {string} jwt - 사용자 JWT 토큰
 * @param {string} testId - 테스트 실행 ID (로그/메트릭 태깅용)
 * @param {number} eventId - 이벤트 ID
 * @param {number} seatId - 선택할 좌석 ID
 */
export function selectSeat(baseUrl, jwt, testId, eventId, seatId) {
  const url = `${baseUrl}/api/v1/events/${eventId}/seats/${seatId}/select`;

  const params = {
    headers: {
      Authorization: `Bearer ${jwt}`,
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    tags: {
      api: "selectSeat",
      test_id: testId,
      event_id: eventId,
      seat_id: seatId,
    },
  };

  const res = http.post(url, null, params);

  let json;
  try {
    json = res.json();
  } catch {
    console.error("❌ JSON parse error:", res.body);
    return res;
  }

  // 서버 응답 구조: { message: string, data: SeatSelectionResponse }
  const data = json?.data ?? null;

  check(res, {
    "status 200": (r) => r.status === 200,
    "data exists": () => data !== null,
    "has ticketId": () => typeof data?.ticketId === "number",
    "has eventId": () => typeof data?.eventId === "number",
    "has seatId": () => typeof data?.seatId === "number",
    "has userId": () => typeof data?.userId === "number",
  });

  if (res.status !== 200) {
    console.error(`❌ selectSeat failed [${res.status}]:`, JSON.stringify({
      status: res.status,
      message: json?.message,
      eventId,
      seatId,
    }));
  } else {
    // 성공 시 티켓 정보 로깅 (디버깅용)
    if (__ENV.DEBUG === "true") {
      console.log(`✅ Seat selected: eventId=${eventId}, seatId=${seatId}, ticketId=${data?.ticketId}`);
    }
  }

  return res;
}
