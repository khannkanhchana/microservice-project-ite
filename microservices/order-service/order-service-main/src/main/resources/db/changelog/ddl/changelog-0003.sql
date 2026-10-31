--liquibase formatted sql

--changeset order-service:use-integer-order-item-id
-- The existing (id, order_id) primary key stores integer item IDs per order.
-- Preserve UUID values for existing rows; new items use only the integer id.
ALTER TABLE order_items ALTER COLUMN item_id DROP NOT NULL;
