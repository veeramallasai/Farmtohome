-- table addresses schema migration
CREATE TABLE IF NOT EXISTS addresses (
  id uuid PRIMARY KEY,
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

-- Safely ensure owner_uid column exists on existing addresses table
DO $$
BEGIN
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
EXCEPTION WHEN OTHERS THEN NULL;
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
