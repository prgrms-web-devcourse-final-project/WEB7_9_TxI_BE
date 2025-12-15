import { login } from "../scenarios/login.js";

export const options = {
  vus: parseInt(__ENV.VUS || "10", 10),
  duration: __ENV.DURATION || "10s",
};

/**
 * Perf 더미데이터 규칙: PerfDataInitializer
 * - email: perf_user{i}@example.com
 * - password: pass{i}
 *
 * 기존 부하테스트와 동일하게 VU 기반으로 사용자 선택
 */
export function setup() {
  const users = Array.from({ length: 200 }, (_, i) => ({
    email: `perf_user${i + 1}@example.com`,
    password: `pass${i + 1}`,
  }));

  return {
    users,
    testId: new Date().toISOString().replace(/[:.]/g, "-"),
  };
}

export default function (data) {
  const baseUrl = __ENV.BASE_URL || "http://host.docker.internal:8080";

  // 1~200 사이 로그인 계정 라운드로빈
  const idx = ((__VU - 1) % 200);
  const { email, password } = data.users[idx];

  login(baseUrl, data.testId, email, password);
}