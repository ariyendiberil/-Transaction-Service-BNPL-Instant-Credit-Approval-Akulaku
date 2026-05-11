-- Demo credit account for local / E2E curl (same UUIDs as unit tests & README examples).
INSERT INTO credit_accounts (
    id,
    user_id,
    account_number,
    credit_limit,
    available_limit,
    currency,
    status,
    version,
    created_at,
    updated_at
) VALUES (
    '11111111-1111-1111-1111-111111111111',
    '22222222-2222-2222-2222-222222222222',
    'ACC-DEMO-001',
    100000000.0000,
    100000000.0000,
    'IDR',
    'ACTIVE',
    0,
    now(),
    now()
)
ON CONFLICT (user_id) DO NOTHING;
