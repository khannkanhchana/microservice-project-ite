CREATE TABLE IF NOT EXISTS customers (
    customer_id UUID PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    family_name VARCHAR(255) NOT NULL,
    given_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_by VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE,
    modified_by VARCHAR(255),
    modified_at VARCHAR(255)
);