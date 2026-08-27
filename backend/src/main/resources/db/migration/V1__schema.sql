CREATE TABLE IF NOT EXISTS products (
  id varchar(120) PRIMARY KEY,
  name varchar(255) NOT NULL,
  english_name varchar(160) NOT NULL,
  telugu_name varchar(160) NOT NULL,
  description varchar(800) NOT NULL,
  category varchar(80) NOT NULL,
  image_url varchar(500) NOT NULL,
  unit varchar(80) NOT NULL,
  price numeric(12,2) NOT NULL CHECK (price > 0),
  mrp numeric(12,2) NOT NULL CHECK (mrp >= price),
  shop_unit varchar(80) NOT NULL,
  shop_price numeric(12,2) NOT NULL CHECK (shop_price > 0),
  shop_mrp numeric(12,2) NOT NULL CHECK (shop_mrp >= shop_price),
  stock_quantity integer NOT NULL CHECK (stock_quantity >= 0),
  active boolean NOT NULL DEFAULT true,
  fresh boolean NOT NULL DEFAULT true,
  rating numeric(3,2) NOT NULL DEFAULT 0,
  review_count integer NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
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
ALTER TABLE products ADD COLUMN IF NOT EXISTS rating numeric(3,2) NOT NULL DEFAULT 0;
ALTER TABLE products ADD COLUMN IF NOT EXISTS review_count integer NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_products_category_active ON products(category, active);

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
ALTER TABLE coupons ALTER COLUMN id TYPE varchar(80) USING id::text;
ALTER TABLE coupons ADD COLUMN IF NOT EXISTS code varchar(80) NOT NULL DEFAULT '';
ALTER TABLE coupons ADD COLUMN IF NOT EXISTS title varchar(180) NOT NULL DEFAULT '';
ALTER TABLE coupons ADD COLUMN IF NOT EXISTS discount_type varchar(30) NOT NULL DEFAULT 'percentage';
ALTER TABLE coupons ADD COLUMN IF NOT EXISTS discount_value numeric(12,2) NOT NULL DEFAULT 0;
ALTER TABLE coupons ADD COLUMN IF NOT EXISTS minimum_order numeric(12,2) NOT NULL DEFAULT 0;
ALTER TABLE coupons ADD COLUMN IF NOT EXISTS maximum_discount numeric(12,2) NOT NULL DEFAULT 0;
ALTER TABLE coupons ADD COLUMN IF NOT EXISTS active boolean NOT NULL DEFAULT true;

CREATE TABLE IF NOT EXISTS carts (
  owner_uid varchar(160) PRIMARY KEY,
  shopping_mode varchar(20) NOT NULL DEFAULT 'home',
  coupon_code varchar(80) NOT NULL DEFAULT '',
  updated_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE carts ADD COLUMN IF NOT EXISTS shopping_mode varchar(20) NOT NULL DEFAULT 'home';
ALTER TABLE carts ADD COLUMN IF NOT EXISTS coupon_code varchar(80) NOT NULL DEFAULT '';
ALTER TABLE carts ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT now();

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

CREATE INDEX IF NOT EXISTS idx_cart_items_owner ON cart_items(owner_uid);

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
