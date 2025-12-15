import http from "k6/http";
import { check } from "k6";

/**
 * ✅ createRoom 시나리오
 * - API: POST /api/v1/challenge/rooms
 * - 요청 본문과 응답 구조 검증 강화
 */
export function createRoom(baseUrl, jwt, testId) {
  const payload = JSON.stringify({
    title: `부하테스트방-${__VU}-${__ITER}`,
    description: "자동 생성된 테스트용 방",
    capacity: 50, // 참가자 수 제한
    duration: 7,  // 기간 (일수)
    videoUrl: "https://www.youtube.com/watch?v=2fpek3wzSZo",
    imageFileName: "perf_room_test.jpg",
    contentType: "image/jpeg",
  });

  const url = `${baseUrl}/api/v1/challenge/rooms`;
  const res = http.post(url, payload, {
    headers: {
      Authorization: `Bearer ${jwt}`,
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    tags: { api: "createRoom", test_id: testId },
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
    "createRoom 200": (r) => r.status === 200,
    "data exists": () => data !== null,
    "has id": () => typeof data?.id === "number",
    "has title": () => typeof data?.title === "string",
    "has createdAt": () =>
      typeof data?.createdAt === "string" || data?.createdAt === undefined,
  });

  if (!data || typeof data !== "object") {
    console.warn("⚠️ Unexpected response shape:", JSON.stringify(json));
  }

  return res;
}