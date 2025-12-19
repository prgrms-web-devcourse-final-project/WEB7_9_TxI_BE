import { getTicketDetail } from "../scenarios/getTicketDetail.js";
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
 * 티켓 상세 조회 부하 테스트
 *
 * 목적:
 * - 사용자가 마이페이지에서 특정 티켓의 상세 정보를 조회하는 패턴
 * - 티켓 상세 조회 성능 측정
 * - 권한 검증 및 응답 속도 확인
 *
 * 테스트 데이터 구조 (Event #4 기준):
 * - 총 사용자: 500명 (test1@test.com ~ test500@test.com)
 * - Event #4: CLOSED 상태, 500석 (모두 ISSUED 티켓)
 * - 티켓 배정: 순환 배정
 *   → ticketId 1: userId 1
 *   → ticketId 2: userId 2
 *   → ...
 *   → ticketId 500: userId 500
 *
 * 시나리오:
 * - VU 1~500만 활성화 (티켓을 가진 사용자만 테스트)
 * - 각 VU는 자신의 티켓 ID로 상세 조회
 * - VU 1 → ticketId 1, VU 2 → ticketId 2, ...
 *
 * 특징:
 * - 읽기 전용 작업으로 실패율 낮음
 * - 권한 검증 로직 포함 (본인 티켓만 조회 가능)
 * - DB 단건 조회 성능 확인
 *
 * 주의:
 * - PEAK_VUS는 500 이하로 설정 권장 (티켓이 500장만 존재)
 * - 500 초과 시 존재하지 않는 ticketId 조회로 404 에러 발생
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

  // 티켓이 있는 사용자(1~500)만 토큰 생성
  const ticketOwnerCount = 500;
  const effectiveVus = Math.min(maxVus, ticketOwnerCount);

  // VU당 JWT 토큰 생성
  const tokens = Array.from({ length: effectiveVus }, (_, i) => {
    const userId = i + 1; // userId 1~500
    return generateJWT(
      {
        id: userId,
        email: `test${userId}@test.com`,
        nickname: `PerfUser${userId}`
      },
      secret
    );
  });

  console.log(`Testing with ${effectiveVus} users (ticket owners only)`);
  console.log(`Event #4 티켓 상세 조회:`);
  console.log(`   - ticketId 1~${effectiveVus}: userId 1~${effectiveVus}`);

  if (maxVus > ticketOwnerCount) {
    console.warn(`⚠️  PEAK_VUS(${maxVus})가 티켓 수(${ticketOwnerCount})보다 큽니다.`);
    console.warn(`   ${ticketOwnerCount}명의 사용자만 테스트됩니다.`);
  }

  return {
    tokens,
    testId: new Date().toISOString().replace(/[:.]/g, "-"),
    effectiveVus,
  };
}

export default function (data) {
  const baseUrl = __ENV.BASE_URL || "http://host.docker.internal:8080";

  // VU별 고유 JWT 토큰 사용
  const vuIndex = (__VU - 1) % data.effectiveVus;
  const jwt = data.tokens[vuIndex];

  // VU 번호와 ticketId를 1:1 매칭
  // VU 1 → ticketId 1, VU 2 → ticketId 2, ...
  const ticketId = vuIndex + 1;

  getTicketDetail(baseUrl, jwt, data.testId, ticketId);

  // 사용자가 티켓 상세를 확인하는 시간 (0.5~2.0초)
  sleep(Math.random() * 1.5 + 0.5);
}