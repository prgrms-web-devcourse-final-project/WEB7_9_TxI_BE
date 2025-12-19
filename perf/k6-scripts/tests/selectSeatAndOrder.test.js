import { selectSeat } from "../scenarios/selectSeat.js";
import { createOrder } from "../scenarios/createOrder.js";
import { generateJWT } from "../util/jwt.js";
import { sleep } from "k6";

export const options = {
  stages: [
    { duration: "10s", target: parseInt(__ENV.RAMP_UP_VUS || "50", 10) },
    { duration: "1m", target: parseInt(__ENV.RAMP_UP_VUS || "50", 10) },
    { duration: "10s", target: parseInt(__ENV.PEAK_VUS || "100", 10) },
    { duration: "1m", target: parseInt(__ENV.PEAK_VUS || "100", 10) },
    { duration: "1m", target: 0 },
  ],
};

/**
 * 좌석 선택 + 주문 생성 통합 부하 테스트
 *
 * 목적:
 * - 실제 티켓팅 플로우를 시뮬레이션 (좌석 선택 → 주문 생성)
 * - 두 API의 연계 동작 성능 측정
 * - 전체 트랜잭션 처리 시간 확인
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
 * - VU별로 고유한 좌석 할당 (경합 없음 - Baseline)
 * - VU 1 → 좌석 1 (VIP), VU 2 → 좌석 2 (VIP), ...
 * - VU 51 → 좌석 51 (R), VU 152 → 좌석 152 (S), ...
 * - 각 VU는 다음 플로우를 수행:
 *   1. 좌석 선택 (selectSeat)
 *   2. 선택 성공 시 주문 생성 (createOrder)
 *   3. 주문 생성 성공 시 완료
 *
 * 특징:
 * - Non-Competitive 테스트 (좌석 중복 없음)
 * - 실제 사용자 플로우 재현
 * - 두 API의 순차 호출 성능 측정
 * - DB 트랜잭션 연계 처리 확인
 *
 * 주의:
 * - PEAK_VUS는 500 이하로 설정 권장 (좌석이 500개만 존재)
 * - 500 초과 시 좌석이 순환되어 중복 가능
 * - selectSeat 실패 시 createOrder를 호출하지 않음
 */
export function setup() {
  const secret = __ENV.JWT_SECRET;
  if (!secret) {
    throw new Error("JWT_SECRET 환경변수가 필요합니다.");
  }

  const maxVus = Math.max(
    parseInt(__ENV.RAMP_UP_VUS || "50", 10),
    parseInt(__ENV.PEAK_VUS || "100", 10)
  );

  // VU당 JWT 토큰 생성
  const tokens = Array.from({ length: maxVus }, (_, i) => {
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

  console.log(`Testing with ${maxVus} VUs`);
  console.log(`좌석 선택 + 주문 생성 통합 테스트:`);
  console.log(`   - Event #3 (뉴진스 2025 콘서트 - 서울)`);
  console.log(`   - 총 500석 (VIP 50, R 100, S 150, A 200)`);
  console.log(`   - VU별 고유 좌석 할당 (경합 없음)`);

  if (maxVus > 500) {
    console.warn(`⚠️  PEAK_VUS(${maxVus})가 좌석 수(500)보다 큽니다.`);
    console.warn(`   좌석 ID가 순환됩니다.`);
  }

  return {
    tokens,
    testId: new Date().toISOString().replace(/[:.]/g, "-"),
  };
}

/**
 * Event #3의 좌석 ID로 가격 계산
 * - 좌석 1~50: VIP 165,000원
 * - 좌석 51~150: R 145,000원
 * - 좌석 151~300: S 129,000원
 * - 좌석 301~500: A 99,000원
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
  // 기본값 (범위 밖이면 최저가)
  return 99000;
}

export default function (data) {
  const baseUrl = __ENV.BASE_URL || "http://host.docker.internal:8080";

  // VU별 JWT 토큰 사용
  const jwt = data.tokens[(__VU - 1) % data.tokens.length];

  // Event #3 (OPEN 상태, 500석)
  const eventId = 3;

  // VU별 고유 좌석 할당 (경합 없음)
  // VU 1 → 좌석 1, VU 2 → 좌석 2, ..., VU 500 → 좌석 500
  const totalSeats = 500;
  const seatId = ((__VU - 1) % totalSeats) + 1;

  // 좌석 ID에 따른 가격 계산
  const amount = getSeatPrice(seatId);

  // 1. 좌석 선택
  const selectRes = selectSeat(baseUrl, jwt, data.testId, eventId, seatId);

  // 좌석 선택 성공 시에만 주문 생성
  if (selectRes.status === 200) {
    // 사용자가 좌석 선택 후 주문 버튼을 누르기까지의 시간 (0.3~1.0초)
    sleep(Math.random() * 0.7 + 0.3);

    // 2. 주문 생성
    createOrder(baseUrl, jwt, data.testId, eventId, seatId, amount);
  } else {
    // 좌석 선택 실패 시 로깅
    if (__ENV.DEBUG === "true") {
      console.log(`⚠️  Seat selection failed for seatId=${seatId}, skipping order creation`);
    }
  }

  // 사용자가 다음 행동을 하기까지의 대기 시간 (0.5~2.0초)
  sleep(Math.random() * 1.5 + 0.5);
}
