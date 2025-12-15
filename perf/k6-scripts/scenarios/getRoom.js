import http from "k6/http";
import { check } from "k6";

export function getRoom(baseUrl, jwt, roomId, testId) {
  const url = `${baseUrl}/api/v1/challenge/rooms/${roomId}`;
  const res = http.get(url, {
    headers: {
      Authorization: `Bearer ${jwt}`,
      Accept: "application/json",
    },
    tags: { api: "getRoom", test_id: testId },
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
    "getRoom 200": (r) => r.status === 200,
    "data exists": () => data !== null,
    "has id": () => typeof data?.id === "number",
    "has title": () => typeof data?.title === "string",
    "has participants": () =>
      Array.isArray(data?.participants) || data?.participants === undefined,
  });

  if (!data || typeof data !== "object") {
    console.warn("⚠️ Unexpected response shape:", JSON.stringify(json));
  }

  return res;
}