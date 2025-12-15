import http from "k6/http";
import { check } from "k6";

/**
 * GET /api/v1/rooms/{roomId}/comments?page={page}&size={size}
 * 각 유저가 속한 roomId를 기반으로 댓글 목록 조회
 */
export function getComments(baseUrl, jwt, testId, roomId, page = 0, size = 10) {
  const url = `${baseUrl}/api/v1/rooms/${roomId}/comments?page=${page}&size=${size}`;
  const params = {
    headers: {
      Authorization: `Bearer ${jwt}`,
      Accept: "application/json",
    },
    tags: { api: "getComments", test_id: testId, room_id: roomId },
  };

  const res = http.get(url, params);

  let json;
  try {
    json = res.json();
  } catch {
    console.error(`❌ JSON parse error (room=${roomId}):`, res.body);
    return res;
  }

  const content = json?.data?.content ?? [];
  const totalElements = json?.data?.totalElements ?? 0;

  check(res, {
    "status 200": (r) => r.status === 200,
    "data exists": () => json?.data !== undefined && json?.data !== null,
    "content is array": () => Array.isArray(content),
    "totalElements valid": () => typeof totalElements === "number",
    "content length >= 0": () => content.length >= 0,
  });

  if (!Array.isArray(content)) {
    console.warn(`⚠️ response shape mismatch (room=${roomId}):`, JSON.stringify(json));
  }

  return res;
}