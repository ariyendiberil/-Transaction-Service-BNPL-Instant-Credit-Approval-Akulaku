CREATE TABLE credit_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    account_number VARCHAR(20) NOT NULL UNIQUE,
    credit_limit NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK (credit_limit >= 0),
    available_limit NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK (available_limit >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'IDR',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_available_le_credit CHECK (available_limit <= credit_limit)
);

CREATE INDEX idx_credit_accounts_status ON credit_accounts(status);
