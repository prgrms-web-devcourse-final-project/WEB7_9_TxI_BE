import http from "k6/http";
import { check } from "k6";

/**
 * POST /api/v1/auth/local/login
 * 이메일 + 비밀번호 기반 로그인 테스트 시나리오
 *
 * @param {string} baseUrl - API 기본 주소
 * @param {string} testId - 테스트 실행 ID (로그/메트릭 태깅용)
 * @param {string} email - 로그인 이메일
 * @param {string} password - 로그인 비밀번호
 */
export function login(baseUrl, testId, email, password) {
  const url = `${baseUrl}/api/v1/auth/local/login`;

  const payload = JSON.stringify({
    email,
    password,
  });

  const params = {
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    tags: {
      api: "login",
      test_id: testId,
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

  const data = json?.data ?? null;

  check(res, {
    "status 200": (r) => r.status === 200,
    "data exists": () => data !== null,
    "has accessToken": () => typeof data?.accessToken === "string",
    "has refreshToken": () => typeof data?.refreshToken === "string",
    "has userId": () => typeof data?.userId === "number",
  });

  return res;
}