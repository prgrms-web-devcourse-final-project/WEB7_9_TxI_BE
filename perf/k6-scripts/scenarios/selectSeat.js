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
    "has seatCode": () => typeof data?.seatCode === "string",
    "has seatGrade": () => typeof data?.seatGrade === "string",
    "has seatPrice": () => typeof data?.seatPrice === "number",
    "has seatStatus": () => typeof data?.seatStatus === "string",
    "has ticketStatus": () => typeof data?.ticketStatus === "string",
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

/**
 * DELETE /api/v1/events/{eventId}/seats/{seatId}/deselect
 * 좌석 선택 취소 시나리오 (인증 필요)
 *
 * @param {string} baseUrl - API 기본 주소
 * @param {string} jwt - 사용자 JWT 토큰
 * @param {string} testId - 테스트 실행 ID (로그/메트릭 태깅용)
 * @param {number} eventId - 이벤트 ID
 * @param {number} seatId - 취소할 좌석 ID
 */
export function deselectSeat(baseUrl, jwt, testId, eventId, seatId) {
  const url = `${baseUrl}/api/v1/events/${eventId}/seats/${seatId}/deselect`;

  const params = {
    headers: {
      Authorization: `Bearer ${jwt}`,
      Accept: "application/json",
    },
    tags: {
      api: "deselectSeat",
      test_id: testId,
      event_id: eventId,
      seat_id: seatId,
    },
  };

  const res = http.del(url, null, params);

  check(res, {
    "status 204": (r) => r.status === 204,
  });

  if (res.status !== 204) {
    let json;
    try {
      json = res.json();
    } catch {
      // 204는 body가 없으므로 에러가 정상
    }
    console.error(`❌ deselectSeat failed [${res.status}]:`, JSON.stringify({
      status: res.status,
      message: json?.message,
      eventId,
      seatId,
    }));
  } else {
    if (__ENV.DEBUG === "true") {
      console.log(`✅ Seat deselected: eventId=${eventId}, seatId=${seatId}`);
    }
  }

  return res;
}
