CREATE TABLE IF NOT EXISTS coupons (
  id varchar(80) PRIMARY KEY,
  code varchar(80) NOT NULL UNIQUE,
  title varchar(180) NOT NULL,
  discount_type varchar(30) NOT NULL,
  discount_value numeric(12,2) NOT NULL,
  minimum_order numeric(12,2) NOT NULL DEFAULT 0,
  maximum_discount numeric(12,2) NOT NULL DEFAULT 0,
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
  BEGIN
    ALTER TABLE coupons ALTER COLUMN id TYPE varchar(80) USING id::text;
  EXCEPTION
    WHEN OTHERS THEN NULL;
  END;
END $$;

ALTER TABLE coupons ADD COLUMN IF NOT EXISTS created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE coupons ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP;

DO $$
BEGIN
  BEGIN
    ALTER TABLE coupons ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
    ALTER TABLE coupons ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
  EXCEPTION
    WHEN OTHERS THEN NULL;
  END;
END $$;

-- pre-existing tables (created before "code" had a UNIQUE constraint) need it added explicitly
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conrelid = 'coupons'::regclass
      AND contype = 'u'
      AND conkey = ARRAY[(SELECT attnum FROM pg_attribute WHERE attrelid = 'coupons'::regclass AND attname = 'code')]
  ) THEN
    ALTER TABLE coupons ADD CONSTRAINT coupons_code_key UNIQUE (code);
  END IF;
EXCEPTION
  WHEN OTHERS THEN NULL;
END $$;

-- use WHERE NOT EXISTS instead of ON CONFLICT so this works even if the unique constraint is still missing
INSERT INTO coupons (
    id,
    code,
    title,
    discount_type,
    discount_value,
    minimum_order,
    maximum_discount,
    active,
    created_at,
    updated_at
)
SELECT
    '11111111-1111-1111-1111-111111111111',
    'FRESH10',
    '10% fresh savings',
    'percentage',
    10,
    299,
    100,
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM coupons WHERE code = 'FRESH10'
);

INSERT INTO coupons (
    id,
    code,
    title,
    discount_type,
    discount_value,
    minimum_order,
    maximum_discount,
    active,
    created_at,
    updated_at
)
SELECT
    '22222222-2222-2222-2222-222222222222',
    'FARM50',
    'Flat ₹50 off',
    'fixed',
    50,
    499,
    50,
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM coupons WHERE code = 'FARM50'
);
