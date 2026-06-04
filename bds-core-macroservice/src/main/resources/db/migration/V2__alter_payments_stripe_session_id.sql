-- Rename payway_payment_id to stripe_session_id and expand its varchar length
ALTER TABLE transaction_workflow.payments RENAME COLUMN payway_payment_id TO stripe_session_id;
ALTER TABLE transaction_workflow.payments ALTER COLUMN stripe_session_id TYPE varchar(255);
