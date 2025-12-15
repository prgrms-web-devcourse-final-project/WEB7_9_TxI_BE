import { CONFIG as DEFAULT } from "./env.default.js";

export const CONFIG = {
  ...DEFAULT,
  BASE_URL: "http://host.docker.internal:8080",
  PROM_URL: "http://prometheus:9090/api/v1/write",
};