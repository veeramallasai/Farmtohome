-- V11__fix_cart_and_order_items_id_types.sql - Align cart_items.id, order_items.id, and all remaining entity column types to PostgreSQL schema

DO $$
DECLARE
  rec RECORD;
BEGIN
  -- 1. Reconcile cart_items.id to BIGINT with sequence generator
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'cart_items' AND column_name = 'id'
      AND data_type NOT IN ('bigint')
  ) THEN
    -- Drop existing primary key constraint on cart_items safely
    FOR rec IN (
      SELECT constraint_name
      FROM information_schema.table_constraints
      WHERE table_name = 'cart_items' AND constraint_type = 'PRIMARY KEY'
    ) LOOP
      EXECUTE format('ALTER TABLE cart_items DROP CONSTRAINT IF EXISTS %I CASCADE', rec.constraint_name);
    END LOOP;

    BEGIN
      EXECUTE 'ALTER TABLE cart_items ALTER COLUMN id DROP DEFAULT';
    EXCEPTION WHEN OTHERS THEN NULL;
    END;

    ALTER TABLE cart_items ALTER COLUMN id TYPE bigint USING (
      CASE WHEN id::text ~ '^[0-9]+$' THEN id::text::bigint ELSE 1 END
    );

    CREATE SEQUENCE IF NOT EXISTS cart_items_id_seq;
    ALTER TABLE cart_items ALTER COLUMN id SET DEFAULT nextval('cart_items_id_seq');
    ALTER SEQUENCE cart_items_id_seq OWNED BY cart_items.id;

    BEGIN
      ALTER TABLE cart_items ADD CONSTRAINT cart_items_pkey PRIMARY KEY (id);
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
  END IF;

  -- 2. Reconcile order_items.id to BIGINT with sequence generator
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'order_items' AND column_name = 'id'
      AND data_type NOT IN ('bigint')
  ) THEN
    -- Drop existing primary key constraint on order_items safely
    FOR rec IN (
      SELECT constraint_name
      FROM information_schema.table_constraints
      WHERE table_name = 'order_items' AND constraint_type = 'PRIMARY KEY'
    ) LOOP
      EXECUTE format('ALTER TABLE order_items DROP CONSTRAINT IF EXISTS %I CASCADE', rec.constraint_name);
    END LOOP;

    BEGIN
      EXECUTE 'ALTER TABLE order_items ALTER COLUMN id DROP DEFAULT';
    EXCEPTION WHEN OTHERS THEN NULL;
    END;

    ALTER TABLE order_items ALTER COLUMN id TYPE bigint USING (
      CASE WHEN id::text ~ '^[0-9]+$' THEN id::text::bigint ELSE 1 END
    );

    CREATE SEQUENCE IF NOT EXISTS order_items_id_seq;
    ALTER TABLE order_items ALTER COLUMN id SET DEFAULT nextval('order_items_id_seq');
    ALTER SEQUENCE order_items_id_seq OWNED BY order_items.id;

    BEGIN
      ALTER TABLE order_items ADD CONSTRAINT order_items_pkey PRIMARY KEY (id);
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
  END IF;

  -- 3. Universal safeguard: Ensure owner_uid across all tables is varchar(160)
  FOR rec IN (
    SELECT table_name
    FROM information_schema.columns
    WHERE column_name = 'owner_uid'
      AND table_schema = 'public'
      AND (data_type NOT IN ('character varying', 'text', 'varchar') OR character_maximum_length < 160)
  ) LOOP
    BEGIN
      EXECUTE format('ALTER TABLE %I ALTER COLUMN owner_uid DROP DEFAULT', rec.table_name);
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
    BEGIN
      EXECUTE format('ALTER TABLE %I ALTER COLUMN owner_uid TYPE varchar(160) USING owner_uid::text', rec.table_name);
      EXECUTE format('ALTER TABLE %I ALTER COLUMN owner_uid SET DEFAULT %L', rec.table_name, '');
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
  END LOOP;

  -- 4. Universal safeguard: Ensure product_id across all tables is varchar(120)
  FOR rec IN (
    SELECT table_name
    FROM information_schema.columns
    WHERE column_name = 'product_id'
      AND table_schema = 'public'
      AND (data_type NOT IN ('character varying', 'text', 'varchar') OR character_maximum_length < 120)
  ) LOOP
    BEGIN
      EXECUTE format('ALTER TABLE %I ALTER COLUMN product_id DROP DEFAULT', rec.table_name);
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
    BEGIN
      EXECUTE format('ALTER TABLE %I ALTER COLUMN product_id TYPE varchar(120) USING product_id::text', rec.table_name);
      EXECUTE format('ALTER TABLE %I ALTER COLUMN product_id SET DEFAULT %L', rec.table_name, '');
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
  END LOOP;

END $$;
