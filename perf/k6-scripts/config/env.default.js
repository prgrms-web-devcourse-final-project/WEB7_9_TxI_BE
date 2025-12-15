export const CONFIG = {
  BASE_URL: "http://host.docker.internal:8080",
  PROM_URL: "http://prometheus:9090/api/v1/write",
  JWT_SECRET: __ENV.JWT_SECRET || "defaultSecret",
  ENABLE_LOG: (__ENV.ENABLE_LOG || "false") === "true",
  ROOM_ID: parseInt(__ENV.ROOM_ID || "1", 10),
  VUS: parseInt(__ENV.VUS || "10", 10),
  DURATION: __ENV.DURATION || "10s",
};