-- V6__platform_modules.sql - Clean Platform Modules Migration

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

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'categories' AND column_name = 'id'
      AND data_type NOT IN ('uuid')
  ) THEN
    ALTER TABLE categories ALTER COLUMN id TYPE uuid USING (
      CASE WHEN id ~ '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
           THEN id::uuid
           ELSE gen_random_uuid()
      END
    );
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'categories' AND column_name = 'name') THEN
    ALTER TABLE categories ADD COLUMN name varchar(160) NOT NULL DEFAULT '';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'categories' AND column_name = 'description') THEN
    ALTER TABLE categories ADD COLUMN description varchar(500) NOT NULL DEFAULT '';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'categories' AND column_name = 'image_url') THEN
    ALTER TABLE categories ADD COLUMN image_url varchar(500) NOT NULL DEFAULT '';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'categories' AND column_name = 'icon_name') THEN
    ALTER TABLE categories ADD COLUMN icon_name varchar(100) NOT NULL DEFAULT '';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'categories' AND column_name = 'sort_order') THEN
    ALTER TABLE categories ADD COLUMN sort_order integer NOT NULL DEFAULT 0;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'categories' AND column_name = 'active') THEN
    ALTER TABLE categories ADD COLUMN active boolean NOT NULL DEFAULT true;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'categories' AND column_name = 'deleted') THEN
    ALTER TABLE categories ADD COLUMN deleted boolean NOT NULL DEFAULT false;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'categories' AND column_name = 'created_at') THEN
    ALTER TABLE categories ADD COLUMN created_at timestamptz NOT NULL DEFAULT now();
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'categories' AND column_name = 'updated_at') THEN
    ALTER TABLE categories ADD COLUMN updated_at timestamptz NOT NULL DEFAULT now();
  END IF;
END $$;

UPDATE categories SET name = '' WHERE name IS NULL;
UPDATE categories SET description = '' WHERE description IS NULL;
UPDATE categories SET image_url = '' WHERE image_url IS NULL;
UPDATE categories SET icon_name = '' WHERE icon_name IS NULL;
UPDATE categories SET sort_order = 0 WHERE sort_order IS NULL;
UPDATE categories SET active = true WHERE active IS NULL;
UPDATE categories SET deleted = false WHERE deleted IS NULL;
UPDATE categories SET created_at = now() WHERE created_at IS NULL;
UPDATE categories SET updated_at = now() WHERE updated_at IS NULL;

ALTER TABLE categories ALTER COLUMN name SET DEFAULT '';
ALTER TABLE categories ALTER COLUMN description SET DEFAULT '';
ALTER TABLE categories ALTER COLUMN image_url SET DEFAULT '';
ALTER TABLE categories ALTER COLUMN icon_name SET DEFAULT '';
ALTER TABLE categories ALTER COLUMN sort_order SET DEFAULT 0;
ALTER TABLE categories ALTER COLUMN active SET DEFAULT true;
ALTER TABLE categories ALTER COLUMN deleted SET DEFAULT false;
ALTER TABLE categories ALTER COLUMN created_at SET DEFAULT now();
ALTER TABLE categories ALTER COLUMN updated_at SET DEFAULT now();

CREATE INDEX IF NOT EXISTS idx_categories_active_sort ON categories(active, sort_order);

-- 2. banners table (Recreated cleanly to drop broken legacy dev columns)
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

-- 3. offers table (Recreated cleanly to drop broken legacy dev columns)
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

-- 4. farmers table (Recreated cleanly to drop broken legacy dev columns)
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

-- 5. delivery_slots table (Recreated cleanly to drop broken legacy dev columns)
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

-- 6. Dynamic foreign key cleanup and product_id type alignment
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
    WHERE table_name = 'products'
      AND column_name = 'id'
      AND data_type NOT IN ('character varying', 'text', 'varchar')
  ) THEN
    ALTER TABLE products ALTER COLUMN id TYPE varchar(120) USING id::text;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'favorites'
      AND column_name = 'product_id'
      AND data_type NOT IN ('character varying', 'text', 'varchar')
  ) THEN
    ALTER TABLE favorites ALTER COLUMN product_id TYPE varchar(120) USING product_id::text;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'reviews'
      AND column_name = 'product_id'
      AND data_type NOT IN ('character varying', 'text', 'varchar')
  ) THEN
    ALTER TABLE reviews ALTER COLUMN product_id TYPE varchar(120) USING product_id::text;
  END IF;
END $$;

-- 7. Legacy user_id / user_uid column renaming to owner_uid
DO $$
DECLARE
  tbl text;
BEGIN
  FOR tbl IN VALUES ('favorites', 'reviews', 'notifications', 'support_tickets', 'device_tokens', 'payment_events') LOOP
    IF EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_name = tbl AND column_name = 'user_id'
    ) AND NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_name = tbl AND column_name = 'owner_uid'
    ) THEN
      EXECUTE format('ALTER TABLE %I RENAME COLUMN user_id TO owner_uid', tbl);
    END IF;

    IF EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_name = tbl AND column_name = 'user_uid'
    ) AND NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_name = tbl AND column_name = 'owner_uid'
    ) THEN
      EXECUTE format('ALTER TABLE %I RENAME COLUMN user_uid TO owner_uid', tbl);
    END IF;
  END LOOP;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

-- 8. favorites table
CREATE TABLE IF NOT EXISTS favorites (
  id bigserial PRIMARY KEY,
  owner_uid varchar(160) NOT NULL DEFAULT '',
  product_id varchar(120) NOT NULL DEFAULT '',
  created_at timestamptz NOT NULL DEFAULT now()
);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'favorites' AND column_name = 'owner_uid') THEN
    ALTER TABLE favorites ADD COLUMN owner_uid varchar(160) NOT NULL DEFAULT '';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'favorites' AND column_name = 'product_id') THEN
    ALTER TABLE favorites ADD COLUMN product_id varchar(120) NOT NULL DEFAULT '';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'favorites' AND column_name = 'created_at') THEN
    ALTER TABLE favorites ADD COLUMN created_at timestamptz NOT NULL DEFAULT now();
  END IF;
END $$;

UPDATE favorites SET created_at = now() WHERE created_at IS NULL;
ALTER TABLE favorites ALTER COLUMN created_at SET DEFAULT now();

CREATE INDEX IF NOT EXISTS idx_favorites_owner ON favorites(owner_uid, created_at DESC);

-- 9. reviews table
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
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'reviews' AND column_name = 'product_id') THEN
    ALTER TABLE reviews ADD COLUMN product_id varchar(120) NOT NULL DEFAULT '';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'reviews' AND column_name = 'owner_uid') THEN
    ALTER TABLE reviews ADD COLUMN owner_uid varchar(160) NOT NULL DEFAULT '';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'reviews' AND column_name = 'user_name') THEN
    ALTER TABLE reviews ADD COLUMN user_name varchar(180) NOT NULL DEFAULT 'Verified customer';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'reviews' AND column_name = 'rating') THEN
    ALTER TABLE reviews ADD COLUMN rating numeric(2,1) NOT NULL DEFAULT 5;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'reviews' AND column_name = 'comment') THEN
    ALTER TABLE reviews ADD COLUMN comment varchar(1500) NOT NULL DEFAULT '';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'reviews' AND column_name = 'image_urls') THEN
    ALTER TABLE reviews ADD COLUMN image_urls text NOT NULL DEFAULT '';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'reviews' AND column_name = 'verified_purchase') THEN
    ALTER TABLE reviews ADD COLUMN verified_purchase boolean NOT NULL DEFAULT false;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'reviews' AND column_name = 'created_at') THEN
    ALTER TABLE reviews ADD COLUMN created_at timestamptz NOT NULL DEFAULT now();
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'reviews' AND column_name = 'updated_at') THEN
    ALTER TABLE reviews ADD COLUMN updated_at timestamptz NOT NULL DEFAULT now();
  END IF;
END $$;

UPDATE reviews SET verified_purchase = false WHERE verified_purchase IS NULL;
UPDATE reviews SET rating = 5 WHERE rating IS NULL;
UPDATE reviews SET created_at = now() WHERE created_at IS NULL;
UPDATE reviews SET updated_at = now() WHERE updated_at IS NULL;

ALTER TABLE reviews ALTER COLUMN verified_purchase SET DEFAULT false;
ALTER TABLE reviews ALTER COLUMN rating SET DEFAULT 5;
ALTER TABLE reviews ALTER COLUMN created_at SET DEFAULT now();
ALTER TABLE reviews ALTER COLUMN updated_at SET DEFAULT now();

CREATE INDEX IF NOT EXISTS idx_reviews_product_created ON reviews(product_id, created_at DESC);

-- 10. notifications table
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
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'notifications' AND column_name = 'owner_uid') THEN
    ALTER TABLE notifications ADD COLUMN owner_uid varchar(160) NOT NULL DEFAULT '';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'notifications' AND column_name = 'title') THEN
    ALTER TABLE notifications ADD COLUMN title varchar(220) NOT NULL DEFAULT '';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'notifications' AND column_name = 'body') THEN
    ALTER TABLE notifications ADD COLUMN body varchar(1000) NOT NULL DEFAULT '';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'notifications' AND column_name = 'notification_type') THEN
    ALTER TABLE notifications ADD COLUMN notification_type varchar(60) NOT NULL DEFAULT 'general';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'notifications' AND column_name = 'image_url') THEN
    ALTER TABLE notifications ADD COLUMN image_url varchar(500) NOT NULL DEFAULT '';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'notifications' AND column_name = 'route') THEN
    ALTER TABLE notifications ADD COLUMN route varchar(300) NOT NULL DEFAULT '';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'notifications' AND column_name = 'data_json') THEN
    ALTER TABLE notifications ADD COLUMN data_json text NOT NULL DEFAULT '{}';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'notifications' AND column_name = 'is_read') THEN
    ALTER TABLE notifications ADD COLUMN is_read boolean NOT NULL DEFAULT false;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'notifications' AND column_name = 'created_at') THEN
    ALTER TABLE notifications ADD COLUMN created_at timestamptz NOT NULL DEFAULT now();
  END IF;
END $$;

UPDATE notifications SET is_read = false WHERE is_read IS NULL;
UPDATE notifications SET created_at = now() WHERE created_at IS NULL;

ALTER TABLE notifications ALTER COLUMN is_read SET DEFAULT false;
ALTER TABLE notifications ALTER COLUMN created_at SET DEFAULT now();

CREATE INDEX IF NOT EXISTS idx_notifications_owner_created ON notifications(owner_uid, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_unread ON notifications(owner_uid, is_read) WHERE NOT is_read;

-- 11. support_tickets table
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
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'support_tickets' AND column_name = 'owner_uid') THEN
    ALTER TABLE support_tickets ADD COLUMN owner_uid varchar(160) NOT NULL DEFAULT '';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'support_tickets' AND column_name = 'subject') THEN
    ALTER TABLE support_tickets ADD COLUMN subject varchar(220) NOT NULL DEFAULT '';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'support_tickets' AND column_name = 'message') THEN
    ALTER TABLE support_tickets ADD COLUMN message varchar(2500) NOT NULL DEFAULT '';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'support_tickets' AND column_name = 'category') THEN
    ALTER TABLE support_tickets ADD COLUMN category varchar(60) NOT NULL DEFAULT 'general';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'support_tickets' AND column_name = 'status') THEN
    ALTER TABLE support_tickets ADD COLUMN status varchar(40) NOT NULL DEFAULT 'open';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'support_tickets' AND column_name = 'priority') THEN
    ALTER TABLE support_tickets ADD COLUMN priority varchar(40) NOT NULL DEFAULT 'normal';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'support_tickets' AND column_name = 'response') THEN
    ALTER TABLE support_tickets ADD COLUMN response varchar(2500) NOT NULL DEFAULT '';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'support_tickets' AND column_name = 'created_at') THEN
    ALTER TABLE support_tickets ADD COLUMN created_at timestamptz NOT NULL DEFAULT now();
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'support_tickets' AND column_name = 'updated_at') THEN
    ALTER TABLE support_tickets ADD COLUMN updated_at timestamptz NOT NULL DEFAULT now();
  END IF;
END $$;

UPDATE support_tickets SET status = 'open' WHERE status IS NULL;
UPDATE support_tickets SET priority = 'normal' WHERE priority IS NULL;
UPDATE support_tickets SET created_at = now() WHERE created_at IS NULL;
UPDATE support_tickets SET updated_at = now() WHERE updated_at IS NULL;

ALTER TABLE support_tickets ALTER COLUMN status SET DEFAULT 'open';
ALTER TABLE support_tickets ALTER COLUMN priority SET DEFAULT 'normal';
ALTER TABLE support_tickets ALTER COLUMN created_at SET DEFAULT now();
ALTER TABLE support_tickets ALTER COLUMN updated_at SET DEFAULT now();

CREATE INDEX IF NOT EXISTS idx_support_owner_updated ON support_tickets(owner_uid, updated_at DESC);

-- 12. device_tokens table
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
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'device_tokens' AND column_name = 'owner_uid') THEN
    ALTER TABLE device_tokens ADD COLUMN owner_uid varchar(160) NOT NULL DEFAULT '';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'device_tokens' AND column_name = 'token') THEN
    ALTER TABLE device_tokens ADD COLUMN token varchar(1000) NOT NULL DEFAULT '';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'device_tokens' AND column_name = 'platform') THEN
    ALTER TABLE device_tokens ADD COLUMN platform varchar(30) NOT NULL DEFAULT 'unknown';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'device_tokens' AND column_name = 'device_name') THEN
    ALTER TABLE device_tokens ADD COLUMN device_name varchar(180) NOT NULL DEFAULT '';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'device_tokens' AND column_name = 'active') THEN
    ALTER TABLE device_tokens ADD COLUMN active boolean NOT NULL DEFAULT true;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'device_tokens' AND column_name = 'last_seen_at') THEN
    ALTER TABLE device_tokens ADD COLUMN last_seen_at timestamptz NOT NULL DEFAULT now();
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'device_tokens' AND column_name = 'created_at') THEN
    ALTER TABLE device_tokens ADD COLUMN created_at timestamptz NOT NULL DEFAULT now();
  END IF;
END $$;

UPDATE device_tokens SET active = true WHERE active IS NULL;
UPDATE device_tokens SET last_seen_at = now() WHERE last_seen_at IS NULL;
UPDATE device_tokens SET created_at = now() WHERE created_at IS NULL;

ALTER TABLE device_tokens ALTER COLUMN active SET DEFAULT true;
ALTER TABLE device_tokens ALTER COLUMN last_seen_at SET DEFAULT now();
ALTER TABLE device_tokens ALTER COLUMN created_at SET DEFAULT now();

CREATE INDEX IF NOT EXISTS idx_device_tokens_owner ON device_tokens(owner_uid, active);

-- 13. payment_events table
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
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'payment_events' AND column_name = 'payment_id') THEN
    ALTER TABLE payment_events ADD COLUMN payment_id uuid;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'payment_events' AND column_name = 'owner_uid') THEN
    ALTER TABLE payment_events ADD COLUMN owner_uid varchar(160) NOT NULL DEFAULT '';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'payment_events' AND column_name = 'gateway') THEN
    ALTER TABLE payment_events ADD COLUMN gateway varchar(80) NOT NULL DEFAULT '';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'payment_events' AND column_name = 'gateway_event_id') THEN
    ALTER TABLE payment_events ADD COLUMN gateway_event_id varchar(220) NOT NULL DEFAULT '';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'payment_events' AND column_name = 'event_type') THEN
    ALTER TABLE payment_events ADD COLUMN event_type varchar(100) NOT NULL DEFAULT '';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'payment_events' AND column_name = 'signature_verified') THEN
    ALTER TABLE payment_events ADD COLUMN signature_verified boolean NOT NULL DEFAULT false;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'payment_events' AND column_name = 'payload_json') THEN
    ALTER TABLE payment_events ADD COLUMN payload_json text NOT NULL DEFAULT '{}';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'payment_events' AND column_name = 'processed_at') THEN
    ALTER TABLE payment_events ADD COLUMN processed_at timestamptz;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'payment_events' AND column_name = 'created_at') THEN
    ALTER TABLE payment_events ADD COLUMN created_at timestamptz NOT NULL DEFAULT now();
  END IF;
END $$;

UPDATE payment_events SET signature_verified = false WHERE signature_verified IS NULL;
UPDATE payment_events SET created_at = now() WHERE created_at IS NULL;

ALTER TABLE payment_events ALTER COLUMN signature_verified SET DEFAULT false;
ALTER TABLE payment_events ALTER COLUMN created_at SET DEFAULT now();

CREATE INDEX IF NOT EXISTS idx_payment_events_payment ON payment_events(payment_id, created_at DESC);

-- 14. Seed Data (Fully Idempotent with UUID PKs and business key existence guards)
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
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM banners WHERE title = 'Fresh from local farms') THEN
    INSERT INTO banners (id, title, subtitle, image_url, action_label, route, priority, active, deleted, created_at, updated_at)
    VALUES (gen_random_uuid(), 'Fresh from local farms', 'Handpicked vegetables delivered with care', 'assets/images/categories/vegetables.png', 'Shop now', '/category-products?category=vegetables', 0, true, false, now(), now());
  END IF;

  IF NOT EXISTS (SELECT 1 FROM banners WHERE title = 'Seasonal favourites') THEN
    INSERT INTO banners (id, title, subtitle, image_url, action_label, route, priority, active, deleted, created_at, updated_at)
    VALUES (gen_random_uuid(), 'Seasonal favourites', 'Naturally fresh fruits at honest prices', 'assets/images/categories/seasonal.png', 'Explore', '/category-products?category=seasonal', 1, true, false, now(), now());
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM offers WHERE code = 'FRESH10') THEN
    INSERT INTO offers (id, title, description, code, discount_type, discount_value, minimum_order, maximum_discount, active, deleted, created_at, updated_at)
    VALUES (gen_random_uuid(), 'Fresh 10% Off', 'Save on your first farm-fresh basket', 'FRESH10', 'percentage', 10, 299, 100, true, false, now(), now());
  END IF;

  IF NOT EXISTS (SELECT 1 FROM offers WHERE code = 'FARM50') THEN
    INSERT INTO offers (id, title, description, code, discount_type, discount_value, minimum_order, maximum_discount, active, deleted, created_at, updated_at)
    VALUES (gen_random_uuid(), '₹50 Farm Savings', 'Flat savings on orders above ₹499', 'FARM50', 'fixed', 50, 499, 0, true, false, now(), now());
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM farmers WHERE farm_name = 'Green Valley Farms') THEN
    INSERT INTO farmers (id, name, farm_name, location, rating, review_count, verified, experience_years, speciality, active, deleted, created_at, updated_at)
    VALUES (gen_random_uuid(), 'Ravi Kumar', 'Green Valley Farms', 'Guntur, Andhra Pradesh', 4.8, 126, true, 14, 'Leafy vegetables', true, false, now(), now());
  END IF;

  IF NOT EXISTS (SELECT 1 FROM farmers WHERE farm_name = 'Sunrise Natural Farms') THEN
    INSERT INTO farmers (id, name, farm_name, location, rating, review_count, verified, experience_years, speciality, active, deleted, created_at, updated_at)
    VALUES (gen_random_uuid(), 'Lakshmi Devi', 'Sunrise Natural Farms', 'Vijayawada, Andhra Pradesh', 4.7, 98, true, 11, 'Seasonal fruits', true, false, now(), now());
  END IF;

  IF NOT EXISTS (SELECT 1 FROM farmers WHERE farm_name = 'Milky Way Dairy') THEN
    INSERT INTO farmers (id, name, farm_name, location, rating, review_count, verified, experience_years, speciality, active, deleted, created_at, updated_at)
    VALUES (gen_random_uuid(), 'Srinivas Reddy', 'Milky Way Dairy', 'Tenali, Andhra Pradesh', 4.9, 154, true, 18, 'Dairy products', true, false, now(), now());
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM delivery_slots WHERE label = 'Morning delivery') THEN
    INSERT INTO delivery_slots (id, method, label, start_time, end_time, fee, capacity, booked_count, available, created_at, updated_at)
    VALUES (gen_random_uuid(), 'standard', 'Morning delivery', '08:00', '12:00', 35, 100, 0, true, now(), now());
  END IF;

  IF NOT EXISTS (SELECT 1 FROM delivery_slots WHERE label = 'Evening delivery') THEN
    INSERT INTO delivery_slots (id, method, label, start_time, end_time, fee, capacity, booked_count, available, created_at, updated_at)
    VALUES (gen_random_uuid(), 'standard', 'Evening delivery', '16:00', '20:00', 35, 100, 0, true, now(), now());
  END IF;

  IF NOT EXISTS (SELECT 1 FROM delivery_slots WHERE label = 'Express in 90 minutes') THEN
    INSERT INTO delivery_slots (id, method, label, start_time, end_time, fee, capacity, booked_count, available, created_at, updated_at)
    VALUES (gen_random_uuid(), 'express', 'Express in 90 minutes', '09:00', '21:00', 69, 60, 0, true, now(), now());
  END IF;

  IF NOT EXISTS (SELECT 1 FROM delivery_slots WHERE label = '8 AM - 11 AM') THEN
    INSERT INTO delivery_slots (id, method, label, start_time, end_time, fee, capacity, booked_count, available, created_at, updated_at)
    VALUES (gen_random_uuid(), 'scheduled', '8 AM - 11 AM', '08:00', '11:00', 20, 80, 0, true, now(), now());
  END IF;

  IF NOT EXISTS (SELECT 1 FROM delivery_slots WHERE label = '1 PM - 4 PM') THEN
    INSERT INTO delivery_slots (id, method, label, start_time, end_time, fee, capacity, booked_count, available, created_at, updated_at)
    VALUES (gen_random_uuid(), 'scheduled', '1 PM - 4 PM', '13:00', '16:00', 20, 80, 0, true, now(), now());
  END IF;

  IF NOT EXISTS (SELECT 1 FROM delivery_slots WHERE label = 'Store pickup') THEN
    INSERT INTO delivery_slots (id, method, label, start_time, end_time, fee, capacity, booked_count, available, created_at, updated_at)
    VALUES (gen_random_uuid(), 'pickup', 'Store pickup', '08:00', '21:00', 0, 0, 0, true, now(), now());
  END IF;
END $$;
