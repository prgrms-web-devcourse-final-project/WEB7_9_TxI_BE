import { getEvents } from "../scenarios/getEvents.js";
import { sleep } from "k6";

export const options = {
  stages: [
    { duration: "10s", target: parseInt(__ENV.RAMP_UP_VUS || "50", 10) },   // 10초 동안 50명으로 증가
    { duration: "1m", target: parseInt(__ENV.RAMP_UP_VUS || "50", 10) },     // 1분간 50명 유지
    { duration: "10s", target: parseInt(__ENV.PEAK_VUS || "100", 10) },      // 10초 동안 100명으로 증가 (피크)
    { duration: "1m", target: parseInt(__ENV.PEAK_VUS || "100", 10) },       // 1분간 100명 유지
    { duration: "1m", target: 0 },                                           // 1분 동안 0으로 감소
  ],
};

/**
 * 이벤트 목록 조회 부하 테스트
 * - 인증 불필요 (공개 API)
 * - 페이지 랜덤 조회로 다양한 캐시 패턴 테스트
 */
export function setup() {
  return {
    testId: new Date().toISOString().replace(/[:.]/g, "-"),
  };
}

export default function (data) {
  const baseUrl = __ENV.BASE_URL || "http://host.docker.internal:8080";

  // 다양한 페이지 조회 (0~9 페이지 랜덤)
  const page = Math.floor(Math.random() * 10);
  const size = 10;

  // 옵션: 필터 사용 여부 (20% 확률로 status 필터 적용)
  const useStatusFilter = Math.random() < 0.2;
  const status = useStatusFilter ? ["READY", "PRE_OPEN", "OPEN"][Math.floor(Math.random() * 3)] : null;

  getEvents(baseUrl, data.testId, page, size, status, null);

  sleep(1);
}