# Chaos Engineering & Load Testing Suite

This specialized testing suite is designed to prove advanced distributed systems theorems (**Universal Scalability Law**, **PACELC**, **CALM**, and **FLP Impossibility**) on the `bds-core-macroservice` architecture.

---

## 1. Universal Scalability Law (DB Contention Thrashing)
*USL proves that concurrency scaling eventually degrades throughput due to contention (coherency costs) and serialization limits.*

### Execution Steps
1. **Startup the Constrained Environment:**
   Run the stack scaling to 5 instances with Postgres resource bounds and oversized connection pools (50 connections per instance):
   ```bash
   docker compose -f docker-compose.yml -f scripts/chaos-engineering-suite/docker-compose.usl.yml up -d --scale core-macroservice=5
   ```
2. **Start the Starvation Chaos:**
   In one terminal, start the noisy neighbor script:
   ```bash
   ./scripts/chaos-engineering-suite/usl-noisy-neighbor.sh
   ```
3. **Trigger the Load:**
   In another terminal, start the ramping load test:
   ```bash
   k6 run scripts/chaos-engineering-suite/usl-contention.js
   ```

### Observability & Verification
- **k6 Output Verification:**
  Compare the Requests Per Second (RPS) metrics at the beginning (10 VUs) to the peak (500 VUs) while the noisy neighbor script restricts CPU to 0.3. You will observe that as concurrency climbs, RPS stops growing linearly, plateaus, and actively drops (the classic USL downward curve).
- **Postgres Active Clients Audit:**
  Run this SQL query against Postgres to check for thundering active backends and waiting locks:
  ```sql
  SELECT count(*), state, wait_event_type, wait_event FROM pg_stat_activity GROUP BY state, wait_event_type, wait_event;
  ```
- **Grafana Loki Query:**
  Search for Connection pool starvation timeouts and DB lock waits:
  ```text
  {container_name="bds-core-macroservice"} |= "HikariPool-1 - Connection is not available"
  ```

---

## 2. PACELC Theorem (Cache Consistency vs Latency)
*PACELC dictates that under network degradation (Else), we choose between Latency (L) and Consistency (C). This test validates that the system prioritizes latency/availability (EL) while temporarily serving stale reads.*

### Execution Steps
1. **Start the Proxy Environment:**
   Start the containers with the Toxiproxy interceptor:
   ```bash
   docker compose -f docker-compose.yml -f scripts/chaos-engineering-suite/docker-compose-chaos-suite.yml up -d
   ```
2. **Inject Network Degradation (Jitter + Latency + Loss):**
   ```bash
   ./scripts/chaos-engineering-suite/pacelc-toxiproxy.sh inject
   ```
3. **Execute Load & Consistency Checks:**
   ```bash
   k6 run scripts/chaos-engineering-suite/pacelc-consistency.js
   ```
4. **Heal the Network:**
   ```bash
   ./scripts/chaos-engineering-suite/pacelc-toxiproxy.sh heal
   ```

### Observability & Verification
- **k6 Results:**
  Validate the `staleness_window_ms` custom metric. You will see it capture the millisecond delay before the cache/database reconciles the new state due to Redis degradation. Verify that despite 15% packet loss and up to 1.5s jitter, `http_req_failed` remains under 5% (verifying system availability).

---

## 3. CALM Theorem (Concurrent Modification Collision)
*CALM states that non-monotonic operations require coordination. This test validates that version-controlled entity mutations gracefully reject duplicate/stale updates with a 409 Conflict status.*

### Execution Steps
1. **Trigger the Retry Storm:**
   Fire exactly 50 duplicate mutation requests to approve the same Contract ID spread over a 3-second window:
   ```bash
   k6 run scripts/chaos-engineering-suite/calm-retry-storm.js
   ```

### Observability & Verification
- **k6 Assertions:**
  k6 will automatically assert:
  - Exactly **1** request returns `200 OK` (success).
  - Exactly **49** requests return `409 Conflict` (Optimistic locking collision).
  - Exactly **0** requests return `500 Internal Server Error` (proves that database conflicts are handled gracefully).
- **Application Console Logs (Loki):**
  Verify that the application intercepts the conflicts cleanly:
  ```text
  {container_name="bds-core-macroservice"} |= "OptimisticLockException"
  ```

---

## 4. FLP Impossibility (Zombie Node)
*FLP proves that in an asynchronous network, dead nodes cannot be distinguished from slow nodes. This test validates ShedLock lease safety when a frozen node resurrects.*

### Execution Steps
1. **Execute the Zombie Freeze & Resurrect:**
   This script freezes the ShedLock scheduler leader during its execution, waits for the lease to expire (55s lease + 5s buffer), and unpauses it:
   ```bash
   ./scripts/chaos-engineering-suite/flp-zombie.sh
   ```

### Observability & Verification
- **App Console Audit:**
  Read the logs of the resurrected container to verify that it aborted duplicate processing gracefully with a concurrency conflict exception once it woke up:
  ```bash
  docker logs --since 3m <leader-container> | grep -E 'OptimisticLockException|ObjectOptimisticLockingFailureException|Conflict'
  ```
- **Postgres Lock Tables Audit:**
  Run this query during the freeze to verify the lock lease state in ShedLock table:
  ```sql
  SELECT * FROM transaction_workflow.shedlock;
  ```
