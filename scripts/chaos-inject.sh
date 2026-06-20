#!/bin/bash

# ==============================================================================
# BDS Microservices Chaos Injection Script
# Senior Chaos Engineer & Systems Architect Tool
#
# Usage:
#   ./chaos-inject.sh partition    - Inject network partition (Core -> Financial/Kafka)
#   ./chaos-inject.sh latency      - Inject 200ms database latency (Core -> Postgres)
#   ./chaos-inject.sh heal         - Remove all injected chaos rules
#   ./chaos-inject.sh status       - Check active network configuration inside containers
# ==============================================================================

set -euo pipefail

# Find all running core macroservice containers (handles scaling dynamically)
get_core_containers() {
    docker ps --format '{{.Names}}' | grep -E "core-macroservice|core_macroservice" || echo ""
}

# ------------------------------------------------------------------------------
# 1. NETWORK PARTITION: Core -> Financial & Kafka
# ------------------------------------------------------------------------------
inject_partition() {
    containers=$(get_core_containers)
    if [ -z "$containers" ]; then
        echo "Error: No running core-macroservice containers found."
        exit 1
    fi

    for container in $containers; do
        echo "==== Ingesting Network Partition on: $container ===="
        
        # Prevent duplicate rules by flushing first
        docker exec -t "$container" iptables -F OUTPUT 2>/dev/null || true
        
        # Block outbound TCP connections to Financial service and Kafka
        # -d accepts hostname, which will resolve inside the container bridge network
        echo "-> Dropping egress traffic to 'financial-service'..."
        docker exec -t "$container" iptables -A OUTPUT -d financial-service -j DROP
        
        echo "-> Dropping egress traffic to 'kafka'..."
        docker exec -t "$container" iptables -A OUTPUT -d kafka -j DROP
        
        echo "Partition injected successfully on $container."
    done
}

# ------------------------------------------------------------------------------
# 2. DATABASE LATENCY: Core -> PostgreSQL (5432)
# ------------------------------------------------------------------------------
inject_latency() {
    containers=$(get_core_containers)
    if [ -z "$containers" ]; then
        echo "Error: No running core-macroservice containers found."
        exit 1
    fi

    for container in $containers; do
        echo "==== Injecting 200ms Database Latency on: $container ===="
        
        # Clean up any existing traffic control rules on eth0
        docker exec -t "$container" tc qdisc del dev eth0 root 2>/dev/null || true
        
        # Create a classful qdisc (prio) on eth0 root
        # This will group traffic into bands. Band 3 (1:3) will receive the latency
        docker exec -t "$container" tc qdisc add dev eth0 root handle 1: prio
        
        # Attach a netem qdisc to parent 1:3 to inject 200ms latency
        docker exec -t "$container" tc qdisc add dev eth0 parent 1:3 handle 30: netem delay 200ms
        
        # Add a filter routing traffic going to Postgres (port 5432) to class 1:3
        docker exec -t "$container" tc filter add dev eth0 protocol ip parent 1:0 prio 3 u32 match ip dport 5432 0xffff flowid 1:3
        
        echo "Database latency (200ms) successfully applied to PostgreSQL traffic on $container."
    done
}

# ------------------------------------------------------------------------------
# 3. HEAL: Restore Network State
# ------------------------------------------------------------------------------
heal_network() {
    containers=$(get_core_containers)
    if [ -z "$containers" ]; then
        echo "Warning: No running core-macroservice containers found to heal."
        exit 0
    fi

    for container in $containers; do
        echo "==== Healing Network State on: $container ===="
        
        # Flush all iptables rules
        docker exec -t "$container" iptables -F OUTPUT 2>/dev/null || true
        
        # Delete tc qdiscs
        docker exec -t "$container" tc qdisc del dev eth0 root 2>/dev/null || true
        
        echo "Network fully healed on $container."
    done
}

# ------------------------------------------------------------------------------
# 4. STATUS: Verify Active Rules
# ------------------------------------------------------------------------------
check_status() {
    containers=$(get_core_containers)
    if [ -z "$containers" ]; then
        echo "No running core-macroservice containers found."
        exit 0
    fi

    for container in $containers; do
        echo "=========================================================="
        echo "STATUS FOR CONTAINER: $container"
        echo "=========================================================="
        
        echo "--> Active iptables Outgoing Rules:"
        docker exec -t "$container" iptables -L OUTPUT -n -v || echo "Failed to fetch iptables"
        
        echo -e "\n--> Active Traffic Control (tc) Qdiscs:"
        docker exec -t "$container" tc qdisc show dev eth0 || echo "Failed to fetch tc qdisc info"
        
        echo -e "\n--> Active Traffic Control (tc) Filters:"
        docker exec -t "$container" tc filter show dev eth0 || echo "Failed to fetch tc filter info"
        echo "=========================================================="
    done
}

# ------------------------------------------------------------------------------
# Command Router
# ------------------------------------------------------------------------------
case "${1:-}" in
    partition)
        inject_partition
        ;;
    latency)
        inject_latency
        ;;
    heal)
        heal_network
        ;;
    status)
        check_status
        ;;
    *)
        echo "Usage: $0 {partition|latency|heal|status}"
        exit 1
        ;;
esac
