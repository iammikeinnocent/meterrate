CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    address VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE meters (
    id BIGSERIAL PRIMARY KEY,
    serial VARCHAR(40) NOT NULL UNIQUE,
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE tariffs (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(60) NOT NULL,
    effective_from DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE tariff_tiers (
    id BIGSERIAL PRIMARY KEY,
    tariff_id BIGINT NOT NULL REFERENCES tariffs(id) ON DELETE CASCADE,
    up_to_kwh NUMERIC(10,2), -- NULL = top band
    rate_per_kwh NUMERIC(10,4) NOT NULL,
    sequence INT NOT NULL
);

CREATE TABLE readings (
    id BIGSERIAL PRIMARY KEY,
    meter_id BIGINT NOT NULL REFERENCES meters(id),
    read_on DATE NOT NULL,
    kwh NUMERIC(12,3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (meter_id, read_on)
);

CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,
    meter_id BIGINT NOT NULL REFERENCES meters(id),
    period VARCHAR(7) NOT NULL, -- YYYY-MM
    kwh NUMERIC(12,3) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    computed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (meter_id, period)
);

CREATE TABLE idempotency_keys (
    key VARCHAR(80) PRIMARY KEY,
    request_hash VARCHAR(64) NOT NULL,
    response_status INT NOT NULL,
    response_body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_readings_meter_period ON readings (meter_id, read_on);
CREATE INDEX idx_invoices_period ON invoices (period);
