import { getRooms } from "../scenarios/getRooms.js";
import { generateJWT } from "../util/jwt.js";

export const options = {
  vus: parseInt(__ENV.VUS || "10", 10),
  duration: __ENV.DURATION || "10s",
};

export function setup() {
  const secret = __ENV.JWT_SECRET;
  const tokens = Array.from({ length: options.vus }, (_, i) =>
    generateJWT({ id: i + 1, nickname: `PerfUser${i + 1}` }, secret)
  );
  return {
    tokens,
    testId: new Date().toISOString().replace(/[:.]/g, "-"),
  };
}

export default function (data) {
  const jwt = data.tokens[__VU - 1];
  const baseUrl = __ENV.BASE_URL || "http://host.docker.internal:8080";
  const roomId = parseInt(__ENV.ROOM_ID || "1", 10);

  getRooms(baseUrl, jwt, data.testId, 0, 10);
}