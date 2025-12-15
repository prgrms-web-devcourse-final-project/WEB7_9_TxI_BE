import http from "k6/http";
import { check } from "k6";

function randomString(length) {
  const chars = "abcdefghijklmnopqrstuvwxyz0123456789";
  let s = "";
  for (let i = 0; i < length; i++) {
    s += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return s;
}

export function signup(baseUrl, testId) {
  // 랜덤 ID 생성 (중복 방지)
  const rand = Math.floor(Math.random() * 10000000);

  const email = `user${rand}@test.com`;
  const password = "PerfUser123!";
  const nickname = randomString(8);

  const payload = JSON.stringify({
    email,
    password,
    nickname,
  });

  const headers = {
    "Content-Type": "application/json",
    Accept: "application/json",
  };

  const url = `${baseUrl}/api/v1/auth/local/signup`;

  const res = http.post(url, payload, {
    headers,
    tags: { api: "signup", test_id: testId },
  });

  let json;
  try {
    json = res.json();
  } catch {
    console.error("❌ JSON parse error:", res.body);
    return res;
  }

  const data = json?.data ?? null;

  check(res, {
    "signup status 201": (r) => r.status === 201,
    "response has data": () => data !== null,
    "has userId": () => typeof data?.userId === "number",
    "has email": () => typeof data?.email === "string",
    "has nickname": () => typeof data?.nickname === "string",
    "has tokens": () =>
      typeof data?.accessToken === "string" &&
      typeof data?.refreshToken === "string",
  });

  return res;
}