-- V6__platform_modules.sql - Minimal Core Platform Modules DDL (No Seed Data)

-- 1. categories table
CREATE TABLE IF NOT EXISTS categories (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name varchar(160) NOT NULL DEFAULT '',
  description varchar(500) NOT NULL DEFAULT '',
  image_url varchar(500) NOT NULL DEFAULT '',
  icon_name varchar(100) NOT NULL DEFAULT '',
  sort_order integer NOT NULL DEFAULT 0,
  active boolean NOT NULL DEFAULT true,
  deleted boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_categories_active_sort ON categories(active, sort_order);

-- 2. banners table
DROP TABLE IF EXISTS banners CASCADE;
CREATE TABLE banners (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  title varchar(200) NOT NULL DEFAULT '',
  subtitle varchar(500) NOT NULL DEFAULT '',
  image_url varchar(500) NOT NULL DEFAULT '',
  action_label varchar(100) NOT NULL DEFAULT '',
  route varchar(300) NOT NULL DEFAULT '',
  priority integer NOT NULL DEFAULT 0,
  active boolean NOT NULL DEFAULT true,
  deleted boolean NOT NULL DEFAULT false,
  starts_at timestamptz,
  ends_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_banners_visible ON banners(active, priority);

-- 3. offers table
DROP TABLE IF EXISTS offers CASCADE;
CREATE TABLE offers (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  title varchar(200) NOT NULL DEFAULT '',
  description varchar(500) NOT NULL DEFAULT '',
  code varchar(80) NOT NULL UNIQUE,
  discount_type varchar(30) NOT NULL DEFAULT 'percentage',
  discount_value numeric(12,2) NOT NULL DEFAULT 0,
  minimum_order numeric(12,2) NOT NULL DEFAULT 0,
  maximum_discount numeric(12,2) NOT NULL DEFAULT 0,
  image_url varchar(500) NOT NULL DEFAULT '',
  active boolean NOT NULL DEFAULT true,
  deleted boolean NOT NULL DEFAULT false,
  starts_at timestamptz,
  ends_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_offers_active ON offers(active, starts_at, ends_at);

-- 4. farmers table
DROP TABLE IF EXISTS farmers CASCADE;
CREATE TABLE farmers (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name varchar(180) NOT NULL DEFAULT '',
  farm_name varchar(220) NOT NULL DEFAULT '',
  location varchar(250) NOT NULL DEFAULT '',
  image_url varchar(500) NOT NULL DEFAULT '',
  rating numeric(3,2) NOT NULL DEFAULT 0,
  review_count integer NOT NULL DEFAULT 0,
  verified boolean NOT NULL DEFAULT false,
  experience_years integer NOT NULL DEFAULT 0,
  speciality varchar(250) NOT NULL DEFAULT '',
  active boolean NOT NULL DEFAULT true,
  deleted boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_farmers_active_rating ON farmers(active, verified DESC, rating DESC);

-- 5. delivery_slots table
DROP TABLE IF EXISTS delivery_slots CASCADE;
CREATE TABLE delivery_slots (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  method varchar(40) NOT NULL DEFAULT 'standard',
  label varchar(160) NOT NULL DEFAULT '',
  start_time time NOT NULL DEFAULT '08:00:00',
  end_time time NOT NULL DEFAULT '20:00:00',
  fee numeric(12,2) NOT NULL DEFAULT 0,
  available boolean NOT NULL DEFAULT true,
  capacity integer NOT NULL DEFAULT 0,
  booked_count integer NOT NULL DEFAULT 0,
  slot_date date,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_delivery_slots_lookup ON delivery_slots(method, slot_date, available, start_time);

-- 6. favorites table
CREATE TABLE IF NOT EXISTS favorites (
  id bigserial PRIMARY KEY,
  owner_uid varchar(160) NOT NULL DEFAULT '',
  product_id varchar(120) NOT NULL DEFAULT '',
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_favorites_owner ON favorites(owner_uid, created_at DESC);

-- 7. reviews table
CREATE TABLE IF NOT EXISTS reviews (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  product_id varchar(120) NOT NULL DEFAULT '',
  owner_uid varchar(160) NOT NULL DEFAULT '',
  user_name varchar(180) NOT NULL DEFAULT 'Verified customer',
  rating numeric(2,1) NOT NULL DEFAULT 5,
  comment varchar(1500) NOT NULL DEFAULT '',
  image_urls text NOT NULL DEFAULT '',
  verified_purchase boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_reviews_product_created ON reviews(product_id, created_at DESC);

-- 8. notifications table
CREATE TABLE IF NOT EXISTS notifications (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_uid varchar(160) NOT NULL DEFAULT '',
  title varchar(220) NOT NULL DEFAULT '',
  body varchar(1000) NOT NULL DEFAULT '',
  notification_type varchar(60) NOT NULL DEFAULT 'general',
  image_url varchar(500) NOT NULL DEFAULT '',
  route varchar(300) NOT NULL DEFAULT '',
  data_json text NOT NULL DEFAULT '{}',
  is_read boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_notifications_owner_created ON notifications(owner_uid, created_at DESC);

-- 9. support_tickets table
CREATE TABLE IF NOT EXISTS support_tickets (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_uid varchar(160) NOT NULL DEFAULT '',
  subject varchar(220) NOT NULL DEFAULT '',
  message varchar(2500) NOT NULL DEFAULT '',
  category varchar(60) NOT NULL DEFAULT 'general',
  status varchar(40) NOT NULL DEFAULT 'open',
  priority varchar(40) NOT NULL DEFAULT 'normal',
  response varchar(2500) NOT NULL DEFAULT '',
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_support_owner_updated ON support_tickets(owner_uid, updated_at DESC);

-- 10. device_tokens table
CREATE TABLE IF NOT EXISTS device_tokens (
  id bigserial PRIMARY KEY,
  owner_uid varchar(160) NOT NULL DEFAULT '',
  token varchar(1000) NOT NULL DEFAULT '',
  platform varchar(30) NOT NULL DEFAULT 'unknown',
  device_name varchar(180) NOT NULL DEFAULT '',
  active boolean NOT NULL DEFAULT true,
  last_seen_at timestamptz NOT NULL DEFAULT now(),
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_device_tokens_owner ON device_tokens(owner_uid, active);

-- 11. payment_events table
CREATE TABLE IF NOT EXISTS payment_events (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  payment_id uuid,
  owner_uid varchar(160) NOT NULL DEFAULT '',
  gateway varchar(80) NOT NULL DEFAULT '',
  gateway_event_id varchar(220) NOT NULL DEFAULT '',
  event_type varchar(100) NOT NULL DEFAULT '',
  signature_verified boolean NOT NULL DEFAULT false,
  payload_json text NOT NULL DEFAULT '{}',
  processed_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_payment_events_payment ON payment_events(payment_id, created_at DESC);

-- 12. Dynamic foreign key cleanup and product_id alignment
DO $$
DECLARE
  rec RECORD;
BEGIN
  FOR rec IN
    SELECT tc.table_name, tc.constraint_name
    FROM information_schema.table_constraints tc
    JOIN information_schema.key_column_usage kcu
      ON tc.constraint_name = kcu.constraint_name
      AND tc.table_schema = kcu.table_schema
    WHERE tc.constraint_type = 'FOREIGN KEY'
      AND kcu.column_name = 'product_id'
      AND tc.table_name IN ('favorites', 'reviews', 'cart_items', 'order_items')
  LOOP
    BEGIN
      EXECUTE format('ALTER TABLE %I DROP CONSTRAINT %I', rec.table_name, rec.constraint_name);
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
  END LOOP;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'products' AND column_name = 'id'
      AND data_type NOT IN ('character varying', 'text', 'varchar')
  ) THEN
    ALTER TABLE products ALTER COLUMN id TYPE varchar(120) USING id::text;
  END IF;
END $$;
