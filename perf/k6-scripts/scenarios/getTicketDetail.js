import http from "k6/http";
import { check } from "k6";

/**
 * GET /api/v1/tickets/my/{ticketId}/details
 * 티켓 상세 조회 시나리오 (인증 필요)
 *
 * 테스트 데이터:
 * - Event #4의 100석에 대해 ISSUED 티켓 생성됨
 * - ticketId 1~100: userId 1~100에게 각각 할당
 * - 본인의 티켓만 조회 가능 (다른 사용자 티켓 조회 시 에러)
 *
 * @param {string} baseUrl - API 기본 주소
 * @param {string} jwt - 사용자 JWT 토큰
 * @param {string} testId - 테스트 실행 ID (로그/메트릭 태깅용)
 * @param {number} ticketId - 조회할 티켓 ID
 */
export function getTicketDetail(baseUrl, jwt, testId, ticketId) {
  const url = `${baseUrl}/api/v1/tickets/my/${ticketId}/details`;

  const params = {
    headers: {
      Authorization: `Bearer ${jwt}`,
      Accept: "application/json",
    },
    tags: {
      api: "getTicketDetail",
      test_id: testId,
      ticket_id: ticketId,
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

  // 서버 응답 구조: { message: string, data: TicketResponse }
  const data = json?.data ?? null;

  check(res, {
    "status 200": (r) => r.status === 200,
    "data exists": () => data !== null,
    "data is object": () => typeof data === "object" && !Array.isArray(data),
  });

  // 티켓 상세 데이터 구조 검증
  if (res.status === 200 && data) {
    check(res, {
      "has ticketId": () => typeof data?.ticketId === "number",
      "has eventId": () => typeof data?.eventId === "number",
      "has eventTitle": () => typeof data?.eventTitle === "string",
      "has seatCode": () => typeof data?.seatCode === "string",
      "has seatGrade": () => typeof data?.seatGrade === "string",
      "has seatPrice": () => typeof data?.seatPrice === "number",
      "has seatStatus": () => typeof data?.seatStatus === "string",
      "has ticketStatus": () => typeof data?.ticketStatus === "string",
      "ticketId matches": () => data?.ticketId === ticketId,
    });
  }

  if (res.status !== 200) {
    console.error(`❌ getTicketDetail failed [${res.status}]:`, JSON.stringify({
      status: res.status,
      message: json?.message,
      ticketId,
    }));
  } else {
    // 성공 시 티켓 정보 로깅 (디버깅용)
    if (__ENV.DEBUG === "true") {
      console.log(`✅ Ticket detail retrieved: ticketId=${data?.ticketId}, seat=${data?.seatCode}, status=${data?.ticketStatus}`);
    }
  }

  return res;
}
