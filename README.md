# Transaction Service — BNPL Instant Credit Approval

Microservice Spring Boot untuk **pencatatan transaksi BNPL** (*Buy Now Pay Later*) pada modul **Instant Credit Approval & Disbursement**.



---

## Dokumentasi terkait

| Dokumen | Deskripsi |
|---------|-----------|
| **[`openapi.yaml`](openapi.yaml)** | Kontrak REST **OpenAPI 3.0** (`POST /api/v1/transactions`, skema, error RFC 7807) |
| **[`docs/architecture.md`](docs/architecture.md)** | Arsitektur (C4, sequence, hexagonal, DB/Kafka/idempotency/outbox) |
| **[`scripts/e2e-curl.sh`](scripts/e2e-curl.sh)** | Smoke test manual end-to-end dengan `curl` |

**Swagger UI (runtime):** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) — dihasilkan **springdoc** dari kode + anotasi; `openapi.yaml` di root dipakai sebagai **sumber kontrak** untuk submission / review statis.

---

## Highlights teknis

| Aspek | Implementasi |
|-------|----------------|
| **Arsitektur** | Hexagonal (Ports & Adapters) |
| **Concurrency** | Pessimistic lock: `SELECT … FOR UPDATE` pada saldo akun |
| **Idempotency** | Header `Idempotency-Key` + **Redis** (`SETNX` + TTL) |
| **Event** | **Outbox** di PostgreSQL → **Kafka** (`transaction.created`) |
| **API** | **RFC 7807** (`application/problem+json`), filter **`X-Correlation-Id`** |
| **Skema DB** | **Flyway** `V0`–`V5` (termasuk seed akun demo) |
| **Uang** | `BigDecimal(19,4)` — tanpa `float` / `double` untuk nominal |

---

## Stack

- **Java 17**, **Spring Boot 3.3**, **Maven**
- **PostgreSQL 15** — system of record
- **Redis 7** — idempotency
- **Kafka 3.7** (KRaft) — messaging
- **JUnit 5 + Mockito** — unit tests

---

## Struktur paket (hexagonal)

```
src/main/java/com/akulaku/transaction/
├── domain/           # Model murni, exception, port in/out — tanpa dependency framework
├── application/      # CreateTransactionService, DefaultCreateTransactionWorkflow, IdempotencyHasher
├── infrastructure/   # JPA, Redis, Kafka relay, config, serialization
└── interfaces/       # REST controller, DTO, GlobalExceptionHandler, CorrelationIdFilter
```

---

## Quick start

### Prasyarat

- Docker & Docker Compose  
- Java 17  
- Maven 3.9+ (atau `./mvnw`)

### Full stack (Docker + app container)

```bash
docker compose --profile app up -d --build
curl -sS http://localhost:8080/actuator/health
```

### Infra di Docker, app di host (disarankan untuk dev)

```bash
docker compose up -d
# Dari host: broker Kafka di docker-compose di-advertise ke localhost:9094
export KAFKA_BROKERS=localhost:9094
./mvnw spring-boot:run
# atau: ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Berhenti & bersihkan volume

```bash
docker compose --profile app down -v
```

---

## Endpoint berguna

| URL | Keterangan |
|-----|------------|
| `GET /actuator/health` | Health |
| `GET /actuator/prometheus` | Metrik Prometheus |
| `GET /swagger-ui/index.html` | Swagger UI |
| `GET /v3/api-docs` | OpenAPI JSON (springdoc) |
| `POST /api/v1/transactions` | Buat transaksi (lihat `openapi.yaml`) |

Kafka UI (compose): [http://localhost:8081](http://localhost:8081)

---

## E2E dengan `curl`

**Akun demo** (Flyway `V5__seed_demo_account.sql`):

| Field | Nilai |
|-------|--------|
| `userId` | `22222222-2222-2222-2222-222222222222` |
| Limit awal | `100000000` IDR |

```bash
export KAFKA_BROKERS=localhost:9094
docker compose up -d
./mvnw spring-boot:run
```

```bash
export IDEM="$(uuidgen)"
curl -sS -D - -o /tmp/tx.json \
  -X POST http://localhost:8080/api/v1/transactions \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: $IDEM" \
  -H 'X-Correlation-Id: my-trace-1' \
  -d '{
    "userId": "22222222-2222-2222-2222-222222222222",
    "externalRef": "ORDER-CURL-001",
    "amount": 1500000,
    "currency": "IDR",
    "tenorMonths": 3,
    "transactionType": "PURCHASE",
    "merchantId": "MERCH-DEMO"
  }'
cat /tmp/tx.json
```

Ulangi request **dengan `Idempotency-Key` sama** → header `X-Idempotency-Replayed: true`.

Detail & skrip otomatis: **`scripts/e2e-curl.sh`**.

---

## Variabel lingkungan

| Variable | Default | Keterangan |
|----------|---------|------------|
| `DB_HOST` | `localhost` | Postgres |
| `DB_PORT` | `5432` | |
| `DB_NAME` | `transaction_db` | |
| `DB_USER` / `DB_PASSWORD` | `txuser` / `txpass` | |
| `REDIS_HOST` | `localhost` | |
| `REDIS_PORT` | `6379` | |
| `KAFKA_BROKERS` | `localhost:9094` | Host → Docker **EXTERNAL**; di container → `kafka:9092` |
| `SPRING_PROFILES_ACTIVE` | — | `local` = log SQL verbose |

---

## Build & test

```bash
./mvnw clean compile
./mvnw test
./mvnw clean package -DskipTests
```

JaCoCo: `target/site/jacoco/index.html` setelah `mvn test`.

---

## Status fitur (ringkas)

- [x] Domain: `Money`, `CreditAccount`, `Transaction`, `LedgerEntry`, exceptions  
- [x] Use case: idempotency + **ACID** workflow + **outbox** append  
- [x] Infra: JPA, Redis, Kafka relay  
- [x] REST: DTO, `TransactionController`, **RFC 7807** `GlobalExceptionHandler`, correlation filter  
- [x] Flyway **V0–V5** + seed demo  
- [x] Unit tests (domain + application)  
- [x] **OpenAPI** file: `openapi.yaml` + springdoc runtime  
- [x] **Dokumen arsitektur:** `docs/architecture.md`  

---

## Bundle submission (sesuai instruction assessment)

Satukan dalam satu folder atau repo privat:

1. **PDF / slide** — ekspor dari `docs/architecture.md` (tambahkan diagram draw.io bila perlu) + bagian leadership jika sudah Anda tulis.  
2. **Source code** — repo ini, lengkap dengan **`README.md`**, **`docker-compose.yml`**, **`openapi.yaml`**.
