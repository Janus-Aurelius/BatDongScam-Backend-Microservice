#!/bin/bash
# ==============================================================================
# USL Chaos Script: Noisy Neighbor Database CPU Starvation
# ==============================================================================
set -euo pipefail

POSTGRES_CONTAINER="bds-postgres"
NOISY_NEIGHBOR_CPUS="0.3"
ORIGINAL_CPUS="1.0"
DELAY_INJECT=60
TEST_DURATION=300

echo "==== [USL CHAOS INITIALIZED] ===="
echo "Monitoring traffic baseline... Waiting ${DELAY_INJECT} seconds before injection."
sleep ${DELAY_INJECT}

echo "==== [INJECTING NOISY NEIGHBOR] ===="
echo "Reducing ${POSTGRES_CONTAINER} allocation: ${ORIGINAL_CPUS} CPUs -> ${NOISY_NEIGHBOR_CPUS} CPUs..."
docker update --cpus "${NOISY_NEIGHBOR_CPUS}" "${POSTGRES_CONTAINER}"
echo "Starvation rule active. Postgres is now CPU-bounded to cause context thrashing."

# Wait for test to finish
HEAL_DELAY=$((TEST_DURATION - DELAY_INJECT))
echo "Holding constraint for ${HEAL_DELAY} seconds..."
sleep ${HEAL_DELAY}

echo "==== [HEALING DB INSTANCE] ===="
echo "Restoring ${POSTGRES_CONTAINER} allocation to original: ${ORIGINAL_CPUS} CPUs..."
docker update --cpus "${ORIGINAL_CPUS}" "${POSTGRES_CONTAINER}"
echo "Postgres resources restored. USL chaos test execution complete."
