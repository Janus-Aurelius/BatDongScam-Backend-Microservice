#!/bin/bash

# Extract tokens using sed
curl -s -X POST http://localhost:8088/api/auth/login -H "Content-Type: application/json" -d '{"email": "owner@bds.com", "password": "password123", "rememberMe": true}' > owner.json
OWNER_TOKEN=$(cat owner.json | grep -o '"token":"[^"]*"' | sed 's/"token":"//g' | sed 's/"//g')
echo "OWNER_TOKEN=$OWNER_TOKEN" > tokens.env

curl -s -X POST http://localhost:8088/api/auth/login -H "Content-Type: application/json" -d '{"email": "customer@bds.com", "password": "password123", "rememberMe": true}' > customer.json
CUSTOMER_TOKEN=$(cat customer.json | grep -o '"token":"[^"]*"' | sed 's/"token":"//g' | sed 's/"//g')
echo "CUSTOMER_TOKEN=$CUSTOMER_TOKEN" >> tokens.env

curl -s -X POST http://localhost:8088/api/auth/login -H "Content-Type: application/json" -d '{"email": "test-admin@bds.com", "password": "password123", "rememberMe": true}' > admin.json
ADMIN_TOKEN=$(cat admin.json | grep -o '"token":"[^"]*"' | sed 's/"token":"//g' | sed 's/"//g')
echo "ADMIN_TOKEN=$ADMIN_TOKEN" >> tokens.env

curl -s -X POST http://localhost:8088/api/auth/login -H "Content-Type: application/json" -d '{"email": "agent2@bds.com", "password": "password123", "rememberMe": true}' > agent.json
AGENT_TOKEN=$(cat agent.json | grep -o '"token":"[^"]*"' | sed 's/"token":"//g' | sed 's/"//g')
echo "AGENT_TOKEN=$AGENT_TOKEN" >> tokens.env

echo "Tokens extracted successfully."
