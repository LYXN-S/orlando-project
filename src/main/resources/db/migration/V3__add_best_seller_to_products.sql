-- Add best_seller flag to products for curated public/admin merchandising
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'products'
          AND column_name = 'best_seller'
    ) THEN
        ALTER TABLE products
            ADD COLUMN best_seller boolean NOT NULL DEFAULT false;
    END IF;
END $$;
