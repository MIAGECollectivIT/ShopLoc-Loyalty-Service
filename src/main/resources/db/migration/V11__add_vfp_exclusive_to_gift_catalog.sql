-- Story SLE-49: Add VFP-exclusive flag to loyalty gift catalog
ALTER TABLE loyalty_gifts
    ADD COLUMN IF NOT EXISTS vfp_exclusive BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_loyalty_gifts_vfp_exclusive
    ON loyalty_gifts (vfp_exclusive, is_active);
