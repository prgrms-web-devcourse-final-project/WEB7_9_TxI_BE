import http from "k6/http";
import { check } from "k6";

/**
 * GET /api/v1/tickets/my
 * 내 티켓 목록 조회 시나리오 (인증 필요)
 *
 * 테스트 데이터:
 * - Event #4의 100석에 대해 ISSUED 티켓 생성됨
 * - userId 1~100: 티켓 1장씩 보유
 * - userId 101 이상: 티켓 없음 (빈 배열 반환)
 *
 * @param {string} baseUrl - API 기본 주소
 * @param {string} jwt - 사용자 JWT 토큰
 * @param {string} testId - 테스트 실행 ID (로그/메트릭 태깅용)
 */
export function getMyTickets(baseUrl, jwt, testId) {
  const url = `${baseUrl}/api/v1/tickets/my`;

  const params = {
    headers: {
      Authorization: `Bearer ${jwt}`,
      Accept: "application/json",
    },
    tags: {
      api: "getMyTickets",
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

  // 서버 응답 구조: { message: string, data: List<TicketResponse> }
  const data = json?.data ?? null;

  check(res, {
    "status 200": (r) => r.status === 200,
    "data exists": () => data !== null,
    "data is array": () => Array.isArray(data),
    "tickets length ≥ 0": () => data.length >= 0,
  });

  // 티켓이 있을 경우 첫 번째 티켓 구조 검증
  if (Array.isArray(data) && data.length > 0) {
    const firstTicket = data[0];
    check(res, {
      "has ticketId": () => typeof firstTicket?.ticketId === "number",
      "has eventId": () => typeof firstTicket?.eventId === "number",
      "has eventTitle": () => typeof firstTicket?.eventTitle === "string",
      "has seatCode": () => typeof firstTicket?.seatCode === "string",
      "has seatGrade": () => typeof firstTicket?.seatGrade === "string",
      "has seatPrice": () => typeof firstTicket?.seatPrice === "number",
      "has seatStatus": () => typeof firstTicket?.seatStatus === "string",
      "has ticketStatus": () => typeof firstTicket?.ticketStatus === "string",
    });
  }

  if (res.status !== 200) {
    console.error(`❌ getMyTickets failed [${res.status}]:`, JSON.stringify({
      status: res.status,
      message: json?.message,
    }));
  } else {
    // 성공 시 티켓 개수 로깅 (디버깅용)
    if (__ENV.DEBUG === "true") {
      console.log(`✅ My tickets retrieved: ${data?.length || 0} tickets`);
    }
  }

  return res;
}
