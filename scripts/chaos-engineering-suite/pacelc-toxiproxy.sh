#!/bin/bash
# ==============================================================================
# PACELC Chaos Script: Configure Toxiproxy for Redis Jitter & Packet Loss
# ==============================================================================
set -euo pipefail

TOXIPROXY_API="http://localhost:8474"
PROXY_NAME="redis_proxy"

case "${1:-}" in
    inject)
        echo "==== [CONFIGURING TOXIPROXY REDIS PROXY] ===="
        
        # 1. Create proxy if not exists
        curl -s -X POST "${TOXIPROXY_API}/proxies" \
          -H "Content-Type: application/json" \
          -d "{
            \"name\": \"${PROXY_NAME}\",
            \"listen\": \"0.0.0.0:26379\",
            \"upstream\": \"redis:6379\",
            \"enabled\": true
          }" || echo "Proxy may already exist. Reconfiguring..."

        # 2. Add Latency Jitter (Fluctuates randomly between 50ms and 1500ms)
        echo "-> Injecting latency jitter (50ms base, 1450ms jitter)..."
        curl -s -X POST "${TOXIPROXY_API}/proxies/${PROXY_NAME}/toxics" \
          -H "Content-Type: application/json" \
          -d '{
            "name": "redis_latency",
            "type": "latency",
            "stream": "upstream",
            "toxicity": 1.0,
            "attributes": {
              "latency": 50,
              "jitter": 1450
            }
          }'

        # 3. Add Packet Loss (15% timeout rate)
        echo "-> Injecting 15% packet loss (timeout)..."
        curl -s -X POST "${TOXIPROXY_API}/proxies/${PROXY_NAME}/toxics" \
          -H "Content-Type: application/json" \
          -d '{
            "name": "redis_timeout",
            "type": "timeout",
            "stream": "upstream",
            "toxicity": 0.15,
            "attributes": {
              "timeout": 0
            }
          }'
        
        echo "==== [PACELC REDIS CHAOS INJECTED SUCCESSFULLY] ===="
        ;;

    heal)
        echo "==== [HEALING TOXIPROXY REDIS PROXY] ===="
        
        # Remove all toxics by deleting the proxy and recreating it clean
        curl -s -X DELETE "${TOXIPROXY_API}/proxies/${PROXY_NAME}" || true
        
        curl -s -X POST "${TOXIPROXY_API}/proxies" \
          -H "Content-Type: application/json" \
          -d "{
            \"name\": \"${PROXY_NAME}\",
            \"listen\": \"0.0.0.0:26379\",
            \"upstream\": \"redis:6379\",
            \"enabled\": true
          }"
        
        echo "Toxiproxy Redis connection healed (restored to direct upstream)."
        ;;

    *)
        echo "Usage: $0 {inject|heal}"
        exit 1
        ;;
esac
