#!/bin/bash

# Extract tokens (avoiding command substitution in the builder prompt)
OWNER_LOGIN=$(curl -s -X POST http://localhost:8088/api/auth/login -H "Content-Type: application/json" -d '{"email": "owner@bds.com", "password": "password123", "rememberMe": true}')
OWNER_TOKEN=$(echo $OWNER_LOGIN | grep -o '"token":"[^"]*' | grep -o '[^"]*$')

CUSTOMER_LOGIN=$(curl -s -X POST http://localhost:8088/api/auth/login -H "Content-Type: application/json" -d '{"email": "customer@bds.com", "password": "password123", "rememberMe": true}')
CUSTOMER_TOKEN=$(echo $CUSTOMER_LOGIN | grep -o '"token":"[^"]*' | grep -o '[^"]*$')

echo "OWNER_TOKEN=$OWNER_TOKEN" > tokens.env
echo "CUSTOMER_TOKEN=$CUSTOMER_TOKEN" >> tokens.env

echo -e "\n--- 7.2 GET /api/account/me (OWNER) ---"
curl -s -X GET http://localhost:8088/api/account/me -H "Authorization: Bearer $OWNER_TOKEN"
echo -e "\n"

echo -e "\n--- 7.2 PATCH /api/account/me (CUSTOMER) ---"
curl -s -X PATCH http://localhost:8088/api/account/me -H "Authorization: Bearer $CUSTOMER_TOKEN" -F "firstName=CustomerJohnny"
echo -e "\n"
