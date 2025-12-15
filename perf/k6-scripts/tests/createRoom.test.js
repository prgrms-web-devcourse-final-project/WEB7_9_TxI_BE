import { createRoom } from "../scenarios/createRoom.js";
import { generateJWT } from "../util/jwt.js";

export const options = {
  vus: parseInt(__ENV.VUS || "10", 10),
  duration: __ENV.DURATION || "10s",
};

export function setup() {
  const secret = __ENV.JWT_SECRET;
  const vus = options.vus;

  const tokens = Array.from({ length: vus }, (_, i) =>
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
  createRoom(baseUrl, jwt, data.testId);
}