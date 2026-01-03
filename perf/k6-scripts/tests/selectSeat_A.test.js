import { selectSeat, deselectSeat } from "../scenarios/selectSeat.js";
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
 * 시나리오 A: Non-Competitive Baseline 테스트
 *
 * 목적:
 * - 좌석 선택 로직 자체의 순수 처리 한계 측정
 * - DB 트랜잭션, 비즈니스 로직의 이론적 최대 처리량 파악
 * - 락 경합 배제
 *
 * 방법:
 * - VU별로 고유한 좌석 ID 할당 (__VU를 이용한 고정 할당)
 * - 절대 같은 좌석을 선택하지 않음
 * - 실패율 ≈ 0% 예상
 *
 * 기대 결과:
 * - 이 테스트의 TPS = 시스템의 이론적 최대 처리량 (베이스라인)
 * - 이후 경합 테스트와 비교를 위한 기준선 설정
 *
 * 시나리오 패턴:
 * - select → (고민 시간) → deselect → (대기) → 반복
 * - 좌석을 재사용하므로 무한 iteration 가능
 * - VU 수 제한 없음 (625를 초과해도 순환으로 처리)
 *
 * 주의:
 * - select와 deselect가 쌍으로 동작하여 좌석 재사용
 * - 실제 사용자가 좌석을 보고 취소하는 패턴 시뮬레이션
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

  return {
    tokens,
    testId: new Date().toISOString().replace(/[:.]/g, "-"),
  };
}

export default function (data) {
  const baseUrl = __ENV.BASE_URL || "http://host.docker.internal:8080";

  // VU별 JWT 토큰 사용
  const jwt = data.tokens[(__VU - 1) % data.tokens.length];

  // Event #3 (OPEN 상태, 625석)
  const eventId = 3;

  // ✅ VU별 고유 좌석 할당 (경합 없음)
  // Event #3의 좌석 ID 범위: 1~625 (625석)
  // VU 1 → 좌석 1, VU 2 → 좌석 2, ..., VU 625 → 좌석 625
  // VU가 625를 초과하면 순환 (VU 626 → 좌석 1)
  // select → deselect 패턴으로 좌석 재사용
  const totalSeats = 625;
  const seatId = ((__VU - 1) % totalSeats) + 1; // 1~625

  // 좌석 선택
  const selectRes = selectSeat(baseUrl, jwt, data.testId, eventId, seatId);

  // 사용자가 좌석을 확인하고 고민하는 시간 시뮬레이션 (0.5~2.0초)
  sleep(Math.random() * 1.5 + 0.5);

  // 좌석 선택에 성공했을 때만 취소 (재사용을 위해)
  // 실패한 경우 deselectSeat 호출 방지 (불필요한 에러 로그 제거)
  if (selectRes.status === 200) {
    deselectSeat(baseUrl, jwt, data.testId, eventId, seatId);
  }

  // 다음 선택 전 대기
  sleep(0.5);
}
