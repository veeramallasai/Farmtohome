CREATE TABLE IF NOT EXISTS delivery_partners (
  id uuid PRIMARY KEY,
  name varchar(160) NOT NULL DEFAULT '',
  phone varchar(30),
  photo_url text,
  vehicle_number varchar(80),
  rating numeric(2,1) NOT NULL DEFAULT 0,
  review_count integer NOT NULL DEFAULT 0,
  is_active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables WHERE table_name = 'orders'
  ) THEN
    ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_partner_id uuid;
    
    IF NOT EXISTS (
      SELECT 1 FROM pg_constraint WHERE conname = 'fk_orders_delivery_partner'
    ) THEN
      BEGIN
        ALTER TABLE orders
          ADD CONSTRAINT fk_orders_delivery_partner
          FOREIGN KEY (delivery_partner_id)
          REFERENCES delivery_partners(id)
          ON DELETE SET NULL;
      EXCEPTION WHEN OTHERS THEN NULL;
      END;
    END IF;
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS delivery_partner_reviews (
  id uuid PRIMARY KEY,
  order_id uuid NOT NULL,
  customer_uid varchar(160) NOT NULL,
  delivery_partner_id uuid NOT NULL,
  rating numeric(2,1) NOT NULL DEFAULT 5,
  comment text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables WHERE table_name = 'orders'
  ) THEN
    IF NOT EXISTS (
      SELECT 1 FROM pg_constraint WHERE conname = 'fk_delivery_partner_review_order'
    ) THEN
      BEGIN
        ALTER TABLE delivery_partner_reviews
          ADD CONSTRAINT fk_delivery_partner_review_order
          FOREIGN KEY (order_id)
          REFERENCES orders(id)
          ON DELETE CASCADE;
      EXCEPTION WHEN OTHERS THEN NULL;
      END;
    END IF;
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_delivery_partner_reviews_partner
  ON delivery_partner_reviews(delivery_partner_id);

CREATE INDEX IF NOT EXISTS idx_delivery_partner_reviews_customer
  ON delivery_partner_reviews(customer_uid);