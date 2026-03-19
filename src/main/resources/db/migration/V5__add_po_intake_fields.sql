ALTER TABLE system_purchase_orders
    ADD COLUMN IF NOT EXISTS tin VARCHAR(255),
    ADD COLUMN IF NOT EXISTS business_address TEXT,
    ADD COLUMN IF NOT EXISTS delivery_address TEXT,
    ADD COLUMN IF NOT EXISTS same_as_business_address BOOLEAN DEFAULT FALSE;
