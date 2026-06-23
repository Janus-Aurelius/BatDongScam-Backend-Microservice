#!/bin/bash
# ==============================================================================
# FLP Impossibility Chaos Script: Zombie Node Simulation (Process Freeze & Wake)
# ==============================================================================
set -euo pipefail

LOCK_KEY="bds-lock:pdfRetryLock"
LEASE_EXPIRY_SEC=55
BUFFER_SEC=5

echo "==== [FLP ZOMBIE TEST INITIALIZED] ===="

# 1. Discover core containers
echo "Discovering core-macroservice instances..."
containers=$(docker ps --format '{{.Names}}' | grep -E "core-macroservice|core_macroservice" || echo "")

if [ -z "$containers" ]; then
    echo "Error: No running core-macroservice containers found."
    exit 1
fi

# 2. Find the leader that currently holds the lock or ran the scheduler
LEADER_CONTAINER=""
for container in $containers; do
    echo "Checking logs of $container for active scheduler runs..."
    if docker logs --since 5m "$container" 2>&1 | grep "Scanning for failed PDF contract uploads" > /dev/null; then
        LEADER_CONTAINER="$container"
        echo "Found active scheduler leader: ${LEADER_CONTAINER}"
        break
    fi
done

if [ -z "$LEADER_CONTAINER" ]; then
    # Fallback to the first container in the list if no active runs logged yet
    LEADER_CONTAINER=$(echo "$containers" | head -n 1)
    echo "Warning: No scheduler activity logged in past 5 min. Defaulting to first container: ${LEADER_CONTAINER}"
fi

# 3. Simulate GC pause / Freeze the leader
echo "==== [FREEZING LEADER: SIGSTOP / PAUSE] ===="
echo "Pausing container ${LEADER_CONTAINER} to simulate OOM/GC Starvation..."
docker pause "${LEADER_CONTAINER}"

# 4. Wait for lease timeout (ShedLock lockAtMostFor = 55s)
WAIT_TIME=$((LEASE_EXPIRY_SEC + BUFFER_SEC))
echo "Waiting ${WAIT_TIME} seconds for ShedLock lease to expire and backup node to acquire lock..."
sleep ${WAIT_TIME}

# 5. Resurrect the zombie node
echo "==== [RESURRECTING ZOMBIE NODE: UNPAUSE] ===="
echo "Unpausing container ${LEADER_CONTAINER}..."
docker unpause "${LEADER_CONTAINER}"

echo "Zombie node resurrected."
echo "==== [FLP OBSERVABILITY AUDIT] ===="
echo "Audit application logs on the resurrected node (${LEADER_CONTAINER}) for OptimisticLockingFailureException or ConcurrencyFailureException."
echo "Use the following command to monitor logs on the zombie node:"
echo "  docker logs --since 2m ${LEADER_CONTAINER} | grep -E 'OptimisticLockException|ObjectOptimisticLockingFailureException|Conflict'"
echo "========================================"
