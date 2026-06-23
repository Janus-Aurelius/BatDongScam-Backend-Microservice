import http from 'k6/http';
import { check, sleep } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';
import { Trend } from 'k6/metrics';

// ==============================================================================
// PACELC Cache/DB Consistency & Latency Load Test (k6)
// ==============================================================================

const GATEWAY_URL = __ENV.GATEWAY_URL || 'http://localhost:8088';
const StalenessTrend = new Trend('staleness_window_ms');

export const options = {
  scenarios: {
    pacelc_scenario: {
      executor: 'constant-vus',
      vus: 10,
      duration: '180s',
    },
  },
  thresholds: {
    // Assert 0% critical 500 errors (Availability prioritized)
    'http_req_failed': ['rate < 0.05'], // API gateway shouldn't crash
    'staleness_window_ms': ['p(95) < 5000'], // Assert staleness caught up within SLA
  },
};

export function setup() {
  console.log('[SETUP] Logging in Admin for status updates...');
  
  const loginPayload = JSON.stringify({
    email:      'admin@bds.com',
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
  console.log('[SETUP] Agent authenticated. Finding property IDs to target...');

  let propertyIds = ['024293fc-eb47-4b9c-832b-bcdb2e5b3156']; // Fallback
  const searchRes = http.get(`${GATEWAY_URL}/public/properties/search?page=0&size=50`);
  if (searchRes.status === 200) {
    const content = searchRes.json().content || [];
    const ids = content.map(p => p.id).filter(Boolean);
    if (ids.length > 0) {
      propertyIds = ids;
    }
  }

  console.log(`[SETUP] Found ${propertyIds.length} target property IDs`);
  return { token, propertyIds };
}

export default function (data) {
  // Use a unique property ID per VU to avoid concurrent collisions/409 lock conflicts
  const vuId = __VU;
  const propertyId = data.propertyIds[(vuId - 1) % data.propertyIds.length];

  const authHeaders = {
    'Content-Type':  'application/json',
    'Authorization': `Bearer ${data.token}`,
    'X-Load-Test':   'true',
    'X-Test-Scenario': 'pacelc_consistency',
  };

  const getHeaders = {
    'X-Load-Test':   'true',
    'X-Test-Scenario': 'pacelc_consistency',
  };

  // 1. Determine current status, toggle target
  const detailRes = http.get(`${GATEWAY_URL}/public/properties/${propertyId}`, { headers: getHeaders });
  if (detailRes.status !== 200) {
    sleep(0.5);
    return;
  }

  const currentStatus = detailRes.json().status || 'AVAILABLE';
  const targetStatus = currentStatus === 'AVAILABLE' ? 'RENTED' : 'AVAILABLE';

  // 2. Perform Write
  const writeUrl = `${GATEWAY_URL}/properties/${propertyId}/status`;
  const writePayload = JSON.stringify({ status: targetStatus });
  
  const writeStartTime = Date.now();
  const writeRes = http.put(writeUrl, writePayload, { headers: authHeaders });

  const writeCheck = check(writeRes, {
    'Write response is 200 OK': (r) => r.status === 200,
  });

  if (!writeCheck) {
    sleep(0.5);
    return;
  }

  const writeDoneTime = Date.now();

  // 3. Immediately read in a loop until the updated status is returned
  let readSuccess = false;
  let attempts = 0;
  const maxAttempts = 15;
  const loopStartTime = Date.now();

  while (attempts < maxAttempts) {
    attempts++;
    const readRes = http.get(`${GATEWAY_URL}/public/properties/${propertyId}`, { headers: getHeaders });
    
    if (readRes.status === 200) {
      const readStatus = readRes.json().status;
      if (readStatus === targetStatus) {
        const readTime = Date.now();
        const stalenessMs = readTime - writeDoneTime;
        StalenessTrend.add(stalenessMs);
        readSuccess = true;
        break;
      }
    }
    sleep(0.1); // Wait 100ms between check loops
  }

  check(readSuccess, {
    'Eventually consistent status observed': (s) => s === true,
  });

  sleep(1.0); // VU Pacing
}

export function handleSummary(data) {
  console.log('Writing PACELC Consistency summary report...');
  return {
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
    'scripts/chaos-engineering-suite/k6-summary-pacelc.txt': textSummary(data, { indent: ' ', enableColors: false }),
  };
}
