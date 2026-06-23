import http from 'k6/http';
import { check, sleep } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';
import { Counter } from 'k6/metrics';

// ==============================================================================
// CALM Concurrent Retry Storm Test (k6)
// Target: bds-core-macroservice Optimistic Locking & Mutability Safety
// ==============================================================================

const GATEWAY_URL = __ENV.GATEWAY_URL || 'http://localhost:8088';

const status200 = new Counter('status_200');
const status409 = new Counter('status_409');
const status500 = new Counter('status_500');

export const options = {
  scenarios: {
    retry_storm: {
      executor: 'per-vu-iterations',
      vus: 30,
      iterations: 1, // Exactly 30 requests in total
      maxDuration: '20s',
    },
  },
  thresholds: {
    // Assert exactly 1 successful update transition
    'status_200': ['count == 1'],
    // Assert exactly 29 collision rejections (409 Conflict)
    'status_409': ['count == 29'],
    // Assert zero unhandled internal server failures
    'status_500': ['count == 0'],
  },
};

export function setup() {
  console.log('[SETUP] Authenticating agent...');
  
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
  const authHeaders = {
    'Content-Type':  'application/json',
    'Authorization': `Bearer ${token}`,
  };

  console.log('[SETUP] Resolving an available property...');
  let propertyId = '8f0a0c64-42b1-4c12-a16f-9988cc654a11'; // Fallback
  const searchRes = http.get(`${GATEWAY_URL}/public/properties/search?page=0&size=50`);
  if (searchRes.status === 200) {
    const content = searchRes.json().content || [];
    const availableProperty = content.find(p => p.status === 'AVAILABLE');
    if (availableProperty && availableProperty.id) {
      propertyId = availableProperty.id;
    } else if (content.length > 0 && content[0].id) {
      propertyId = content[0].id;
    }
  }

  console.log(`[SETUP] Creating a single DRAFT contract on propertyId=${propertyId}...`);
  const contractPayload = JSON.stringify({
    propertyId: propertyId,
    customerId: 'f0df88b1-3d7b-40f1-bbd5-ccdb7b4fd5d1',
    monthlyRentAmount: 12000000.00,
    securityDepositAmount: 24000000.00,
    durationMonths: 12,
    paymentCycleMonths: 1,
    expectedStartDate: '2027-01-01',
    note: 'CALM validation draft contract'
  });

  const contractRes = http.post(`${GATEWAY_URL}/contracts/rental`, contractPayload, { headers: authHeaders });
  if (contractRes.status !== 200) {
    throw new Error(`Draft contract creation failed: ${contractRes.status} ${contractRes.body}`);
  }

  const contractId = contractRes.json().id;
  console.log(`[SETUP] Draft contract created: contractId=${contractId}`);

  return { token, contractId };
}

export default function (data) {
  // Spread the 50 requests randomly over a 3-second window to create a high-collision queue storm
  sleep(Math.random() * 3.0);

  const headers = {
    'Content-Type':  'application/json',
    'Authorization': `Bearer ${data.token}`,
    'X-Load-Test':   'true',
    'X-Test-Scenario': 'calm_retry_storm',
  };

  const url = `${GATEWAY_URL}/contracts/rental/${data.contractId}/approve`;
  const res = http.post(url, null, { headers });

  // Update counters based on response status
  if (res.status === 200) status200.add(1);
  else if (res.status === 409) status409.add(1);
  else if (res.status === 500) status500.add(1);

  check(res, {
    'CALM: Status is 200 or 409': (r) => [200, 409].includes(r.status),
    'CALM: Status is not 500': (r) => r.status !== 500,
  });
}

export function handleSummary(data) {
  console.log('Writing CALM Collision summary report...');
  return {
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
    'scripts/chaos-engineering-suite/k6-summary-calm.txt': textSummary(data, { indent: ' ', enableColors: false }),
  };
}
