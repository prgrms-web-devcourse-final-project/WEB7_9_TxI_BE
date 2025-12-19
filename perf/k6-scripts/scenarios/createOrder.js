import http from "k6/http";
import { check } from "k6";

/**
 * POST /api/v1/order
 * 주문 생성 시나리오 (인증 필요)
 *
 * @param {string} baseUrl - API 기본 주소
 * @param {string} jwt - 사용자 JWT 토큰
 * @param {string} testId - 테스트 실행 ID (로그/메트릭 태깅용)
 * @param {number} eventId - 이벤트 ID
 * @param {number} seatId - 좌석 ID
 * @param {number} amount - 주문 총액
 */
export function createOrder(baseUrl, jwt, testId, eventId, seatId, amount) {
  const url = `${baseUrl}/api/v1/order`;

  const payload = JSON.stringify({
    amount: amount,
    eventId: eventId,
    seatId: seatId,
  });

  const params = {
    headers: {
      Authorization: `Bearer ${jwt}`,
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    tags: {
      api: "createOrder",
      test_id: testId,
      event_id: eventId,
      seat_id: seatId,
    },
  };

  const res = http.post(url, payload, params);

  let json;
  try {
    json = res.json();
  } catch {
    console.error("❌ JSON parse error:", res.body);
    return res;
  }

  // 서버 응답 구조: { message: string, data: OrderResponseDto }
  const data = json?.data ?? null;

  check(res, {
    "status 200": (r) => r.status === 200,
    "data exists": () => data !== null,
    "has orderId": () => typeof data?.orderId === "number",
    "has orderKey": () => typeof data?.orderKey === "string",
    "has ticketId": () => typeof data?.ticketId === "number",
    "has amount": () => typeof data?.amount === "number",
    "amount matches": () => data?.amount === amount,
  });

  if (res.status !== 200) {
    console.error(`❌ createOrder failed [${res.status}]:`, JSON.stringify({
      status: res.status,
      message: json?.message,
      eventId,
      seatId,
      amount,
    }));
  } else {
    // 성공 시 주문 정보 로깅 (디버깅용)
    if (__ENV.DEBUG === "true") {
      console.log(`✅ Order created: orderId=${data?.orderId}, orderKey=${data?.orderKey}, ticketId=${data?.ticketId}, amount=${data?.amount}`);
    }
  }

  return res;
}
