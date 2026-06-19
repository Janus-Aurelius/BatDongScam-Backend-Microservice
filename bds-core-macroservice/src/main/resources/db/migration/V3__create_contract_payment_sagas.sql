CREATE TABLE transaction_workflow.contract_payment_sagas (
    saga_id UUID PRIMARY KEY,
    contract_id UUID NOT NULL,
    payment_id UUID,
    amount NUMERIC(15, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
