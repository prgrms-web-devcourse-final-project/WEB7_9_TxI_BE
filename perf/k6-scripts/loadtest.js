import { sleep } from "k6";
import { CONFIG } from "./config.js";
import { generateJWT } from "./util/jwt.js";
import { createRoom, getVideosToday, postCompleteMission, getTodayMissions } from "./scenarios/index.js";

export const options = CONFIG.STAGES?.length
  ? { stages: CONFIG.STAGES }
  : { vus: CONFIG.VUS, duration: CONFIG.DURATION };

export function setup() {
  const secret = CONFIG.JWT_SECRET;
  const tokens = Array.from({ length: CONFIG.VUS }, (_, i) =>
    generateJWT({ id: i + 1, nickname: `PerfUser${i + 1}` }, secret)
  );
  const testId = __ENV.TEST_ID || new Date().toISOString().replace(/[:.]/g, "-");
  return { tokens, testId };
}




// export default function (data) {
//   const jwt = data.tokens[__VU - 1];
//   createRoom(CONFIG.BASE_URL, jwt, data.testId);
//   sleep(1);
// }

// export default function (data) {
//   const jwt = data.tokens[__VU - 1];
//   const testId = data.testId;
//   const r = Math.random();

//   if (r < 0.33) {
//     getVideosToday(CONFIG.BASE_URL, jwt, CONFIG.ROOM_ID, testId);
//   } else if (r < 0.66) {
//     getTodayMissions(CONFIG.BASE_URL, jwt, CONFIG.ROOM_ID, testId);
//   } else {
//     postCompleteMission(CONFIG.BASE_URL, jwt, CONFIG.ROOM_ID, testId);
//   }
//   sleep(1);
// }