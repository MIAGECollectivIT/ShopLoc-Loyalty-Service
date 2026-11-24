-- Backfill store_uid columns for existing records based on historical store_id mappings (SLE-223)

UPDATE merchant_loyalty_rates SET store_uid = '11111111-1111-1111-1111-111111111111' WHERE store_id = 1 AND store_uid IS NULL;
UPDATE merchant_loyalty_rates SET store_uid = '22222222-2222-2222-2222-222222222222' WHERE store_id = 2 AND store_uid IS NULL;
UPDATE merchant_loyalty_rates SET store_uid = '33333333-3333-3333-3333-333333333333' WHERE store_id = 3 AND store_uid IS NULL;
UPDATE merchant_loyalty_rates SET store_uid = '44444444-4444-4444-4444-444444444444' WHERE store_id = 4 AND store_uid IS NULL;
UPDATE merchant_loyalty_rates SET store_uid = '55555555-5555-5555-5555-555555555555' WHERE store_id = 5 AND store_uid IS NULL;

UPDATE loyalty_gifts SET store_uid = '11111111-1111-1111-1111-111111111111' WHERE store_id = 1 AND store_uid IS NULL;
UPDATE loyalty_gifts SET store_uid = '22222222-2222-2222-2222-222222222222' WHERE store_id = 2 AND store_uid IS NULL;
UPDATE loyalty_gifts SET store_uid = '33333333-3333-3333-3333-333333333333' WHERE store_id = 3 AND store_uid IS NULL;
UPDATE loyalty_gifts SET store_uid = '44444444-4444-4444-4444-444444444444' WHERE store_id = 4 AND store_uid IS NULL;
UPDATE loyalty_gifts SET store_uid = '55555555-5555-5555-5555-555555555555' WHERE store_id = 5 AND store_uid IS NULL;

UPDATE loyalty_points_transactions SET store_uid = '11111111-1111-1111-1111-111111111111' WHERE store_id = 1 AND store_uid IS NULL;
UPDATE loyalty_points_transactions SET store_uid = '22222222-2222-2222-2222-222222222222' WHERE store_id = 2 AND store_uid IS NULL;
UPDATE loyalty_points_transactions SET store_uid = '33333333-3333-3333-3333-333333333333' WHERE store_id = 3 AND store_uid IS NULL;
UPDATE loyalty_points_transactions SET store_uid = '44444444-4444-4444-4444-444444444444' WHERE store_id = 4 AND store_uid IS NULL;
UPDATE loyalty_points_transactions SET store_uid = '55555555-5555-5555-5555-555555555555' WHERE store_id = 5 AND store_uid IS NULL;

