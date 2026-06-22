#!/bin/bash

# Configuration
POSTGRES_CONTAINER="bds-postgres"
IAM_DB="bds-iam-service-db"
CORE_DB="batdongsan_db"
POSTGRES_USER="postgres"

# BCrypt for 'password123'
BCRYPT_PASS='$2a$10$h.B58BhvPEtu6aeT.3EFyeySS02DM570PlRyO7r0AD81127fs1Lqa'

echo "============================================="
echo "🚀 Starting Intelligent Database Seeding"
echo "============================================="

# 1. Wait for Postgres to be healthy
echo "---> Checking if Postgres is ready..."
until docker exec $POSTGRES_CONTAINER pg_isready -U $POSTGRES_USER; do
  echo "Waiting for Postgres..."
  sleep 2
done

# 2. Seed IAM Service (Users)
echo -e "\n---> Seeding IAM Service (Users)..."
docker exec -i $POSTGRES_CONTAINER psql -U $POSTGRES_USER -d $IAM_DB <<EOF
INSERT INTO users (user_id, email, password, first_name, last_name, phone_number, role, status, created_at, updated_at)
VALUES 
('a0000000-0000-0000-0000-000000000000', 'admin@bds.com', '$BCRYPT_PASS', 'Super', 'Admin', '0900000001', 'ADMIN', 'ACTIVE', NOW(), NOW()),
('a0000000-0000-0000-0000-000000000001', 'agent@bds.com', '$BCRYPT_PASS', 'Pro', 'Agent', '0900000002', 'SALESAGENT', 'ACTIVE', NOW(), NOW()),
('2a878977-af51-4b2a-bb96-ddf2eac0dcb7', 'owner@bds.com', '$BCRYPT_PASS', 'Property', 'Owner', '0900000003', 'PROPERTY_OWNER', 'ACTIVE', NOW(), NOW()),
('f0df88b1-3d7b-40f1-bbd5-ccdb7b4fd5d1', 'customer@bds.com', '$BCRYPT_PASS', 'Happy', 'Customer', '0900000004', 'CUSTOMER', 'ACTIVE', NOW(), NOW())
ON CONFLICT (email) DO NOTHING;

-- Ensure specialized tables have records if they exist (JPA usually creates them)
INSERT INTO property_owners (owner_id) SELECT user_id FROM users WHERE role = 'PROPERTY_OWNER' ON CONFLICT DO NOTHING;
INSERT INTO customers (customer_id) SELECT user_id FROM users WHERE role = 'CUSTOMER' ON CONFLICT DO NOTHING;
INSERT INTO sale_agents (sale_agent_id, hired_date, employee_code, max_properties) SELECT user_id, NOW(), 'EMP-001', 10 FROM users WHERE role = 'SALESAGENT' ON CONFLICT DO NOTHING;
EOF

# 3. Seed Core Macroservice (Locations & Types)
echo -e "\n---> Seeding Core Macroservice (Metadata)..."
docker exec -i $POSTGRES_CONTAINER psql -U $POSTGRES_USER -d $CORE_DB <<EOF
-- Schema property_catalog
INSERT INTO property_catalog.property_types (property_type_id, type_name, description, is_active, created_at, updated_at)
VALUES ('40000000-0000-0000-0000-000000000001', 'Villa', 'Luxury villas', true, NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO property_catalog.cities (city_id, city_name) 
VALUES ('a0000000-0000-0000-0000-000000000000', 'Ho Chi Minh City') 
ON CONFLICT DO NOTHING;

INSERT INTO property_catalog.districts (district_id, district_name, city_id) 
VALUES ('d0000000-0000-0000-0000-000000000000', 'District 1', 'a0000000-0000-0000-0000-000000000000') 
ON CONFLICT DO NOTHING;

INSERT INTO property_catalog.wards (ward_id, ward_name, district_id) 
VALUES ('550e8400-e29b-41d4-a716-446655440000', 'Ben Nghe Ward', 'd0000000-0000-0000-0000-000000000000') 
ON CONFLICT DO NOTHING;

INSERT INTO property_catalog.document_types (document_type_id, name, is_compulsory, created_at, updated_at)
VALUES ('a0000000-0000-0000-0000-000000000001', 'Ownership Certificate', true, NOW(), NOW())
ON CONFLICT DO NOTHING;
EOF

# 4. Seed a Sample Property (Optional - only if none exists)
echo -e "\n---> Checking for existing properties..."
PROP_COUNT=$(docker exec $POSTGRES_CONTAINER psql -U $POSTGRES_USER -d $CORE_DB -t -c "SELECT count(*) FROM property_catalog.properties;")
if [ "${PROP_COUNT//[[:space:]]/}" -eq 0 ]; then
  echo "Database empty. Seeding sample property..."
  docker exec -i $POSTGRES_CONTAINER psql -U $POSTGRES_USER -d $CORE_DB <<EOF
  INSERT INTO property_catalog.properties (
    property_id, owner_id, property_type_id, ward_id, title, description, 
    transaction_type, full_address, area, price_amount, commission_rate, 
    service_fee_amount, service_fee_collected_amount, status, created_at, updated_at
  ) VALUES (
    '024293fc-eb47-4b9c-832b-bcdb2e5b3156', '2a878977-af51-4b2a-bb96-ddf2eac0dcb7', 
    '40000000-0000-0000-0000-000000000001', '550e8400-e29b-41d4-a716-446655440000',
    'Seeded Villa', 'Automatically seeded for testing', 'SALE', '123 Seed St', 
    500, 1000000, 0.05, 500, 0, 'PENDING', NOW(), NOW()
  );
EOF
else
  echo "Data detected ($PROP_COUNT properties). Skipping property seed."
fi

echo -e "\n============================================="
echo "✅ Seeding Complete. Data is persistent and correlated."
echo "============================================="
