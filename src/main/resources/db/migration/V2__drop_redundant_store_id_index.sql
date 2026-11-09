-- Story SLE-55 / SLE-145: Drop redundant store_id index (already enforced by unique constraint uq_merchant_loyalty_rates_store_id)
DROP INDEX IF EXISTS idx_merchant_loyalty_rates_store_id;

