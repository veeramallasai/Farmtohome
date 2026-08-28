-- V10__fix_addresses_owner_uid_type.sql - Convert owner_uid columns from legacy UUID to VARCHAR(160)

DO $$
DECLARE
  rec RECORD;
BEGIN
  -- 1. Drop active foreign key constraints on owner_uid / user_id columns across tables
  FOR rec IN (
    SELECT tc.table_name, tc.constraint_name
    FROM information_schema.table_constraints tc
    JOIN information_schema.key_column_usage kcu
      ON tc.constraint_name = kcu.constraint_name
      AND tc.table_schema = kcu.table_schema
    WHERE tc.constraint_type = 'FOREIGN KEY'
      AND (kcu.column_name IN ('owner_uid', 'user_id', 'user_uid'))
  ) LOOP
    BEGIN
      EXECUTE format('ALTER TABLE %I DROP CONSTRAINT IF EXISTS %I CASCADE', rec.table_name, rec.constraint_name);
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
  END LOOP;

  -- 2. Safely rename user_id or user_uid columns to owner_uid on addresses table if present
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'addresses' AND column_name = 'user_id'
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'addresses' AND column_name = 'owner_uid'
  ) THEN
    ALTER TABLE addresses RENAME COLUMN user_id TO owner_uid;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'addresses' AND column_name = 'user_uid'
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'addresses' AND column_name = 'owner_uid'
  ) THEN
    ALTER TABLE addresses RENAME COLUMN user_uid TO owner_uid;
  END IF;

  -- 3. Ensure owner_uid column in addresses is varchar(160)
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'addresses' AND column_name = 'owner_uid'
      AND (data_type NOT IN ('character varying', 'text', 'varchar') OR character_maximum_length < 160)
  ) THEN
    BEGIN
      EXECUTE 'ALTER TABLE addresses ALTER COLUMN owner_uid DROP DEFAULT';
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
    ALTER TABLE addresses ALTER COLUMN owner_uid TYPE varchar(160) USING owner_uid::text;
    ALTER TABLE addresses ALTER COLUMN owner_uid SET DEFAULT '';
  END IF;

  -- 4. Ensure owner_uid in any other table is also varchar(160)
  FOR rec IN (
    SELECT table_name
    FROM information_schema.columns
    WHERE column_name = 'owner_uid'
      AND table_schema = 'public'
      AND (data_type NOT IN ('character varying', 'text', 'varchar') OR character_maximum_length < 160)
  ) LOOP
    BEGIN
      EXECUTE format('ALTER TABLE %I ALTER COLUMN owner_uid DROP DEFAULT', rec.table_name);
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
    BEGIN
      EXECUTE format('ALTER TABLE %I ALTER COLUMN owner_uid TYPE varchar(160) USING owner_uid::text', rec.table_name);
      EXECUTE format('ALTER TABLE %I ALTER COLUMN owner_uid SET DEFAULT %L', rec.table_name, '');
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
  END LOOP;

END $$;
