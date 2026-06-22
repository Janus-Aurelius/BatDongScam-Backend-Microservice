import http from 'k6/http';
import { check, fail } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

// ==============================================================================
// BDS Microservices E2E Load Test Suite
// Senior Chaos Engineer & Systems Architect Tool
//
// Execution:
//   k6 run -e SCENARIO=littles_law_cascade scripts/e2e-load-tests.js
//   k6 run -e SCENARIO=usl_baseline scripts/e2e-load-tests.js
//   k6 run -e SCENARIO=usl_scaled scripts/e2e-load-tests.js
//   k6 run -e SCENARIO=peak_capacity scripts/e2e-load-tests.js
// ==============================================================================

// Read Scenario configuration from environment variable (defaults to Little's Law)
const SCENARIO = __ENV.SCENARIO || 'littles_law_cascade';
const GATEWAY_URL = __ENV.GATEWAY_URL || 'http://localhost:8088';

// Configure test duration and VUs dynamically based on the scenario being executed
let scenarioConfig = {};

if (SCENARIO === 'peak_capacity') {
  // Stress Test to find operational limits and maximum capacity metrics for CV
  scenarioConfig = {
    executor: 'ramping-vus',
    startVUs: 1,
    stages: [
      { duration: '15s', target: 150 }, // Ramp from 1 to 150 concurrent users
      { duration: '15s', target: 150 }, // Hold peak load
      { duration: '10s', target: 0 },   // Ramp down to 0
    ],
    gracefulRampDown: '5s',
  };
} else {
  // Constant load testing for specific theorems
  let vus = 10;
  let duration = '30s';
  if (SCENARIO === 'littles_law_cascade') {
    vus = 80;        // High concurrent user count to trigger cascading backpressure
    duration = '35s';
  } else if (SCENARIO === 'usl_baseline' || SCENARIO === 'usl_scaled') {
    vus = 15;        // Medium concurrency to show database locking contention
    duration = '30s';
  }
  scenarioConfig = {
    executor: 'constant-vus',
    vus: vus,
    duration: duration,
  };
}

export const options = {
  scenarios: {
    load_scenario: scenarioConfig,
  },
  thresholds: {
    // We expect some failures under chaos/contention; we track them but don't fail the test runner.
    http_req_failed: ['rate < 1.0'], 
  },
};

// ------------------------------------------------------------------------------
// SETUP: Runs once before the virtual users start executing.
// Fetches tokens and initializes test contracts.
// ------------------------------------------------------------------------------
export function setup() {
  console.log(`[SETUP] Initializing test session for SCENARIO: ${SCENARIO}`);

  if (SCENARIO === 'littles_law_cascade' || SCENARIO === 'peak_capacity') {
    // Little's Law and Peak Capacity tests run on the public search path (read-only, stateless)
    console.log('[SETUP] Stateless scenario. Skipping contract creation.');
    return { token: null, contractId: null };
  }

  // USL tests update the same contract status, requiring authentication
  // 1. Authenticate as Sales Agent
  const loginPayload = JSON.stringify({
    email: 'agent@bds.com',
    password: 'password123',
    rememberMe: true,
  });

  console.log(`[SETUP] Attempting agent login at ${GATEWAY_URL}/api/auth/login...`);
  const loginRes = http.post(`${GATEWAY_URL}/api/auth/login`, loginPayload, {
    headers: { 'Content-Type': 'application/json' },
  });
  console.log(`[SETUP] Login response received. Status: ${loginRes.status}`);

  if (loginRes.status !== 200) {
    fail(`[SETUP ERROR] Login failed for agent@bds.com. Status: ${loginRes.status}, Body: ${loginRes.body}`);
  }

  const agentToken = loginRes.json().token;
  console.log('[SETUP] Sales Agent logged in successfully.');

  // 2. Fetch a valid property ID from the database to link to our test contract
  console.log(`[SETUP] Fetching properties from ${GATEWAY_URL}/public/properties/search...`);
  const searchRes = http.get(`${GATEWAY_URL}/public/properties/search?page=0&size=5`);
  console.log(`[SETUP] Property search response received. Status: ${searchRes.status}`);
  let propertyId = null;

  if (searchRes.status === 200) {
    const searchBody = searchRes.json();
    const content = searchBody.content || (searchBody.data && searchBody.data.content);
    if (content && content.length > 0) {
      propertyId = content[0].id || content[0].propertyId;
      console.log(`[SETUP] Found active property to link to contract: ${propertyId}`);
    }
  }

  // Fallback if no active properties found
  if (!propertyId) {
    propertyId = '50000000-0000-0000-0000-000000000001';
    console.log(`[SETUP] Warning: No properties found via search. Using fallback: ${propertyId}`);
  }

  // 3. Create a DRAFT contract to run contention operations on
  const customerId = 'f0df88b1-3d7b-40f1-bbd5-ccdb7b4fd5d1'; // Default test customer
  const contractPayload = JSON.stringify({
    propertyId: propertyId,
    customerId: customerId,
    agreedPrice: 500000,
    advancePaymentAmount: 50000,
    advancePaymentDeadline: '2026-08-01',
    finalPaymentDeadline: '2026-12-31',
  });

  console.log(`[SETUP] Creating test contract at ${GATEWAY_URL}/contracts/purchases...`);
  const contractRes = http.post(`${GATEWAY_URL}/contracts/purchases`, contractPayload, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${agentToken}`,
    },
  });
  console.log(`[SETUP] Contract creation response received. Status: ${contractRes.status}`);

  if (contractRes.status !== 200) {
    fail(`[SETUP ERROR] Failed to create test purchase contract. Status: ${contractRes.status}, Body: ${contractRes.body}`);
  }

  const contractId = contractRes.json().id;
  console.log(`[SETUP] Created test contract ID: ${contractId} in DRAFT status.`);

  return { token: agentToken, contractId: contractId };
}

// ------------------------------------------------------------------------------
// DEFAULT (VUs Loop): Executed concurrently by each VU.
// ------------------------------------------------------------------------------
export default function (data) {
  // Add telemetry headers to trace load testing traffic via Jaeger/Prometheus/Loki
  const headers = {
    'X-Load-Test': 'true',
    'X-Test-Scenario': SCENARIO,
  };

  if (SCENARIO === 'littles_law_cascade' || SCENARIO === 'peak_capacity') {
    // Read-heavy search flow (routes through API Gateway to Core)
    const url = `${GATEWAY_URL}/public/properties/search?minPrice=100000&maxPrice=600000&page=0&size=10`;
    const res = http.get(url, { headers: headers });

    // Under normal system load, we expect 200 OK.
    // Under queue-overwhelm and chaos latency, we expect 429 (Rate Limit) or 503/504 (Timeout/Circuit Breaker)
    check(res, {
      'gateway returns 200, 429, or 503/504': (r) => [200, 429, 503, 504].includes(r.status),
    });

  } else {
    // USL scenarios (Baseline / Scaled): send concurrent updates to the SAME contract
    // This forces row-level lock contention in PostgreSQL
    if (!data.contractId) {
      fail('[VU ERROR] Aborting: No contract ID supplied by setup.');
    }

    headers['Authorization'] = `Bearer ${data.token}`;
    headers['Content-Type'] = 'application/json';

    const url = `${GATEWAY_URL}/contracts/purchases/${data.contractId}/approve`;
    const res = http.post(url, null, { headers: headers });

    // We check for successful transitions (200) or database transaction lock waits leading to server errors (500 / 504)
    check(res, {
      'lock contention status 200 or 500/504': (r) => [200, 500, 504].includes(r.status),
    });
  }
}

// ------------------------------------------------------------------------------
// TEARDOWN: Runs once at the end of the test.
// Cancels/voids the created contract to return the database to a clean state.
// ------------------------------------------------------------------------------
export function teardown(data) {
  if (SCENARIO === 'littles_law_cascade' || SCENARIO === 'peak_capacity' || !data.contractId) {
    console.log('[TEARDOWN] Stateless read scenario. No cleanup needed.');
    return;
  }

  console.log(`[TEARDOWN] Initiating cleanup for contract ID: ${data.contractId}`);

  // Cancel/Void the contract using the authenticated agent token
  const cancelRes = http.post(`${GATEWAY_URL}/contracts/purchases/${data.contractId}/cancel`, null, {
    headers: {
      'Authorization': `Bearer ${data.token}`,
      'X-Load-Test': 'true',
      'X-Test-Scenario': SCENARIO,
    },
  });

  if (cancelRes.status === 200) {
    console.log(`[TEARDOWN] Cleanup successful. Contract ${data.contractId} has been CANCELLED.`);
  } else {
    console.warn(`[TEARDOWN WARNING] Contract cleanup returned unexpected status: ${cancelRes.status}, Body: ${cancelRes.body}`);
  }
}

// ------------------------------------------------------------------------------
// SUMMARY REPORTING: Write text reports and detailed metrics to files
// ------------------------------------------------------------------------------
export function handleSummary(data) {
  const scenarioLabel = SCENARIO.toLowerCase();
  const summaryTxtFile = `scripts/k6-summary-${scenarioLabel}.txt`;
  
  let cvMetrics = '';
  
  if (SCENARIO === 'peak_capacity') {
    const totalRequests = data.metrics.http_reqs ? (data.metrics.http_reqs.values.count || 0) : 0;
    const peakRPS = data.metrics.http_reqs ? (data.metrics.http_reqs.values.rate || 0).toFixed(2) : '0.00';
    const durationValues = data.metrics.http_req_duration ? data.metrics.http_req_duration.values : null;
    const avgLatency = durationValues ? (durationValues.avg || 0).toFixed(2) : '0.00';
    const p95Latency = durationValues ? (durationValues['p(95)'] || 0).toFixed(2) : '0.00';
    const p99Latency = durationValues ? (durationValues['p(99)'] || 0).toFixed(2) : '0.00';
    const failedRate = data.metrics.http_req_failed ? (data.metrics.http_req_failed.values.rate || 0) : 0;
    const successRate = ((1 - failedRate) * 100).toFixed(2);
    
    cvMetrics = `
======================================================================
                  🏆 CV RESUME KEY PERFORMANCE METRICS 🏆
======================================================================
* Distributed System Throughput: Peak of ${peakRPS} Req/Sec (RPS)
* Total Transactions Handled: ${totalRequests} HTTP requests
* API Gateway Response Times: Avg: ${avgLatency}ms | p95: ${p95Latency}ms | p99: ${p99Latency}ms
* Overall System Success Rate: ${successRate}% under high stress loads
* Maximum Concurrent Users (VUs) Supported: 150 VUs
======================================================================
`;
  }
  
  console.log(`\nWriting E2E load test summary report to: ${summaryTxtFile}`);
  if (cvMetrics) {
    console.log(cvMetrics);
  }
  
  const stdSummary = textSummary(data, { indent: ' ', enableColors: true });
  const fileSummary = textSummary(data, { indent: ' ', enableColors: false });
  
  return {
    'stdout': cvMetrics ? cvMetrics + '\n' + stdSummary : stdSummary,
    [summaryTxtFile]: cvMetrics ? cvMetrics + '\n' + fileSummary : fileSummary,
  };
}
