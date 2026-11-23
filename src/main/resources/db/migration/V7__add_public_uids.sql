-- Refactor strict UUID exposure: add public UUID `uid` columns on persisted tables

ALTER TABLE loyalty_gifts ADD COLUMN IF NOT EXISTS uid UUID DEFAULT gen_random_uuid();
UPDATE loyalty_gifts SET uid = gen_random_uuid() WHERE uid IS NULL;
ALTER TABLE loyalty_gifts ALTER COLUMN uid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_loyalty_gifts_uid ON loyalty_gifts (uid);

ALTER TABLE merchant_loyalty_rates ADD COLUMN IF NOT EXISTS uid UUID DEFAULT gen_random_uuid();
UPDATE merchant_loyalty_rates SET uid = gen_random_uuid() WHERE uid IS NULL;
ALTER TABLE merchant_loyalty_rates ALTER COLUMN uid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_merchant_loyalty_rates_uid ON merchant_loyalty_rates (uid);

ALTER TABLE loyalty_points_accounts ADD COLUMN IF NOT EXISTS uid UUID DEFAULT gen_random_uuid();
UPDATE loyalty_points_accounts SET uid = gen_random_uuid() WHERE uid IS NULL;
ALTER TABLE loyalty_points_accounts ALTER COLUMN uid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_loyalty_points_accounts_uid ON loyalty_points_accounts (uid);

ALTER TABLE loyalty_points_transactions ADD COLUMN IF NOT EXISTS uid UUID DEFAULT gen_random_uuid();
UPDATE loyalty_points_transactions SET uid = gen_random_uuid() WHERE uid IS NULL;
ALTER TABLE loyalty_points_transactions ALTER COLUMN uid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_loyalty_points_transactions_uid ON loyalty_points_transactions (uid);

ALTER TABLE user_vfp_history ADD COLUMN IF NOT EXISTS uid UUID DEFAULT gen_random_uuid();
UPDATE user_vfp_history SET uid = gen_random_uuid() WHERE uid IS NULL;
ALTER TABLE user_vfp_history ALTER COLUMN uid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_vfp_history_uid ON user_vfp_history (uid);

ALTER TABLE vfp_parking_sessions ADD COLUMN IF NOT EXISTS uid UUID DEFAULT gen_random_uuid();
UPDATE vfp_parking_sessions SET uid = gen_random_uuid() WHERE uid IS NULL;
ALTER TABLE vfp_parking_sessions ALTER COLUMN uid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_vfp_parking_sessions_uid ON vfp_parking_sessions (uid);
