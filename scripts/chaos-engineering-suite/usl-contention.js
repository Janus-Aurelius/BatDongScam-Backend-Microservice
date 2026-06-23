import http from 'k6/http';
import { check, sleep } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

// ==============================================================================
// USL Contention Load Test (k6)
// Target: bds-core-macroservice Connection Pool Saturation
// ==============================================================================

const GATEWAY_URL = __ENV.GATEWAY_URL || 'http://localhost:8088';

export const options = {
  scenarios: {
    contention_scenario: {
      executor: 'ramping-vus',
      startVUs: 10,
      stages: [
        { duration: '60s', target: 100 },  // Initial ramp-up
        { duration: '180s', target: 500 }, // Aggressive spike to trigger pool thrashing
        { duration: '60s', target: 0   },   // Cool down
      ],
      gracefulRampDown: '15s',
    },
  },
  thresholds: {
    // We expect the system to degrade under USL noisy neighbor, but monitor the drop
    'http_req_failed': ['rate < 0.20'], // Tolerable error boundary during chaos
    'http_req_duration': ['p(95) < 3000'], // SLA tolerance limits
  },
};

export function setup() {
  console.log('[SETUP] Logging in Sales Agent to obtain JWT authorization...');
  
  const loginPayload = JSON.stringify({
    email:      'agent@bds.com',
    password:   'password123',
    rememberMe: true,
  });

  const loginRes = http.post(`${GATEWAY_URL}/api/auth/login`, loginPayload, {
    headers: { 'Content-Type': 'application/json' },
  });

  if (loginRes.status !== 200) {
    throw new Error(`Login failed: ${loginRes.status} ${loginRes.body}`);
  }

  const token = loginRes.json().token;
  console.log('[SETUP] Sales Agent authenticated. Fetching property candidates...');

  // Fetch available properties to run valid write scenarios
  let propertyIds = [];
  const searchRes = http.get(`${GATEWAY_URL}/public/properties/search?page=0&size=50`);
  if (searchRes.status === 200) {
    const content = searchRes.json().content || [];
    content.forEach((p) => {
      if (p.id) propertyIds.push(p.id);
    });
  }

  if (propertyIds.length === 0) {
    console.log('[SETUP] Warning: No properties found in DB. Using synthetic UUIDs for validation.');
    propertyIds = ['8f0a0c64-42b1-4c12-a16f-9988cc654a11', '8f0a0c64-42b1-4c12-a16f-9988cc654a12'];
  }

  return { token, propertyIds };
}

export default function (data) {
  const headers = {
    'Content-Type':  'application/json',
    'Authorization': `Bearer ${data.token}`,
    'X-Load-Test':   'true',
    'X-Test-Scenario': 'usl_contention',
  };

  const propertyId = data.propertyIds[Math.floor(Math.random() * data.propertyIds.length)];
  const customerId = 'f0df88b1-3d7b-40f1-bbd5-ccdb7b4fd5d1'; // Sample customer UUID

  const payload = JSON.stringify({
    propertyId: propertyId,
    customerId: customerId,
    monthlyRentAmount: Math.floor(Math.random() * 20000000) + 5000000,
    securityDepositAmount: Math.floor(Math.random() * 40000000) + 10000000,
    durationMonths: 12,
    paymentCycleMonths: 1,
    expectedStartDate: '2027-01-01',
    note: 'USL load test contract creation'
  });

  const res = http.post(`${GATEWAY_URL}/contracts/rental`, payload, { headers });

  check(res, {
    'HTTP status is 200, 400, or 409': (r) => [200, 400, 409].includes(r.status),
    'HTTP status is not 500': (r) => r.status !== 500,
  });

  // Short dynamic sleep (50ms - 200ms) to ensure high throughput and rapid queue filling
  sleep(Math.random() * 0.15 + 0.05);
}

export function handleSummary(data) {
  console.log('Writing USL Contention summary report...');
  return {
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
    'scripts/chaos-engineering-suite/k6-summary-usl.txt': textSummary(data, { indent: ' ', enableColors: false }),
  };
}
