-- Workaround for migrations running in a transaction
COMMIT;
-- Compact the database
VACUUM;

BEGIN;
