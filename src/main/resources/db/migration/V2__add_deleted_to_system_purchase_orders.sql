-- Add soft-delete support column for procurement PO records
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'system_purchase_orders'
          AND column_name = 'deleted'
    ) THEN
        ALTER TABLE system_purchase_orders
            ADD COLUMN deleted boolean NOT NULL DEFAULT false;
    END IF;
END $$;
