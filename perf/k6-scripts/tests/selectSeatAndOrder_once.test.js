import { selectSeat } from "../scenarios/selectSeat.js";
import { createOrder } from "../scenarios/createOrder.js";
import { generateJWT } from "../util/jwt.js";

export const options = {
  // ✅ 각 VU가 1번만 실행하도록 설정
  iterations: parseInt(__ENV.VUS || "100", 10),
  vus: parseInt(__ENV.VUS || "100", 10),
};

/**
 * 좌석 선택 + 주문 생성 통합 부하 테스트 (1회 실행 버전)
 *
 * 목적:
 * - 실제 티켓팅 플로우를 시뮬레이션 (좌석 선택 → 주문 생성)
 * - 백엔드 멱등성 수정 전 임시 테스트용
 * - 각 VU가 정확히 1번만 실행
 *
 * 테스트 데이터 구조:
 * - Event #3 (뉴진스 2025 콘서트 - 서울, OPEN 상태)
 * - 총 좌석: 500석
 *   - VIP (A1~A50, 50석): 165,000원
 *   - R (C1~C100, 100석): 145,000원
 *   - S (B1~B150, 150석): 129,000원
 *   - A (D1~D200, 200석): 99,000원
 *
 * 시나리오:
 * - VU별로 고유한 좌석 할당 (경합 없음)
 * - 각 VU는 1번만 실행 (반복 없음)
 * - VU 1 → 좌석 1 (VIP), VU 2 → 좌석 2 (VIP), ...
 *
 * 주의:
 * - VUS는 500 이하로 설정 권장 (좌석이 500개만 존재)
 * - 백엔드의 멱등성 수정 후에는 selectSeatAndOrder.test.js 사용
 */
export function setup() {
  const secret = __ENV.JWT_SECRET;
  if (!secret) {
    throw new Error("JWT_SECRET 환경변수가 필요합니다.");
  }

  const vus = parseInt(__ENV.VUS || "100", 10);

  // VU당 JWT 토큰 생성
  const tokens = Array.from({ length: vus }, (_, i) => {
    const userId = i + 1;
    return generateJWT(
      {
        id: userId,
        email: `test${userId}@test.com`,
        nickname: `PerfUser${userId}`
      },
      secret
    );
  });

  console.log(`Testing with ${vus} VUs (1 iteration each)`);
  console.log(`좌석 선택 + 주문 생성 통합 테스트 (1회 실행):`);
  console.log(`   - Event #3 (뉴진스 2025 콘서트 - 서울)`);
  console.log(`   - 총 500석 (VIP 50, R 100, S 150, A 200)`);
  console.log(`   - VU별 고유 좌석 할당 (경합 없음)`);
  console.log(`   - ⚠️  백엔드 멱등성 수정 전 임시 버전`);

  if (vus > 500) {
    console.warn(`⚠️  VUS(${vus})가 좌석 수(500)보다 큽니다.`);
    console.warn(`   좌석 ID가 순환됩니다.`);
  }

  return {
    tokens,
    testId: new Date().toISOString().replace(/[:.]/g, "-"),
  };
}

/**
 * Event #3의 좌석 ID로 가격 계산
 */
function getSeatPrice(seatId) {
  if (seatId >= 1 && seatId <= 50) {
    return 165000; // VIP (A1~A50)
  } else if (seatId >= 51 && seatId <= 150) {
    return 145000; // R (C1~C100)
  } else if (seatId >= 151 && seatId <= 300) {
    return 129000; // S (B1~B150)
  } else if (seatId >= 301 && seatId <= 500) {
    return 99000; // A (D1~D200)
  }
  return 99000;
}

export default function (data) {
  const baseUrl = __ENV.BASE_URL || "http://host.docker.internal:8080";

  // VU별 JWT 토큰 사용
  const jwt = data.tokens[(__VU - 1) % data.tokens.length];

  // Event #3 (OPEN 상태, 500석)
  const eventId = 3;

  // VU별 고유 좌석 할당
  const totalSeats = 500;
  const seatId = ((__VU - 1) % totalSeats) + 1;

  // 좌석 ID에 따른 가격 계산
  const amount = getSeatPrice(seatId);

  // 1. 좌석 선택
  const selectRes = selectSeat(baseUrl, jwt, data.testId, eventId, seatId);

  // 좌석 선택 성공 시에만 주문 생성
  if (selectRes.status === 200) {
    // 2. 주문 생성
    createOrder(baseUrl, jwt, data.testId, eventId, seatId, amount);
  } else {
    if (__ENV.DEBUG === "true") {
      console.log(`⚠️  Seat selection failed for seatId=${seatId}, skipping order creation`);
    }
  }
}
