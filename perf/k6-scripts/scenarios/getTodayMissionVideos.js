import http from "k6/http";
import { check } from "k6";

/**
 * GET /api/v1/challenge/rooms/{roomId}/videos/today
 * - 특정 방의 오늘 미션 영상 목록 조회
 */
export function getTodayMissionVideos(baseUrl, jwt, testId, roomId) {
  const url = `${baseUrl}/api/v1/challenge/rooms/${roomId}/videos/today`;

  const params = {
    headers: {
      Authorization: `Bearer ${jwt}`,
      Accept: "application/json",
    },
    tags: { api: "getTodayMissionVideos", test_id: testId, room_id: String(roomId) },
  };

  const res = http.get(url, params);

  let json;
  try {
    json = res.json();
  } catch {
    console.error(`❌ JSON parse error (room=${roomId}):`, res.body);
    return res;
  }

  const isWrappedResponse = json?.data !== undefined;

  const videos = isWrappedResponse ? json.data : json;

  check(res, {
    "status 200": (r) => r.status === 200,
    "videos array exists": () => Array.isArray(videos),
    "videos length ≥ 0": () => videos.length >= 0,
  });

  if (res.status !== 200) {
    console.warn(`⚠️ Non-200 status (${res.status}) at room=${roomId}`);
  }

  return res;
}