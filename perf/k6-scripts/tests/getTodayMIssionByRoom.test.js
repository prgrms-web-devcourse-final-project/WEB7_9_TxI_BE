import { generateJWT } from "../util/jwt.js";
import { getTodayMission } from "../scenarios/getTodayMissionByRoom.js";

export const options = {
  vus: parseInt(__ENV.VUS || "10", 10),
  duration: __ENV.DURATION || "10s",
};

export function setup() {
  const secret = __ENV.JWT_SECRET;

  const tokens = Array.from({ length: options.vus }, (_, i) => {
    const userId = i + 1;

    return {
      jwt: generateJWT(
        { id: userId, nickname: `PerfUser${userId}` },
        secret
      ),
      userId,
    };
  });

  return {
    tokens,
    testId: new Date().toISOString().replace(/[:.]/g, "-"),
  };
}

export default function (data) {
  const baseUrl = __ENV.BASE_URL || "http://host.docker.internal:8080";

  const { jwt, userId } = data.tokens[__VU - 1];

  getTodayMission(baseUrl, jwt, userId, data.testId);
}