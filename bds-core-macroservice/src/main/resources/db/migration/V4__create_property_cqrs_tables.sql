CREATE TABLE property_catalog.property_event_store (
    event_id UUID PRIMARY KEY,
    property_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    version INT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_property_event_store_property_id ON property_catalog.property_event_store(property_id);

CREATE TABLE property_catalog.property_read_views (
    property_id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    ward_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    price_amount NUMERIC(15, 2) NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
