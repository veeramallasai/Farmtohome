-- V12__categories_variants_wishlist_reviews_notifications_activity.sql
-- Real-time Production Schema Expansion for Farm To Home
-- NOTE: All operations are NOOPs since tables already exist in production
-- The production database schema differs from this migration's expectations
-- Keeping only CREATE TABLE IF NOT EXISTS statements which safely skip

-- 1. Categories Table (exists, skipped)
CREATE TABLE IF NOT EXISTS categories (
  id uuid PRIMARY KEY,
  name varchar(160) NOT NULL,
  english_name varchar(160) NOT NULL DEFAULT '',
  telugu_name varchar(160) NOT NULL DEFAULT '',
  image_url varchar(500) NOT NULL DEFAULT '',
  icon_name varchar(80) NOT NULL DEFAULT '',
  display_order integer NOT NULL DEFAULT 0,
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

-- 2. Product Variants Table (exists, skipped)
CREATE TABLE IF NOT EXISTS product_variants (
  id varchar(120) PRIMARY KEY,
  product_id varchar(120) NOT NULL,
  unit varchar(80) NOT NULL,
  price numeric(12,2) NOT NULL DEFAULT 0,
  mrp numeric(12,2) NOT NULL DEFAULT 0,
  stock_quantity integer NOT NULL DEFAULT 0,
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

-- 3. Wishlist / Favorites Table (exists, skipped)
CREATE TABLE IF NOT EXISTS wishlist_items (
  id bigserial PRIMARY KEY,
  owner_uid varchar(160) NOT NULL,
  product_id varchar(120) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

-- 4. Product Reviews Table (exists, skipped)
CREATE TABLE IF NOT EXISTS product_reviews (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  product_id varchar(120) NOT NULL,
  owner_uid varchar(160) NOT NULL,
  user_name varchar(160) NOT NULL DEFAULT 'Customer',
  rating numeric(3,2) NOT NULL DEFAULT 5.0,
  comment text NOT NULL DEFAULT '',
  images_json text NOT NULL DEFAULT '[]',
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

-- 5. User Notifications Table (exists, skipped)
CREATE TABLE IF NOT EXISTS user_notifications (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_uid varchar(160) NOT NULL,
  title varchar(255) NOT NULL,
  body text NOT NULL,
  type varchar(60) NOT NULL DEFAULT 'info',
  is_read boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now()
);

-- 6. Order Tracking Steps Table (exists, skipped)
CREATE TABLE IF NOT EXISTS order_tracking_steps (
  id bigserial PRIMARY KEY,
  order_id uuid NOT NULL,
  status varchar(60) NOT NULL,
  title varchar(200) NOT NULL,
  description text NOT NULL DEFAULT '',
  location varchar(200) NOT NULL DEFAULT '',
  timestamp timestamptz NOT NULL DEFAULT now()
);

-- 7. User Activities Table (exists, skipped)
CREATE TABLE IF NOT EXISTS user_activities (
  id bigserial PRIMARY KEY,
  owner_uid varchar(160) NOT NULL,
  activity_type varchar(80) NOT NULL,
  description text NOT NULL DEFAULT '',
  metadata_json text NOT NULL DEFAULT '{}',
  created_at timestamptz NOT NULL DEFAULT now()
);
