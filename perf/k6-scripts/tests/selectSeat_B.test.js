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
 * 시나리오 B: Controlled Contention 테스트
 *
 * 목적:
 * - 좌석 경합이 시스템에 주는 비용 측정
 * - 락 대기, DB 커넥션 풀, Pending Threads 증가 원인 분석
 * - 실제 티켓팅에 가까운 경쟁 상황 재현
 *
 * 방법:
 * - 제한된 좌석 풀만 사용 (50석 고정)
 * - VU별로 순차적으로 다음 좌석 선택 (iteration마다 +1)
 * - 여러 VU가 비슷한 시간에 같은 좌석을 선택 시도
 *
 * 관찰 포인트:
 * - Pending Threads 증가 시점
 * - DB Connection Acquire Time
 * - 실패율 증가 곡선
 * - 409 Conflict 응답 비율
 *
 * 기대 결과:
 * - 실패율 증가 (정상)
 * - 시나리오 A 대비 TPS 감소
 * - 경합 비용 정량화
 *
 * 경쟁 좌석 수 조정:
 * - 50석: VIP석 수준 경쟁 (높은 경합) ← 현재 설정
 * - 100석: 일반석 수준 경쟁 (중간 경합)
 * - 200석: 낮은 경합
 * 변경이 필요하면 setup() 함수 내 hotSeats 값을 직접 수정하세요.
 *
 * 선택 패턴:
 * - 랜덤이 아닌 순차 선택 (VU별 시작 좌석 + iteration)
 * - 50석 범위 내에서 계속 순환하며 선택 시도
 * - 초반에 50석이 빠르게 소진되지만, 계속 순환하며 경합 발생
 * - 실패율 높음 (이미 선택된 좌석 선택 시 400/409 에러)
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

  const hotSeats = 50;

  console.log(`🔥 Competitive Test - HOT_SEATS: ${hotSeats} (경쟁 대상 좌석 수)`);
  console.log(`👥 Max VUs: ${maxVus} (최대 동시 사용자 수)`);
  console.log(`📊 순차 선택 패턴: 각 VU가 iteration마다 다음 좌석 선택 (경합 유지)`);

  return {
    tokens,
    maxVus,
    testId: new Date().toISOString().replace(/[:.]/g, "-"),
    hotSeats,
  };
}

export default function (data) {
  const baseUrl = __ENV.BASE_URL || "http://host.docker.internal:8080";

  // VU별 JWT 토큰 사용
  const jwt = data.tokens[(__VU - 1) % data.tokens.length];

  // Event #3 (OPEN 상태, 625석)
  const eventId = 3;

  // ✅ 제한된 좌석 풀에서 순차 선택 (경합 발생)
  // Event #3의 좌석 ID 범위: 1~625 (625석)
  // HOT_SEATS=50이면 1~50번 좌석만 사용 (처음 50석)
  // VU별 시작 좌석에서 iteration마다 +1하며 순차 선택
  // 여러 VU가 비슷한 시간에 같은 좌석을 선택하면서 경쟁 발생
  //
  // 예시 (maxVus=100, hotSeats=50):
  // VU 1, iter 0 → 좌석 1
  // VU 1, iter 1 → 좌석 2
  // VU 1, iter 50 → 좌석 1 (순환)
  // VU 2, iter 0 → 좌석 2
  // VU 51, iter 0 → 좌석 1 (순환, VU 1과 경합)
  const offset = (__VU - 1 + __ITER) % data.hotSeats;
  const seatId = offset + 1; // 1~50

  const selectRes = selectSeat(baseUrl, jwt, data.testId, eventId, seatId);

  // 사용자 반응 시간 랜덤화 (0.5~2.0초)
  // 실제 사용자처럼 행동하여 Pending Thread 과장 방지
  sleep(Math.random() * 1.5 + 0.5);

  // 좌석 선택에 성공했을 때만 취소 (재사용을 위해)
  // 실패한 경우 deselectSeat 호출 방지 (불필요한 에러 로그 제거)
  if (selectRes.status === 200) {
    deselectSeat(baseUrl, jwt, data.testId, eventId, seatId);
  }

  // 다음 선택 전 대기
  sleep(0.5);
}
