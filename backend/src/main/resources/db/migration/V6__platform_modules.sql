-- V6__platform_modules.sql - Streamlined Essential Platform Modules Migration

-- 1. Pre-migration legacy column reconciliation & safeguards
DO $$
DECLARE
  tbl text;
  col text;
BEGIN
  FOR tbl IN VALUES ('categories', 'banners', 'offers', 'farmers', 'delivery_slots', 'favorites', 'reviews', 'notifications', 'support_tickets', 'device_tokens', 'payment_events') LOOP
    -- Drop NOT NULL on legacy status columns if pre-existing
    FOR col IN VALUES ('is_active', 'active', 'is_available', 'available', 'is_deleted', 'deleted', 'is_read', 'verified', 'verified_purchase', 'signature_verified') LOOP
      IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = tbl AND column_name = col) THEN
        BEGIN
          EXECUTE format('ALTER TABLE %I ALTER COLUMN %I DROP NOT NULL', tbl, col);
        EXCEPTION WHEN OTHERS THEN NULL;
        END;
      END IF;
    END LOOP;

    -- Reconcile legacy is_active vs active
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = tbl AND column_name = 'is_active') THEN
      BEGIN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = tbl AND column_name = 'active') THEN
          EXECUTE format('ALTER TABLE %I RENAME COLUMN is_active TO active', tbl);
        ELSE
          EXECUTE format('ALTER TABLE %I ALTER COLUMN is_active SET DEFAULT true', tbl);
          EXECUTE format('UPDATE %I SET is_active = active WHERE is_active IS NULL', tbl);
        END IF;
      EXCEPTION WHEN OTHERS THEN NULL;
      END;
    END IF;
  END LOOP;
END $$;

-- 2. Essential Platform Tables Creation
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

CREATE TABLE IF NOT EXISTS favorites (
  id bigserial PRIMARY KEY,
  owner_uid varchar(160) NOT NULL DEFAULT '',
  product_id varchar(120) NOT NULL DEFAULT '',
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_favorites_owner ON favorites(owner_uid, created_at DESC);

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

-- 3. Product & User ID Alignments
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

-- 4. Essential Idempotent Seed Data
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Vegetables') THEN
    INSERT INTO categories (id, name, description, image_url, icon_name, sort_order, active, deleted, created_at, updated_at)
    VALUES (gen_random_uuid(), 'Vegetables', 'Farm-fresh vegetables selected every day', 'assets/images/categories/vegetables.png', 'eco', 0, true, false, now(), now());
  END IF;

  IF NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Fruits') THEN
    INSERT INTO categories (id, name, description, image_url, icon_name, sort_order, active, deleted, created_at, updated_at)
    VALUES (gen_random_uuid(), 'Fruits', 'Naturally fresh seasonal and everyday fruits', 'assets/images/categories/fruits.png', 'nutrition', 1, true, false, now(), now());
  END IF;

  IF NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Dairy') THEN
    INSERT INTO categories (id, name, description, image_url, icon_name, sort_order, active, deleted, created_at, updated_at)
    VALUES (gen_random_uuid(), 'Dairy', 'Fresh milk and trusted dairy essentials', 'assets/images/categories/dairy.png', 'local_drink', 2, true, false, now(), now());
  END IF;

  IF NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Seasonal') THEN
    INSERT INTO categories (id, name, description, image_url, icon_name, sort_order, active, deleted, created_at, updated_at)
    VALUES (gen_random_uuid(), 'Seasonal', 'Limited seasonal harvests picked for you', 'assets/images/categories/seasonal.png', 'calendar_month', 3, true, false, now(), now());
  END IF;

  IF NOT EXISTS (SELECT 1 FROM banners WHERE title = 'Fresh from local farms') THEN
    INSERT INTO banners (id, title, subtitle, image_url, action_label, route, priority, active, deleted, created_at, updated_at)
    VALUES (gen_random_uuid(), 'Fresh from local farms', 'Handpicked vegetables delivered with care', 'assets/images/categories/vegetables.png', 'Shop now', '/category-products?category=vegetables', 0, true, false, now(), now());
  END IF;

  IF NOT EXISTS (SELECT 1 FROM banners WHERE title = 'Seasonal favourites') THEN
    INSERT INTO banners (id, title, subtitle, image_url, action_label, route, priority, active, deleted, created_at, updated_at)
    VALUES (gen_random_uuid(), 'Seasonal favourites', 'Naturally fresh fruits at honest prices', 'assets/images/categories/seasonal.png', 'Explore', '/category-products?category=seasonal', 1, true, false, now(), now());
  END IF;

  IF NOT EXISTS (SELECT 1 FROM offers WHERE code = 'FRESH10') THEN
    INSERT INTO offers (id, title, description, code, discount_type, discount_value, minimum_order, maximum_discount, active, deleted, created_at, updated_at)
    VALUES (gen_random_uuid(), 'Fresh 10% Off', 'Save on your first farm-fresh basket', 'FRESH10', 'percentage', 10, 299, 100, true, false, now(), now());
  END IF;

  IF NOT EXISTS (SELECT 1 FROM farmers WHERE farm_name = 'Green Valley Farms') THEN
    INSERT INTO farmers (id, name, farm_name, location, rating, review_count, verified, experience_years, speciality, active, deleted, created_at, updated_at)
    VALUES (gen_random_uuid(), 'Ravi Kumar', 'Green Valley Farms', 'Guntur, Andhra Pradesh', 4.8, 126, true, 14, 'Leafy vegetables', true, false, now(), now());
  END IF;

  IF NOT EXISTS (SELECT 1 FROM delivery_slots WHERE label = 'Morning delivery') THEN
    INSERT INTO delivery_slots (id, method, label, start_time, end_time, fee, capacity, booked_count, available, created_at, updated_at)
    VALUES (gen_random_uuid(), 'standard', 'Morning delivery', '08:00', '12:00', 35, 100, 0, true, now(), now());
  END IF;
END $$;
