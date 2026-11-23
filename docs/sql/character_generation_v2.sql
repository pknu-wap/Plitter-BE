-- Additive schema change. Apply to the verified application database before deploying V2.
-- Existing records remain NULL and continue using legacy metadata rendering.
ALTER TABLE characters ADD COLUMN IF NOT EXISTS generation_spec_json jsonb;
ALTER TABLE characters ADD COLUMN IF NOT EXISTS result_metadata_json jsonb;
