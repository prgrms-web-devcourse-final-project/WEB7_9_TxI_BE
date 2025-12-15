import { CONFIG as DEFAULT } from "./env.default.js";

export const CONFIG = {
  ...DEFAULT,
  // BASE_URL: "http://<EC2_PUBLIC_IP>:8080",
  // PROM_URL: "http://<EC2_PRIVATE_IP>:9090/api/v1/write",
};