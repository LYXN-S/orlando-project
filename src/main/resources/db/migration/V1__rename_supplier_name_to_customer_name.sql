-- Rename supplier_name column to customer_name in system_purchase_orders
-- Uses a conditional block so it is safe to run on both existing and fresh databases.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'system_purchase_orders'
          AND column_name = 'supplier_name'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'system_purchase_orders'
          AND column_name = 'customer_name'
    ) THEN
        ALTER TABLE system_purchase_orders RENAME COLUMN supplier_name TO customer_name;
    END IF;
END $$;
