import { getEvent } from "../scenarios/getEvent.js";
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
 * 이벤트 단건 조회 부하 테스트
 * - 인증 불필요 (공개 API)
 * - 랜덤 eventId 조회로 다양한 이벤트 상세 페이지 조회 시뮬레이션
 * - 사용자가 목록에서 특정 이벤트를 클릭해서 상세 페이지를 보는 패턴
 */
export function setup() {
  return {
    testId: new Date().toISOString().replace(/[:.]/g, "-"),
  };
}

export default function (data) {
  const baseUrl = __ENV.BASE_URL || "http://host.docker.internal:8080";

  // 랜덤 이벤트 조회 (1~10 범위)
  // 실제 환경에 맞게 EVENT_ID_RANGE 환경변수로 조정
  const maxEventId = parseInt(__ENV.EVENT_ID_RANGE || "10", 10);
  const eventId = Math.floor(Math.random() * maxEventId) + 1;

  getEvent(baseUrl, data.testId, eventId);

  // 사용자가 이벤트 상세 페이지를 보는 시간 시뮬레이션
  sleep(1);
}
