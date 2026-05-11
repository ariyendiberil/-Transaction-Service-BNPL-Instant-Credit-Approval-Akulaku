#!/usr/bin/env bash
# End-to-end smoke test: requires stack up (Postgres + Redis + Kafka) and app on :8080.
# Usage:
#   ./scripts/e2e-curl.sh
#   BASE_URL=http://localhost:8080 ./scripts/e2e-curl.sh

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
DEMO_USER="22222222-2222-2222-2222-222222222222"

echo "== Health =="
curl -sS -f "${BASE_URL}/actuator/health" | head -c 400
echo
echo

IDEM="$(uuidgen)"
REF="ORDER-E2E-$(date +%s)"

echo "== POST /api/v1/transactions (201) =="
curl -sS -D /tmp/tx_headers.txt -o /tmp/tx_body.json -w "\nHTTP %{http_code}\n" \
  -X POST "${BASE_URL}/api/v1/transactions" \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: ${IDEM}" \
  -H 'X-Correlation-Id: e2e-smoke-1' \
  -d "$(cat <<JSON
{
  "userId": "${DEMO_USER}",
  "externalRef": "${REF}",
  "amount": 1500000,
  "currency": "IDR",
  "tenorMonths": 3,
  "transactionType": "PURCHASE",
  "merchantId": "MERCH-E2E"
}
JSON
)"

echo "--- Response headers (trim) ---"
grep -i -E '^(HTTP/|location:|x-idempotency-replayed:|x-correlation-id:)' /tmp/tx_headers.txt || true
echo "--- Body ---"
cat /tmp/tx_body.json
echo
echo

echo "== Same Idempotency-Key (replay; X-Idempotency-Replayed: true) =="
curl -sS -D /tmp/tx2_headers.txt -o /tmp/tx2_body.json -w "\nHTTP %{http_code}\n" \
  -X POST "${BASE_URL}/api/v1/transactions" \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: ${IDEM}" \
  -H 'X-Correlation-Id: e2e-smoke-2' \
  -d "$(cat <<JSON
{
  "userId": "${DEMO_USER}",
  "externalRef": "${REF}",
  "amount": 1500000,
  "currency": "IDR",
  "tenorMonths": 3,
  "transactionType": "PURCHASE",
  "merchantId": "MERCH-E2E"
}
JSON
)"
grep -i 'x-idempotency-replayed' /tmp/tx2_headers.txt || true
cat /tmp/tx2_body.json
echo
echo

echo "== Validation error (400, application/problem+json) =="
curl -sS -w "\nHTTP %{http_code}\n" \
  -X POST "${BASE_URL}/api/v1/transactions" \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: $(uuidgen)" \
  -d "$(cat <<'JSON'
{
  "userId": "22222222-2222-2222-2222-222222222222",
  "externalRef": "ORDER-BAD",
  "amount": 500,
  "currency": "IDR",
  "tenorMonths": 3,
  "transactionType": "PURCHASE"
}
JSON
)" || true
echo
echo "Done."
