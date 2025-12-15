import http from "k6/http";
import { check } from "k6";

/**
 * POST /api/v1/challenge/participants/{roomId}/join
 */
export function joinChallengeRoom(baseUrl, jwt, testId, roomId) {
  const url = `${baseUrl}/api/v1/challenge/participants/${roomId}/join`;
  const params = {
    headers: {
      Authorization: `Bearer ${jwt}`,
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    tags: { api: "joinChallengeRoom", test_id: testId, room_id: roomId },
  };

  const res = http.post(url, null, params);

  let json;
  try {
    json = res.json();
  } catch {
    console.error(`❌ JSON parse error (room=${roomId}):`, res.body);
    return res;
  }

  const msg = json?.msg ?? "";

  check(res, {
    "status 200": (r) => r.status === 200,
    "message present": () => typeof msg === "string" && msg.length >= 0,
  });

  if (res.status >= 400) {
    console.warn(`⚠️ joinChallengeRoom failed (${roomId}): ${res.status} - ${msg}`);
  }

  return res;
}