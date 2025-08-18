import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 20,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<400'],
  },
};

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const SKU = 'SKU1';
const STORES = ['A', 'B', 'C', 'D'];

export default function () {
  const store = STORES[Math.floor(Math.random() * STORES.length)];
  const delta = Math.floor(Math.random() * 5) - 2; // -2..+2

  let res = http.post(`${BASE}/api/commands/inventory/adjust`, JSON.stringify({
    storeId: store, sku: SKU, delta: delta
  }), {headers: {'Content-Type': 'application/json'}});
  check(res, { 'adjust 2xx': (r) => r.status === 200 });

  // small pause for eventual delivery
  sleep(0.05);

  res = http.get(`${BASE}/api/query/inventory/global/${SKU}`);
  check(res, { 'global 2xx': (r) => r.status === 200 });
}

