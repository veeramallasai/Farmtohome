-- V14__remove_coupon_references.sql
-- Ensures the coupons table and all its dependent objects are fully removed.
-- This is a safety net in case V2__coupons.sql was never executed against
-- this database (e.g. fresh baseline) or execution was skipped/repaired.

DO $$
DECLARE
  r RECORD;
BEGIN
  -- Drop any foreign key constraints (in other tables) that reference the coupons table
  FOR r IN (
    SELECT tc.table_name, tc.constraint_name
    FROM information_schema.table_constraints tc
    JOIN information_schema.constraint_column_usage ccu
      ON tc.constraint_name = ccu.constraint_name
      AND tc.table_schema = ccu.table_schema
    WHERE tc.constraint_type = 'FOREIGN KEY'
      AND ccu.table_name = 'coupons'
  ) LOOP
    BEGIN
      EXECUTE format('ALTER TABLE %I DROP CONSTRAINT IF EXISTS %I CASCADE', r.table_name, r.constraint_name);
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
  END LOOP;

  -- Drop any indexes defined on the coupons table
  FOR r IN (
    SELECT indexname FROM pg_indexes WHERE tablename = 'coupons'
  ) LOOP
    BEGIN
      EXECUTE format('DROP INDEX IF EXISTS %I CASCADE', r.indexname);
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
  END LOOP;
END $$;

-- Finally, drop the coupons table itself if it still exists
DROP TABLE IF EXISTS coupons CASCADE;
