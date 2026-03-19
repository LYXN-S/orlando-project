CREATE TABLE IF NOT EXISTS system_sales_invoices (
    id BIGSERIAL PRIMARY KEY,
    purchase_order_id BIGINT NOT NULL UNIQUE,
    invoice_date DATE,
    registered_name VARCHAR(255),
    tin VARCHAR(255),
    business_address TEXT,
    terms VARCHAR(255),
    po_number VARCHAR(255),
    total NUMERIC(14,2),
    total_sales NUMERIC(14,2),
    less_vat NUMERIC(14,2),
    amount_net_of_vat NUMERIC(14,2),
    less_wh_tax NUMERIC(14,2),
    total_after_wh_tax NUMERIC(14,2),
    add_vat NUMERIC(14,2),
    total_amount_due NUMERIC(14,2),
    vatable_sales NUMERIC(14,2),
    vat NUMERIC(14,2),
    zero_rated_sales NUMERIC(14,2),
    vat_ex_sales NUMERIC(14,2),
    approved_by VARCHAR(255),
    prepared_by VARCHAR(255),
    prepared_by_user_id BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_sales_invoice_po FOREIGN KEY (purchase_order_id) REFERENCES system_purchase_orders(id)
);

CREATE TABLE IF NOT EXISTS system_sales_invoice_items (
    id BIGSERIAL PRIMARY KEY,
    sales_invoice_id BIGINT NOT NULL,
    line_number INT NOT NULL,
    item_label VARCHAR(50),
    description TEXT,
    quantity INT,
    unit_price NUMERIC(14,2),
    amount NUMERIC(14,2),
    CONSTRAINT fk_sales_invoice_item_invoice FOREIGN KEY (sales_invoice_id) REFERENCES system_sales_invoices(id)
);

CREATE INDEX IF NOT EXISTS idx_sales_invoice_po ON system_sales_invoices(purchase_order_id);
CREATE INDEX IF NOT EXISTS idx_sales_invoice_item_invoice ON system_sales_invoice_items(sales_invoice_id);
