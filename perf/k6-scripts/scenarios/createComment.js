import http from "k6/http";
import { check } from "k6";

/**
 * POST /api/v1/rooms/{roomId}/comments
 * 특정 운동방에 댓글을 작성하는 시나리오
 *
 * @param {string} baseUrl - API 기본 주소
 * @param {string} jwt - 사용자별 JWT 토큰
 * @param {string} testId - 테스트 실행 ID (로그/메트릭 태깅용)
 * @param {number} roomId - 운동방 ID
 * @param {string} [content] - 댓글 내용 (없으면 랜덤 문자열 자동 생성)
 */
export function createComment(baseUrl, jwt, testId, roomId, content) {
  const url = `${baseUrl}/api/v1/rooms/${roomId}/comments`;
  const payload = JSON.stringify({
    content: content || `테스트 댓글-${Math.random().toString(36).substring(2, 8)}`,
  });

  const params = {
    headers: {
      Authorization: `Bearer ${jwt}`,
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    tags: { api: "createComment", test_id: testId, room_id: roomId },
  };

  const res = http.post(url, payload, params);

  let json;
  try {
    json = res.json();
  } catch {
    console.error(`❌ JSON parse error (room=${roomId}):`, res.body);
    return res;
  }

  const data = json?.data ?? {};

  check(res, {
    "status 200 or 201": (r) => r.status === 200 || r.status === 201,
    "data exists": () => data !== null && data !== undefined,
    "has content": () => typeof data.content === "string",
    "has id": () => typeof data.id === "number",
  });

  if (res.status >= 400) {
    console.warn(`⚠️ createComment failed [room=${roomId}]: ${res.status} ${res.body}`);
  }

  return res;
}