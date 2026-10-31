--liquibase formatted sql

--changeset order-service:align-reactive-order-mapping
ALTER TABLE orders ADD COLUMN address_id UUID;
ALTER TABLE orders ADD COLUMN street VARCHAR(255);
ALTER TABLE orders ADD COLUMN postal_code VARCHAR(30);
ALTER TABLE orders ADD COLUMN city VARCHAR(100);
ALTER TABLE orders ADD COLUMN status VARCHAR(20);

UPDATE orders SET status = order_status, address_id = order_address_id;
UPDATE orders o SET street = a.street, postal_code = a.postal_code, city = a.city
FROM order_addresses a WHERE a.id = o.order_address_id;

ALTER TABLE orders ADD CONSTRAINT orders_status_check
    CHECK (status IN ('PENDING', 'PAID', 'APPROVED', 'CANCELLING', 'CANCELLED'));

ALTER TABLE order_items ADD COLUMN item_id UUID;
ALTER TABLE order_items ADD COLUMN product_name VARCHAR(255);
UPDATE order_items SET item_id = md5(order_id::text || ':' || id::text)::uuid;
UPDATE order_items i SET product_name = COALESCE(
    (SELECT b.product_name FROM businesses b JOIN orders o ON o.business_id = b.business_id
     WHERE o.id = i.order_id AND b.product_id = i.product_id),
    i.product_id::text, 'Legacy product');
ALTER TABLE order_items ALTER COLUMN item_id SET NOT NULL;
CREATE UNIQUE INDEX order_items_order_item_idx ON order_items (order_id, item_id);
