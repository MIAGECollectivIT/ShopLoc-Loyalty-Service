-- Refactor strict UUID exposure: add store_uid UUID columns and relax legacy store_id NOT NULL constraints

ALTER TABLE merchant_loyalty_rates ADD COLUMN IF NOT EXISTS store_uid UUID;
CREATE UNIQUE INDEX IF NOT EXISTS uq_merchant_loyalty_rates_store_uid ON merchant_loyalty_rates (store_uid);
ALTER TABLE merchant_loyalty_rates ALTER COLUMN store_id DROP NOT NULL;

ALTER TABLE loyalty_gifts ADD COLUMN IF NOT EXISTS store_uid UUID;
CREATE INDEX IF NOT EXISTS idx_loyalty_gifts_store_uid ON loyalty_gifts (store_uid);
CREATE INDEX IF NOT EXISTS idx_loyalty_gifts_store_uid_active ON loyalty_gifts (store_uid, is_active);
ALTER TABLE loyalty_gifts ALTER COLUMN store_id DROP NOT NULL;

ALTER TABLE loyalty_points_transactions ADD COLUMN IF NOT EXISTS store_uid UUID;
CREATE INDEX IF NOT EXISTS idx_loyalty_points_transactions_store_uid ON loyalty_points_transactions (store_uid);
