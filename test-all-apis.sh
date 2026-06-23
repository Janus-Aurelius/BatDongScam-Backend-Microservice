#!/bin/bash
source tokens.env

export GATEWAY="http://localhost:8088"

# Extract IDs from JSON files
OWNER_ID=$(cat owner.json | grep -o '"userId":"[^"]*"' | sed 's/"userId":"//g' | sed 's/"//g')
CUSTOMER_ID=$(cat customer.json | grep -o '"userId":"[^"]*"' | sed 's/"userId":"//g' | sed 's/"//g')
AGENT_ID=$(cat agent.json | grep -o '"userId":"[^"]*"' | sed 's/"userId":"//g' | sed 's/"//g')
ADMIN_ID=$(cat admin.json | grep -o '"userId":"[^"]*"' | sed 's/"userId":"//g' | sed 's/"//g')

echo "============================================="
echo "7. IAM Service"
echo "============================================="

echo -e "\n---> GET /api/account/me (OWNER)"
curl -s -X GET $GATEWAY/api/account/me -H "Authorization: Bearer $OWNER_TOKEN"

echo -e "\n\n---> PATCH /api/account/me (CUSTOMER)"
curl -s -X PATCH $GATEWAY/api/account/me \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -F "firstName=UpdatedCustomer"

echo -e "\n\n============================================="
echo "13. Core Macroservice"
echo "============================================="

echo -e "\n---> POST /properties (OWNER)"
# Valid property creation payload
# Required: ownerId, propertyTypeId, wardId, title, description, priceAmount, area, transactionType, address, documentsMetadata
PROP_RES=$(curl -s -X POST $GATEWAY/properties \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -F 'payload={
    "ownerId": "'$OWNER_ID'",
    "propertyTypeId": "40000000-0000-0000-0000-000000000001",
    "wardId": "550e8400-e29b-41d4-a716-446655440000",
    "title": "Luxury Villa",
    "description": "5 bedroom villa with pool",
    "priceAmount": 500000,
    "area": 350,
    "transactionType": "SALE",
    "address": "123 Test St",
    "documentsMetadata": [
      {
        "documentTypeId": "a0000000-0000-0000-0000-000000000001",
        "documentNumber": "DOC-123",
        "documentName": "Land Certificate",
        "fileIndex": 0
      }
    ]
  };type=application/json' \
  -F 'images=@test_data/dummy.jpg' \
  -F 'documents=@test_data/dummy.pdf')
echo "$PROP_RES"
PROPERTY_ID=$(echo "$PROP_RES" | grep -o '"propertyId":"[^"]*"' | head -n 1 | sed 's/"propertyId":"//g' | sed 's/"//g')
if [ -z "$PROPERTY_ID" ]; then
    PROPERTY_ID=$(echo "$PROP_RES" | grep -o '[0-9a-f]\{8\}-[0-9a-f]\{4\}-[0-9a-f]\{4\}-[0-9a-f]\{4\}-[0-9a-f]\{12\}' | head -n 1)
fi
echo "Extracted PROPERTY_ID=$PROPERTY_ID"

echo -e "\n\n---> GET /public/properties/search"
curl -s -X GET "$GATEWAY/public/properties/search?minPrice=100000&maxPrice=600000&page=0&size=10"

echo -e "\n\n---> POST /contracts/purchases (AGENT)"
# Required: propertyId, customerId, agreedPrice, advancePaymentAmount, advancePaymentDeadline, finalPaymentDeadline
CONTRACT_RES=$(curl -s -X POST $GATEWAY/contracts/purchases \
  -H "Authorization: Bearer $AGENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "propertyId": "'$PROPERTY_ID'",
    "customerId": "'$CUSTOMER_ID'",
    "agreedPrice": 500000,
    "advancePaymentAmount": 50000,
    "advancePaymentDeadline": "2026-08-01",
    "finalPaymentDeadline": "2026-12-31"
  }')
echo "$CONTRACT_RES"
CONTRACT_ID=$(echo "$CONTRACT_RES" | grep -o '"contractId":"[^"]*"' | head -n 1 | sed 's/"contractId":"//g' | sed 's/"//g')
if [ -z "$CONTRACT_ID" ]; then
    CONTRACT_ID=$(echo "$CONTRACT_RES" | grep -o '[0-9a-f]\{8\}-[0-9a-f]\{4\}-[0-9a-f]\{4\}-[0-9a-f]\{4\}-[0-9a-f]\{12\}' | head -n 1)
fi
echo "Extracted CONTRACT_ID=$CONTRACT_ID"

echo -e "\n\n---> POST /contracts/purchases/{contractId}/approve (AGENT)"
curl -s -X POST $GATEWAY/contracts/purchases/$CONTRACT_ID/approve -H "Authorization: Bearer $AGENT_TOKEN"

echo -e "\n\n============================================="
echo "8. Financial Service"
echo "============================================="

echo -e "\n---> POST /api/payments (ADMIN)"
PAYMENT_RES=$(curl -s -X POST $GATEWAY/api/payments \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "contractId": "'$CONTRACT_ID'",
    "propertyId": "'$PROPERTY_ID'",
    "payerId": "'$CUSTOMER_ID'",
    "paymentType": "DEPOSIT",
    "amount": 50000,
    "dueDate": "2026-07-01",
    "notes": "Deposit for luxury villa"
  }')
echo "$PAYMENT_RES"
PAYMENT_ID=$(echo "$PAYMENT_RES" | grep -o '"id":"[^"]*"' | head -n 1 | sed 's/"id":"//g' | sed 's/"//g')
if [ -z "$PAYMENT_ID" ]; then
    PAYMENT_ID=$(echo "$PAYMENT_RES" | grep -o '[0-9a-f]\{8\}-[0-9a-f]\{4\}-[0-9a-f]\{4\}-[0-9a-f]\{4\}-[0-9a-f]\{12\}' | head -n 1)
fi

echo -e "\n\n---> GET /api/payments/{paymentId} (ADMIN)"
curl -s -X GET $GATEWAY/api/payments/$PAYMENT_ID -H "Authorization: Bearer $ADMIN_TOKEN"

echo -e "\n\n============================================="
echo "9. Appointment Service"
echo "============================================="

echo -e "\n---> POST /api/appointment (CUSTOMER)"
APP_RES=$(curl -s -X POST $GATEWAY/api/appointment \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "propertyId": "'$PROPERTY_ID'",
    "requestedDate": "2026-08-15T10:00:00",
    "customerRequirements": "Need information on utilities",
    "agentId": null
  }')
echo "$APP_RES"
APPOINTMENT_ID=$(echo "$APP_RES" | grep -o '"id":"[^"]*"' | head -n 1 | sed 's/"id":"//g' | sed 's/"//g')
if [ -z "$APPOINTMENT_ID" ]; then
    APPOINTMENT_ID=$(echo "$APP_RES" | grep -o '[0-9a-f]\{8\}-[0-9a-f]\{4\}-[0-9a-f]\{4\}-[0-9a-f]\{4\}-[0-9a-f]\{12\}' | head -n 1)
fi

echo -e "\n\n============================================="
echo "10. Moderation Service"
echo "============================================="

echo -e "\n---> POST /api/violations (CUSTOMER)"
VIO_RES=$(curl -s -X POST $GATEWAY/api/violations \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -F 'payload={
    "reporterId": "'$CUSTOMER_ID'",
    "violationType": "SCAM_ATTEMPT",
    "description": "Suspicious property.",
    "violationReportedType": "PROPERTY",
    "reportedId": "'$PROPERTY_ID'"
  };type=application/json')
echo "$VIO_RES"
VIO_ID=$(echo "$VIO_RES" | grep -o '"id":"[^"]*"' | head -n 1 | sed 's/"id":"//g' | sed 's/"//g')
if [ -z "$VIO_ID" ]; then
    VIO_ID=$(echo "$VIO_RES" | grep -o '[0-9a-f]\{8\}-[0-9a-f]\{4\}-[0-9a-f]\{4\}-[0-9a-f]\{4\}-[0-9a-f]\{12\}' | head -n 1)
fi

echo -e "\n\n---> GET /api/violations/my-violations (CUSTOMER)"
curl -s -X GET $GATEWAY/api/violations/my-violations -H "Authorization: Bearer $CUSTOMER_TOKEN"

echo -e "\n\n============================================="
echo "11. Notification Service"
echo "============================================="

echo -e "\n---> GET /api/notifications (CUSTOMER)"
curl -s -X GET "$GATEWAY/api/notifications?page=1&limit=10" -H "Authorization: Bearer $CUSTOMER_TOKEN"

echo -e "\n\n============================================="
echo "12. Search Service"
echo "============================================="

echo -e "\n---> GET /api/search/top (CUSTOMER)"
curl -s -X GET "$GATEWAY/api/search/top?userId=$CUSTOMER_ID&searchType=CITY&year=2026&month=6" -H "Authorization: Bearer $CUSTOMER_TOKEN"

echo -e "\n\nTesting complete!"
