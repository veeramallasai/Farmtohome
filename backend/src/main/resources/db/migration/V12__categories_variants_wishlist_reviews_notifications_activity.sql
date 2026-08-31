-- V12__categories_variants_wishlist_reviews_notifications_activity.sql
-- Real-time Production Schema Expansion for Farm To Home

-- 1. Categories Table
CREATE TABLE IF NOT EXISTS categories (
  id varchar(80) PRIMARY KEY,
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

-- Seed Categories if empty
INSERT INTO categories (id, name, telugu_name, image_url, icon_name, display_order)
VALUES 
  ('vegetables', 'Vegetables', 'కూరగాయలు', 'assets/images/categories/vegetables.jpg', 'eco', 1),
  ('fruits', 'Fruits', 'పండ్లు', 'assets/images/categories/fruits.jpg', 'apple', 2),
  ('leafy', 'Leafy Greens', 'ఆకుకూరలు', 'assets/images/categories/leafy.jpg', 'grass', 3),
  ('dairy', 'Milk & Dairy', 'పాలు మరియు నెయ్యి', 'assets/images/categories/dairy.jpg', 'local_drink', 4),
  ('grains', 'Grains & Pulses', 'ధాన్యాలు మరియు పప్పులు', 'assets/images/categories/grains.jpg', 'grain', 5),
  ('organic', 'Organic Special', 'సేంద్రీయ ఉత్పత్తులు', 'assets/images/categories/organic.jpg', 'spa', 6),
  ('exotic', 'Exotic Produce', 'విదేశీ కూరగాయలు', 'assets/images/categories/exotic.jpg', 'star', 7)
ON CONFLICT (id) DO NOTHING;

-- 2. Product Variants Table
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
CREATE INDEX IF NOT EXISTS idx_product_variants_prod ON product_variants(product_id);

-- 3. Wishlist / Favorites Table
CREATE TABLE IF NOT EXISTS wishlist_items (
  id bigserial PRIMARY KEY,
  owner_uid varchar(160) NOT NULL,
  product_id varchar(120) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_wishlist_owner_product') THEN
    BEGIN
      ALTER TABLE wishlist_items ADD CONSTRAINT uk_wishlist_owner_product UNIQUE (owner_uid, product_id);
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
  END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_wishlist_owner ON wishlist_items(owner_uid);

-- 4. Product Reviews Table
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
CREATE INDEX IF NOT EXISTS idx_product_reviews_prod ON product_reviews(product_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_product_reviews_owner ON product_reviews(owner_uid);

-- 5. User Notifications Table
CREATE TABLE IF NOT EXISTS user_notifications (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_uid varchar(160) NOT NULL,
  title varchar(255) NOT NULL,
  body text NOT NULL,
  type varchar(60) NOT NULL DEFAULT 'info',
  is_read boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_notifications_owner ON user_notifications(owner_uid, created_at DESC);

-- 6. Order Tracking Steps Table
CREATE TABLE IF NOT EXISTS order_tracking_steps (
  id bigserial PRIMARY KEY,
  order_id uuid NOT NULL,
  status varchar(60) NOT NULL,
  title varchar(200) NOT NULL,
  description text NOT NULL DEFAULT '',
  location varchar(200) NOT NULL DEFAULT '',
  timestamp timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_order_tracking_order ON order_tracking_steps(order_id, timestamp ASC);

-- 7. User Activities Table
CREATE TABLE IF NOT EXISTS user_activities (
  id bigserial PRIMARY KEY,
  owner_uid varchar(160) NOT NULL,
  activity_type varchar(80) NOT NULL,
  description text NOT NULL DEFAULT '',
  metadata_json text NOT NULL DEFAULT '{}',
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_user_activities_owner ON user_activities(owner_uid, created_at DESC);
