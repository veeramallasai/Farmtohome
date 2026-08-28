-- V5__addresses.sql - Address Management Schema with Legacy Foreign Key & Type Alignment

DO $$
DECLARE
  rec RECORD;
BEGIN
  -- 1. Drop any legacy foreign key constraint on addresses (such as fk1fa36y2oqhao3wgg2rw1pi459)
  FOR rec IN
    SELECT constraint_name
    FROM information_schema.table_constraints
    WHERE table_name = 'addresses' AND constraint_type = 'FOREIGN KEY'
  LOOP
    BEGIN
      EXECUTE format('ALTER TABLE addresses DROP CONSTRAINT IF EXISTS %I CASCADE', rec.constraint_name);
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
  END LOOP;

  -- 2. Safely rename user_id or user_uid columns to owner_uid
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
END $$;

-- 3. Create addresses table if not exists
CREATE TABLE IF NOT EXISTS addresses (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_uid varchar(160) NOT NULL DEFAULT '',
  full_name varchar(160) NOT NULL DEFAULT '',
  phone varchar(32) NOT NULL DEFAULT '',
  address_line1 varchar(300) NOT NULL DEFAULT '',
  address_line2 varchar(300) NOT NULL DEFAULT '',
  city varchar(120) NOT NULL DEFAULT '',
  state varchar(120) NOT NULL DEFAULT '',
  postal_code varchar(12) NOT NULL DEFAULT '500001',
  landmark varchar(200) NOT NULL DEFAULT '',
  address_type varchar(20) NOT NULL DEFAULT 'Home',
  is_default boolean NOT NULL DEFAULT false,
  latitude numeric(10,7) NOT NULL DEFAULT 0,
  longitude numeric(10,7) NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

-- 4. Ensure column types and defaults on existing addresses table
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'addresses' AND column_name = 'id'
      AND data_type NOT IN ('uuid')
  ) THEN
    ALTER TABLE addresses ALTER COLUMN id TYPE uuid USING (
      CASE WHEN id::text ~ '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
           THEN id::text::uuid ELSE gen_random_uuid() END
    );
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'addresses' AND column_name = 'owner_uid'
      AND (data_type NOT IN ('character varying', 'text', 'varchar') OR character_maximum_length < 160)
  ) THEN
    ALTER TABLE addresses ALTER COLUMN owner_uid TYPE varchar(160) USING owner_uid::text;
  END IF;
END $$;

ALTER TABLE addresses ADD COLUMN IF NOT EXISTS owner_uid varchar(160) NOT NULL DEFAULT '';
ALTER TABLE addresses ADD COLUMN IF NOT EXISTS full_name varchar(160) NOT NULL DEFAULT '';
ALTER TABLE addresses ADD COLUMN IF NOT EXISTS phone varchar(32) NOT NULL DEFAULT '';
ALTER TABLE addresses ADD COLUMN IF NOT EXISTS address_line1 varchar(300) NOT NULL DEFAULT '';
ALTER TABLE addresses ADD COLUMN IF NOT EXISTS address_line2 varchar(300) NOT NULL DEFAULT '';
ALTER TABLE addresses ADD COLUMN IF NOT EXISTS city varchar(120) NOT NULL DEFAULT '';
ALTER TABLE addresses ADD COLUMN IF NOT EXISTS state varchar(120) NOT NULL DEFAULT '';
ALTER TABLE addresses ADD COLUMN IF NOT EXISTS postal_code varchar(12) NOT NULL DEFAULT '500001';
ALTER TABLE addresses ADD COLUMN IF NOT EXISTS landmark varchar(200) NOT NULL DEFAULT '';
ALTER TABLE addresses ADD COLUMN IF NOT EXISTS address_type varchar(20) NOT NULL DEFAULT 'Home';
ALTER TABLE addresses ADD COLUMN IF NOT EXISTS is_default boolean NOT NULL DEFAULT false;
ALTER TABLE addresses ADD COLUMN IF NOT EXISTS latitude numeric(10,7) NOT NULL DEFAULT 0;
ALTER TABLE addresses ADD COLUMN IF NOT EXISTS longitude numeric(10,7) NOT NULL DEFAULT 0;
ALTER TABLE addresses ADD COLUMN IF NOT EXISTS created_at timestamptz NOT NULL DEFAULT now();
ALTER TABLE addresses ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT now();

CREATE INDEX IF NOT EXISTS idx_addresses_owner_created
  ON addresses(owner_uid, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uk_addresses_one_default_per_owner
  ON addresses(owner_uid)
  WHERE is_default = true;
