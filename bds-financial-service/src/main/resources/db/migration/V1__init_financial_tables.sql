CREATE TABLE payments (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    contract_id UUID,
    property_id UUID,
    payer_id UUID,
    payer_name VARCHAR(255),
    property_title VARCHAR(255),
    contract_number VARCHAR(255),
    payment_type VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    due_date DATE NOT NULL,
    paid_time TIMESTAMP,
    installment_number INTEGER,
    payment_method VARCHAR(255),
    transaction_reference VARCHAR(100),
    penalty_amount NUMERIC(15, 2),
    notes TEXT,
    payos_payment_id VARCHAR(255),
    checkout_url TEXT
);

CREATE TABLE payouts (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    account_number VARCHAR(255) NOT NULL,
    account_holder_name VARCHAR(255) NOT NULL,
    swift_code VARCHAR(255),
    description VARCHAR(255),
    status VARCHAR(255) NOT NULL,
    gateway_payout_id VARCHAR(255),
    error_message TEXT
);

CREATE TABLE commissions (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    payment_id UUID,
    contract_id UUID,
    property_id UUID,
    agent_id UUID,
    agent_name VARCHAR(255),
    property_title VARCHAR(255),
    commission_amount NUMERIC(15, 2) NOT NULL,
    transaction_amount NUMERIC(15, 2) NOT NULL,
    commission_date DATE NOT NULL,
    status VARCHAR(255) NOT NULL,
    notes TEXT
);
