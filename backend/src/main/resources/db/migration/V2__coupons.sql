CREATE TABLE IF NOT EXISTS coupons (
  id varchar(80) PRIMARY KEY,
  code varchar(80) NOT NULL UNIQUE,
  title varchar(180) NOT NULL,
  discount_type varchar(30) NOT NULL,
  discount_value numeric(12,2) NOT NULL,
  minimum_order numeric(12,2) NOT NULL DEFAULT 0,
  maximum_discount numeric(12,2) NOT NULL DEFAULT 0,
  active boolean NOT NULL DEFAULT true
);

DO $$
BEGIN
  BEGIN
    ALTER TABLE coupons ALTER COLUMN id TYPE varchar(80) USING id::text;
  EXCEPTION
    WHEN OTHERS THEN NULL;
  END;
END $$;

INSERT INTO coupons (id, code, title, discount_type, discount_value, minimum_order, maximum_discount, active)
VALUES
  ('11111111-1111-1111-1111-111111111111', 'FRESH10', '10% fresh savings', 'percentage', 10, 299, 100, true),
  ('22222222-2222-2222-2222-222222222222', 'FARM50', 'Flat ₹50 off', 'fixed', 50, 499, 50, true)
ON CONFLICT (code) DO NOTHING;
