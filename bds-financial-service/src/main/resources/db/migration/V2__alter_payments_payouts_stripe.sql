-- Rename payos_payment_id to stripe_session_id in payments table and expand length
ALTER TABLE payments RENAME COLUMN payos_payment_id TO stripe_session_id;
ALTER TABLE payments ALTER COLUMN stripe_session_id TYPE varchar(255);

-- Rename gateway_payout_id to stripe_payout_id in payouts table and expand length
ALTER TABLE payouts RENAME COLUMN gateway_payout_id TO stripe_payout_id;
ALTER TABLE payouts ALTER COLUMN stripe_payout_id TYPE varchar(255);
