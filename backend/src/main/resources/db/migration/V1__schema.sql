-- V1__schema.sql - Core Application Schema with Universal Data Type Safeguards

-- STEP 1: Core Products Table DDL
CREATE TABLE IF NOT EXISTS products (
  id varchar(120) PRIMARY KEY,
  name varchar(255) NOT NULL DEFAULT '',
  english_name varchar(160) NOT NULL DEFAULT '',
  telugu_name varchar(160) NOT NULL DEFAULT '',
  description varchar(800) NOT NULL DEFAULT '',
  category varchar(80) NOT NULL DEFAULT 'vegetables',
  image_url varchar(500) NOT NULL DEFAULT '',
  unit varchar(80) NOT NULL DEFAULT '',
  price numeric(12,2) NOT NULL DEFAULT 1,
  mrp numeric(12,2) NOT NULL DEFAULT 1,
  shop_unit varchar(80) NOT NULL DEFAULT '',
  shop_price numeric(12,2) NOT NULL DEFAULT 1,
  shop_mrp numeric(12,2) NOT NULL DEFAULT 1,
  stock_quantity integer NOT NULL DEFAULT 0,
  active boolean NOT NULL DEFAULT true,
  fresh boolean NOT NULL DEFAULT true,
  available boolean NOT NULL DEFAULT true,
  deleted boolean NOT NULL DEFAULT false,
  rating numeric(3,2) NOT NULL DEFAULT 0,
  review_count integer NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

-- STEP 2: Universal Data Type Alignment Safeguards (Ensures PostgreSQL Schema Matches JPA Entities 100%)
DO $$
DECLARE
  r RECORD;
BEGIN
  -- 2.1 Drop active foreign key constraints to allow type conversion
  FOR r IN (
    SELECT tc.table_name, tc.constraint_name
    FROM information_schema.table_constraints tc
    JOIN information_schema.key_column_usage kcu
      ON tc.constraint_name = kcu.constraint_name
      AND tc.table_schema = kcu.table_schema
    WHERE tc.constraint_type = 'FOREIGN KEY'
      AND (kcu.column_name IN ('product_id', 'order_id', 'payment_id', 'owner_uid', 'user_id'))
  ) LOOP
    BEGIN
      EXECUTE format('ALTER TABLE %I DROP CONSTRAINT IF EXISTS %I CASCADE', r.table_name, r.constraint_name);
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
  END LOOP;

  -- 2.2 Reconcile products.id to VARCHAR(120)
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'products' AND column_name = 'id'
      AND data_type NOT IN ('character varying', 'text', 'varchar')
  ) THEN
    ALTER TABLE products ALTER COLUMN id TYPE varchar(120) USING id::text;
  END IF;

  -- 2.3 Reconcile cart_items.id to BIGINT (Matching Long id in CartItemEntity)
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'cart_items' AND column_name = 'id'
      AND data_type NOT IN ('bigint', 'integer')
  ) THEN
    ALTER TABLE cart_items ALTER COLUMN id TYPE bigint USING (
      CASE WHEN id::text ~ '^[0-9]+$' THEN id::text::bigint ELSE 1 END
    );
  END IF;

  -- 2.4 Reconcile order_items.id to BIGINT (Matching Long id in OrderItemEntity)
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'order_items' AND column_name = 'id'
      AND data_type NOT IN ('bigint', 'integer')
  ) THEN
    ALTER TABLE order_items ALTER COLUMN id TYPE bigint USING (
      CASE WHEN id::text ~ '^[0-9]+$' THEN id::text::bigint ELSE 1 END
    );
  END IF;

  -- 2.5 Reconcile orders.id and orders.payment_id to UUID
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'orders' AND column_name = 'id'
      AND data_type NOT IN ('uuid')
  ) THEN
    ALTER TABLE orders ALTER COLUMN id TYPE uuid USING (
      CASE WHEN id::text ~ '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
           THEN id::text::uuid ELSE gen_random_uuid() END
    );
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'orders' AND column_name = 'payment_id'
      AND data_type NOT IN ('uuid')
  ) THEN
    ALTER TABLE orders ALTER COLUMN payment_id TYPE uuid USING (
      CASE WHEN payment_id::text ~ '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
           THEN payment_id::text::uuid ELSE gen_random_uuid() END
    );
  END IF;

  -- 2.6 Reconcile payments.id and payments.order_id to UUID
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'payments' AND column_name = 'id'
      AND data_type NOT IN ('uuid')
  ) THEN
    ALTER TABLE payments ALTER COLUMN id TYPE uuid USING (
      CASE WHEN id::text ~ '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
           THEN id::text::uuid ELSE gen_random_uuid() END
    );
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'payments' AND column_name = 'order_id'
      AND data_type NOT IN ('uuid')
  ) THEN
    ALTER TABLE payments ALTER COLUMN order_id TYPE uuid USING (
      CASE WHEN order_id::text ~ '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
           THEN order_id::text::uuid ELSE gen_random_uuid() END
    );
  END IF;

  -- 2.7 Reconcile product_id in child tables to VARCHAR(120)
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'cart_items' AND column_name = 'product_id' AND data_type NOT IN ('character varying', 'text', 'varchar')) THEN
    ALTER TABLE cart_items ALTER COLUMN product_id TYPE varchar(120) USING product_id::text;
  END IF;

  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'order_items' AND column_name = 'product_id' AND data_type NOT IN ('character varying', 'text', 'varchar')) THEN
    ALTER TABLE order_items ALTER COLUMN product_id TYPE varchar(120) USING product_id::text;
  END IF;
END $$;

-- STEP 3: Ensure all Products columns exist with defaults
ALTER TABLE products ADD COLUMN IF NOT EXISTS name varchar(255) NOT NULL DEFAULT '';
ALTER TABLE products ADD COLUMN IF NOT EXISTS english_name varchar(160) NOT NULL DEFAULT '';
ALTER TABLE products ADD COLUMN IF NOT EXISTS telugu_name varchar(160) NOT NULL DEFAULT '';
ALTER TABLE products ADD COLUMN IF NOT EXISTS description varchar(800) NOT NULL DEFAULT '';
ALTER TABLE products ADD COLUMN IF NOT EXISTS category varchar(80) NOT NULL DEFAULT 'vegetables';
ALTER TABLE products ADD COLUMN IF NOT EXISTS image_url varchar(500) NOT NULL DEFAULT '';
ALTER TABLE products ADD COLUMN IF NOT EXISTS unit varchar(80) NOT NULL DEFAULT '';
ALTER TABLE products ADD COLUMN IF NOT EXISTS price numeric(12,2) NOT NULL DEFAULT 1;
ALTER TABLE products ADD COLUMN IF NOT EXISTS mrp numeric(12,2) NOT NULL DEFAULT 1;
ALTER TABLE products ADD COLUMN IF NOT EXISTS shop_unit varchar(80) NOT NULL DEFAULT '';
ALTER TABLE products ADD COLUMN IF NOT EXISTS shop_price numeric(12,2) NOT NULL DEFAULT 1;
ALTER TABLE products ADD COLUMN IF NOT EXISTS shop_mrp numeric(12,2) NOT NULL DEFAULT 1;
ALTER TABLE products ADD COLUMN IF NOT EXISTS stock_quantity integer NOT NULL DEFAULT 0;
ALTER TABLE products ADD COLUMN IF NOT EXISTS active boolean NOT NULL DEFAULT true;
ALTER TABLE products ADD COLUMN IF NOT EXISTS fresh boolean NOT NULL DEFAULT true;
ALTER TABLE products ADD COLUMN IF NOT EXISTS available boolean NOT NULL DEFAULT true;
ALTER TABLE products ADD COLUMN IF NOT EXISTS deleted boolean NOT NULL DEFAULT false;
ALTER TABLE products ADD COLUMN IF NOT EXISTS rating numeric(3,2) NOT NULL DEFAULT 0;
ALTER TABLE products ADD COLUMN IF NOT EXISTS review_count integer NOT NULL DEFAULT 0;
ALTER TABLE products ADD COLUMN IF NOT EXISTS created_at timestamptz NOT NULL DEFAULT now();
ALTER TABLE products ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT now();

CREATE INDEX IF NOT EXISTS idx_products_category_active ON products(category, active);

-- STEP 4: Carts Table
CREATE TABLE IF NOT EXISTS carts (
  owner_uid varchar(160) PRIMARY KEY,
  shopping_mode varchar(20) NOT NULL DEFAULT 'home',
  coupon_code varchar(80) NOT NULL DEFAULT '',
  updated_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE carts ADD COLUMN IF NOT EXISTS shopping_mode varchar(20) NOT NULL DEFAULT 'home';
ALTER TABLE carts ADD COLUMN IF NOT EXISTS coupon_code varchar(80) NOT NULL DEFAULT '';
ALTER TABLE carts ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT now();

-- STEP 5: Cart Items Table
CREATE TABLE IF NOT EXISTS cart_items (
  id bigserial PRIMARY KEY,
  owner_uid varchar(160) NOT NULL DEFAULT '',
  item_key varchar(180) NOT NULL DEFAULT '',
  product_id varchar(120) NOT NULL DEFAULT '',
  shopping_mode varchar(20) NOT NULL DEFAULT 'home',
  unit varchar(80) NOT NULL DEFAULT '',
  quantity integer NOT NULL DEFAULT 1,
  updated_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE cart_items ADD COLUMN IF NOT EXISTS owner_uid varchar(160) NOT NULL DEFAULT '';
ALTER TABLE cart_items ADD COLUMN IF NOT EXISTS item_key varchar(180) NOT NULL DEFAULT '';
ALTER TABLE cart_items ADD COLUMN IF NOT EXISTS product_id varchar(120) NOT NULL DEFAULT '';
ALTER TABLE cart_items ADD COLUMN IF NOT EXISTS shopping_mode varchar(20) NOT NULL DEFAULT 'home';
ALTER TABLE cart_items ADD COLUMN IF NOT EXISTS unit varchar(80) NOT NULL DEFAULT '';
ALTER TABLE cart_items ADD COLUMN IF NOT EXISTS quantity integer NOT NULL DEFAULT 1;
ALTER TABLE cart_items ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT now();

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'uk_cart_item_owner_key'
  ) THEN
    BEGIN
      ALTER TABLE cart_items ADD CONSTRAINT uk_cart_item_owner_key UNIQUE (owner_uid, item_key);
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_cart_items_owner ON cart_items(owner_uid);

-- STEP 6: Orders Table
CREATE TABLE IF NOT EXISTS orders (
  id uuid PRIMARY KEY,
  order_number varchar(60) NOT NULL UNIQUE,
  owner_uid varchar(160) NOT NULL,
  shopping_mode varchar(20) NOT NULL,
  status varchar(40) NOT NULL,
  payment_status varchar(40) NOT NULL,
  payment_method varchar(60) NOT NULL,
  payment_id uuid NOT NULL,
  subtotal numeric(12,2) NOT NULL,
  mrp_total numeric(12,2) NOT NULL,
  product_savings numeric(12,2) NOT NULL,
  coupon_code varchar(80) NOT NULL DEFAULT '',
  coupon_discount numeric(12,2) NOT NULL DEFAULT 0,
  delivery_fee numeric(12,2) NOT NULL DEFAULT 0,
  total_amount numeric(12,2) NOT NULL,
  item_count integer NOT NULL,
  address_id varchar(160) NOT NULL,
  address_json text NOT NULL,
  delivery_method varchar(40) NOT NULL,
  delivery_date date,
  delivery_slot varchar(160) NOT NULL,
  cancellation_reason varchar(500) NOT NULL DEFAULT '',
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS order_number varchar(60) NOT NULL DEFAULT '';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS owner_uid varchar(160) NOT NULL DEFAULT '';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS shopping_mode varchar(20) NOT NULL DEFAULT 'home';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS status varchar(40) NOT NULL DEFAULT 'pending';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_status varchar(40) NOT NULL DEFAULT 'pending';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_method varchar(60) NOT NULL DEFAULT 'cod';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS subtotal numeric(12,2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS mrp_total numeric(12,2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS product_savings numeric(12,2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS coupon_code varchar(80) NOT NULL DEFAULT '';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS coupon_discount numeric(12,2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_fee numeric(12,2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS total_amount numeric(12,2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS item_count integer NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS address_id varchar(160) NOT NULL DEFAULT '';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS address_json text NOT NULL DEFAULT '{}';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_method varchar(40) NOT NULL DEFAULT 'standard';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_date date;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_slot varchar(160) NOT NULL DEFAULT '';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS cancellation_reason varchar(500) NOT NULL DEFAULT '';

CREATE INDEX IF NOT EXISTS idx_orders_owner_created ON orders(owner_uid, created_at DESC);

-- STEP 7: Order Items Table
CREATE TABLE IF NOT EXISTS order_items (
  id bigserial PRIMARY KEY,
  order_id uuid NOT NULL,
  cart_item_id varchar(180) NOT NULL DEFAULT '',
  product_id varchar(120) NOT NULL DEFAULT '',
  name varchar(255) NOT NULL DEFAULT '',
  image_url varchar(500) NOT NULL DEFAULT '',
  category varchar(80) NOT NULL DEFAULT '',
  unit varchar(80) NOT NULL DEFAULT '',
  shopping_mode varchar(20) NOT NULL DEFAULT 'home',
  unit_price numeric(12,2) NOT NULL DEFAULT 0,
  mrp numeric(12,2) NOT NULL DEFAULT 0,
  quantity integer NOT NULL DEFAULT 1,
  line_total numeric(12,2) NOT NULL DEFAULT 0
);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS order_id uuid;
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS cart_item_id varchar(180) NOT NULL DEFAULT '';
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS product_id varchar(120) NOT NULL DEFAULT '';
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS name varchar(255) NOT NULL DEFAULT '';
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS image_url varchar(500) NOT NULL DEFAULT '';
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS category varchar(80) NOT NULL DEFAULT '';
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS unit varchar(80) NOT NULL DEFAULT '';
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS shopping_mode varchar(20) NOT NULL DEFAULT 'home';
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS unit_price numeric(12,2) NOT NULL DEFAULT 0;
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS mrp numeric(12,2) NOT NULL DEFAULT 0;
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS quantity integer NOT NULL DEFAULT 1;
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS line_total numeric(12,2) NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_order_items_order ON order_items(order_id);

-- STEP 8: Payments Table
CREATE TABLE IF NOT EXISTS payments (
  id uuid PRIMARY KEY,
  order_id uuid NOT NULL,
  owner_uid varchar(160) NOT NULL DEFAULT '',
  method varchar(60) NOT NULL DEFAULT 'cod',
  status varchar(40) NOT NULL DEFAULT 'pending',
  total_amount numeric(12,2) NOT NULL DEFAULT 0,
  transaction_id varchar(180) NOT NULL DEFAULT '',
  gateway varchar(80) NOT NULL DEFAULT 'cash',
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS order_id uuid;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS owner_uid varchar(160) NOT NULL DEFAULT '';
ALTER TABLE payments ADD COLUMN IF NOT EXISTS method varchar(60) NOT NULL DEFAULT 'cod';
ALTER TABLE payments ADD COLUMN IF NOT EXISTS status varchar(40) NOT NULL DEFAULT 'pending';
ALTER TABLE payments ADD COLUMN IF NOT EXISTS total_amount numeric(12,2) NOT NULL DEFAULT 0;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS transaction_id varchar(180) NOT NULL DEFAULT '';
ALTER TABLE payments ADD COLUMN IF NOT EXISTS gateway varchar(80) NOT NULL DEFAULT 'cash';

CREATE INDEX IF NOT EXISTS idx_payments_owner ON payments(owner_uid, created_at DESC);
