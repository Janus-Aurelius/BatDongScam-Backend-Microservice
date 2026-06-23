# Observability Verification Guide

This guide details the exact metrics, distributed traces, and log outputs you can show your professor to empirically prove the three distributed systems theorems using this chaos and load testing suite.

---

## 1. Little's Law (Global Queuing & Cascading Backpressure)

**Theorem Background**: Little’s Law states that the average number of requests in a queueing system ($L$) is equal to the average arrival rate ($\lambda$) multiplied by the average time a request spends in the system ($W$):
$$L = \lambda W$$
When downstream capacity slows down ($W \to \infty$), the queue size ($L$) grows. Because system resources (threads, connections) are finite, backpressure must propagate UP the chain. When thread pools exhaust, upstream components must reject traffic via **429 Rate Limits** or **503/504 Gateway Timeouts**.

### A. Grafana Dashboard Metrics & PromQL
Open Grafana (`http://localhost:3001` or standard port) and navigate to the **Microservices Production Dashboard**. Look for these panels:
1. **RPS & Error Rate**: You will see a sharp drop in `2xx` statuses and a simultaneous spike in `429 Too Many Requests` (rate limits) and `503 Service Unavailable` / `504 Gateway Timeout` (circuit breakers/timeouts).
2. **PromQL for Arrival Rate ($\lambda$)**:
   ```promql
   sum(rate(http_server_requests_seconds_count[1m])) by (uri, status)
   ```
   *Expected Outcome*: $\lambda$ will hit the API Gateway rate limit ceiling (50-100 RPS), after which requests fail with status `429`.
3. **PromQL for Queue Size / Active Requests ($L$)**:
   ```promql
   sum(http_server_requests_seconds_active_count) by (service_name)
   ```
   *Expected Outcome*: The active request count on `bds-core-macroservice` will plateau at its maximum Tomcat thread pool capacity, followed by a surge of active requests queuing in `bds-api-gateway`.
4. **PromQL for Avg Latency ($W$)**:
   ```promql
   sum(rate(http_server_requests_seconds_sum[1m])) by (uri) / sum(rate(http_server_requests_seconds_count[1m])) by (uri)
   ```
   *Expected Outcome*: Average response time ($W$) spike on `/public/properties/search` to match the injected 200ms DB latency + Tomcat queuing delay.

### B. Distributed Traces (Jaeger)
Open Jaeger (`http://localhost:16686`) and search for traces under the service `api-gateway` with tags `X-Test-Scenario = littles_law_cascade`:
* **Cascading Latency**: You will see a parent span for `api-gateway` lasting hundreds of milliseconds or seconds, child span `core-macroservice` taking up 99% of that time, and the leaf PostgreSQL operations taking exactly 200ms+ each.
* **Timeout Propagation**: Locate a failed trace with status `504 Gateway Timeout`. You will see that `api-gateway` terminated the connection because the downstream `core-macroservice` took longer than the configured HTTP client response timeout (`response-timeout: 10000ms`).

### C. Logs (Loki)
In Grafana's Explore tab, search Loki logs for the gateway container:
* **Rate Limiting**: Look for the log line:
  `Too Many Requests - Rate limit exceeded. Please try again later.`
  This proves the gateway's `RateLimitResponseBodyFilter` intercepted and rate-limited requests to protect the system.

---

## 2. The Two Generals' Problem (Cross-Service Partition & Idempotency)

**Theorem Background**: The Two Generals' Problem proves that over an unreliable network link, it is impossible for two systems to reach absolute consensus on whether a message was successfully delivered. 
Since the sender (`core-macroservice`) cannot know if a timeout means the receiver (`financial-service`) failed to get the packet, or processed it and failed to reply, the sender **must retry**, and the receiver **must be idempotent** to prevent double-processing (e.g., double charges).

### A. Distributed Traces (Jaeger)
In Jaeger, search for traces with service `core-macroservice` or `financial-service`.
1. **Trace 1: Network Timeout (The Loss)**:
   * You will see the orchestrator span `ContractPaymentSagaOrchestrator` publishing `PROCESS_PAYMENT` to Kafka topic `contract-saga-commands`.
   * Under partition chaos, the Kafka message broker or financial service is unreachable. The trace ends with a timeout/failure state or a compensating retry transaction.
2. **Trace 2: Duplicate Processing Check (Idempotency)**:
   * When the network partition heals, the retry is delivered. Search for two traces or two consumer spans inside `financial-service` with the **same** `paymentId`.
   * **First span**: Shows a full execution path: `SagaPaymentCommandListener.consumeCommand` -> `PaymentRepository.findById` -> DB Write (`status = SUCCESS`) -> Stripe Gateway call -> `kafkaTemplate.send` (dispatched result).
   * **Second span**: Shows a short-circuited execution path: `consumeCommand` -> `PaymentRepository.findById` -> immediately returns success without writing to the DB or making any payment gateway calls.

### B. Logs (Loki)
Query Loki logs for `bds-financial-service` matching the string `SagaListener`:
```text
[SagaListener] Received command: {"sagaId":"...","paymentId":"...","commandType":"PROCESS_PAYMENT"}
[SagaListener] Executing PROCESS_PAYMENT for sagaId=...
Checking if payment is already successful (idempotent step)
[SagaListener] PROCESS_PAYMENT succeeded, result sent...
```
*Expected Outcome*: The first log will show standard processing. The second log (duplicate) will print `Checking if payment is already successful (idempotent step)` and immediately return success without processing the transaction again. This proves the system solved the Two Generals' consistency problem.

---

## 3. Universal Scalability Law (USL)

**Theorem Background**: The Universal Scalability Law models relative system capacity $X(N)$ as:
$$X(N) = \frac{N}{1 + \alpha(N - 1) + \beta N(N - 1)}$$
Where $N$ is scale (number of app instances).
* Contention ($\alpha$): Serialization delay (e.g., waiting for database row locks).
* Coherency ($\beta$): Crosstalk delay (coordinating caches/transactions).
USL proves that when $\alpha > 0$ and $\beta > 0$, scaling from 1 to 3 instances **does not** yield a 3x throughput increase. In fact, if lock contention ($\alpha$) is high, throughput will plateau or even retrograde (drop) as scale increases.

### A. Grafana Performance Comparison
Compare the results of `usl_baseline` (1 instance) vs `usl_scaled` (3 instances):
1. **Throughput (RPS) Chart**:
   * *Baseline (1 Instance)*: Throughput reaches a stable peak (e.g., 40 RPS) with low/medium latency.
   * *Scaled (3 Instances)*: Instead of scaling to 120 RPS, the aggregate throughput will plateau close to the baseline (or even drop below it) because all 3 instances are blocking each other on the same PostgreSQL database row lock for `/contracts/purchases/{contractId}/approve`.
2. **Latency (p99) Chart**:
   * You will see p99 response times increase dramatically on the scaled test. This is the **queue wait time** for database locks.

### B. PromQL Database Lock Contention Metrics
Run these queries in Prometheus or Grafana to prove the bottleneck is the database, not JVM cpu/memory resources:
1. **Average Database Connection Wait Time**:
   ```promql
   sum(rate(hikaricp_connections_acquire_seconds_sum[1m])) / sum(rate(hikaricp_connections_acquire_seconds_count[1m]))
   ```
   *Expected Outcome*: Large spike in wait times on the 3-instance cluster as threads starve waiting for a PostgreSQL connection to complete the blocked update transaction.
2. **JVM Active Threads (Starvation)**:
   ```promql
   jvm_threads_live_threads{service_name="core-macroservice"}
   ```
   *Expected Outcome*: Thread pool saturation. Because threads are blocked waiting for PostgreSQL row locks, JVM live threads will shoot up and remain stuck.

### C. Jaeger Trace (Database Blocking)
Analyze a trace from `usl_scaled`:
* Open a span for `PurchaseContractServiceImpl.approvePurchaseContract`.
* Drill down into the child DB transaction span. You will see a massive block of time (e.g., 800ms) spent entirely on the SQL update operation, while the actual CPU processing time of the microservice is < 5ms. This visually proves database serialization contention ($\alpha$).

---

## 4. Peak Capacity & CV Resume Benchmarking

**Goal**: Determine the system's absolute upper limits under a controlled ramp-up load, and capture concrete numbers to display as high-impact bullet points on your CV (e.g., "Designed a microservices architecture that handles X requests per second with a p95 latency under Y ms").

### A. How to Run the Stress Test
Run the k6 test with the `peak_capacity` scenario:
```bash
k6 run -e SCENARIO=peak_capacity scripts/e2e-load-tests.js
```
This test will ramp up from 1 to 150 concurrent VUs (Virtual Users) over 40 seconds to stress test the API Gateway and the Core Microservice.

### B. Reading the Generated Resume Metrics Box
When the test finishes, k6 will output a custom summary box directly to the console and write it to the file `scripts/k6-summary-peak_capacity.txt`. It will look like this:

```text
======================================================================
                  🏆 CV RESUME KEY PERFORMANCE METRICS 🏆
======================================================================
* Distributed System Throughput: Peak of [Your Peak RPS, e.g. 520.25] Req/Sec (RPS)
* Total Transactions Handled: [Total requests, e.g. 15420] HTTP requests
* API Gateway Response Times: Avg: [Avg latency]ms | p95: [p95]ms | p99: [p99]ms
* Overall System Success Rate: [Success rate, e.g. 99.85]% under high stress loads
* Maximum Concurrent Users (VUs) Supported: 150 VUs
======================================================================
```

### C. Matching Grafana Metrics to CV Metrics
Verify these figures in your Grafana Dashboard during the test run:
1. **Peak Throughput**: In the **Requests Per Second (RPS)** chart, check the absolute maximum y-value of the 2xx status line. This corresponds to the highest scale the system processed successfully.
2. **Resource Efficiency**: In the **JVM & Resources** chart, verify the CPU and Heap utilization metrics on `core-macroservice` and `api-gateway` under 150 concurrent VUs. 
   * *CV Bullet Point Tip*: "Optimized Java JVM heap configurations and garbage collection parameters to scale microservice concurrency to 150+ users with less than 384MB memory utilization."
3. **Gateway Overhead**: Compare the gateway's latency to the core service's latency. If the gateway's latency is close to the core service's latency, it proves that the API Gateway adds minimal routing overhead (< 5ms), showing proper system tuning.

