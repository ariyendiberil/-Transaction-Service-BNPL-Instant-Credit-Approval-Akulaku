# Architecture — Transaction Service (BNPL)





Repositori ini memfokuskan implementasi pada **Transaction Service**; layanan lain (Identity, Credit Engine, Notification) digambarkan sebagai konteks dan dependensi.

---

## 1. Konteks bisnis

```mermaid
flowchart LR
    subgraph Actors
        U[End user]
        M[Merchant / E-commerce]
    end

    subgraph BNPL[BNPL platform]
        TS[Transaction Service ★]
    end

    subgraph External
        CB[Credit bureau]
        NS[Notification channels]
    end

    M -->|Order + checkout| U
    U -->|Purchase BNPL| M
    M -->|POST /transactions| TS
    TS -->|Async events| NS
```

**Tanggung jawab Transaction Service (implementasi di repo ini):**

- Mencatat transaksi BNPL (**debit** `available_limit`) dengan konsistensi kuat (ACID).
- **Idempotency** pada API agar retry aman (tidak double debit).
- **Ledger** append-only untuk audit trail saldo per baris transaksi.
- **Outbox** agar publikasi event ke Kafka konsisten dengan commit database.

---

## 2. Arsitektur konteks (C4 Level 1 — disederhanakan)

| Aktor / sistem | Interaksi |
|----------------|-----------|
| E-commerce / merchant SDK | Memanggil API Gateway → Transaction Service |
| Identity Service | (Nanti) validasi user / token — di repo ini belum di-wire JWT penuh |
| Credit Engine | Keputusan kredit & limit; Transaction Service diasumsikan dipanggil setelah approval |
| PostgreSQL | System of record transaksi, akun, ledger, outbox |
| Redis | Cache idempotency (hot path) |
| Kafka | Event `transaction.created` untuk notifikasi, analitik, audit async |

---

## 3. Container diagram (C4 Level 2)

```mermaid
flowchart TB
    Client[E-commerce / Partner]
    GW[API Gateway - optional]

    TS[Transaction Service - Spring Boot]
    PG[(PostgreSQL)]
    RD[(Redis)]
    K{{Kafka}}

    Client --> GW
    GW --> TS
    TS --> PG
    TS --> RD
    TS -. outbox relay .-> K

    style TS fill:#ff6b35,color:#fff
```

**Keputusan teknologi**

| Komponen | Pilihan | Alasan singkat |
|----------|---------|----------------|
| DB transaksi | PostgreSQL | ACID, constraint, `FOR UPDATE`, mature untuk finansial |
| Idempotency | Redis + key TTL | Lookup cepat, `SETNX` atomik |
| Messaging | Kafka | Throughput, retention, consumer groups |
| API errors | RFC 7807 `ProblemDetail` | Standar industri, tooling client |

**Bootstrap Kafka (dev):** aplikasi di **host** biasanya memakai `KAFKA_BROKERS=localhost:9094` (listener EXTERNAL di `docker-compose`); di dalam Docker network memakai `kafka:9092`.

---

## 4. Di dalam Transaction Service (C4 Level 3 — hexagonal)

```mermaid
flowchart TB
    subgraph Driving
        C[TransactionController + DTO]
        H[GlobalExceptionHandler - RFC 7807]
        F[CorrelationIdFilter]
    end

    subgraph Application
        S[CreateTransactionService - idempotency orchestration]
        W[DefaultCreateTransactionWorkflow - transactional use case]
    end

    subgraph Domain
        CA[CreditAccount, Transaction, Money, ...]
        P[Ports in/out]
    end

    subgraph Infrastructure
        JPA[JPA adapters]
        R[RedisIdempotencyStore]
        O[Outbox append + OutboxKafkaRelay]
    end

    F --> C
    C --> S
    C --> H
    S --> W
    W --> P
    P --> JPA
    P --> R
    P --> O
    W --> CA
```

**Aturan hexagonal:** paket `domain` tidak mengimpor Spring / JPA / Kafka / Redis. Implementasi port ada di `infrastructure` dan `application`.

---

## 5. Alur utama: create transaction (happy path)

```mermaid
sequenceDiagram
    participant C as Client
    participant API as TransactionController
    participant S as CreateTransactionService
    participant R as Redis Idempotency
    participant W as Tx Workflow
    participant DB as PostgreSQL
    participant OB as Outbox relay
    participant K as Kafka

    C->>API: POST /transactions + Idempotency-Key
    API->>S: execute(command)
    S->>R: reserve(key, hash)
    R-->>S: NEW
    S->>W: execute (transactional)
    W->>DB: BEGIN
    W->>DB: SELECT account FOR UPDATE
    W->>W: debit domain logic
    W->>DB: INSERT transaction, UPDATE account, INSERT ledger, INSERT outbox
    W->>DB: COMMIT
    S->>R: complete(key, 201, body)
    S-->>API: result
    API-->>C: 201 + Location + X-Idempotency-Replayed: false

loop setiap poll interval
    OB->>DB: SKIP LOCKED batch outbox PENDING
    OB->>K: publish transaction.created
    OB->>DB: mark PUBLISHED
end
```

---

## 6. Model data (ringkasan)

Lihat migrasi Flyway `V1`–`V4` untuk DDL lengkap.

| Tabel | Peran |
|-------|--------|
| `credit_accounts` | Limit & `available_limit` per user (aggregate persisten) |
| `transactions` | Header transaksi; `external_ref` & `idempotency_key` unik |
| `ledger_entries` | Baris buku besar (append-only) + `balance_after` |
| `outbox_events` | Outbox pattern: payload event + status `PENDING` / `PUBLISHED` / `FAILED` |
| Seed `V5` | Akun demo untuk E2E lokal |

**Konkurensi:** `findByUserIdForUpdate` memetakan ke `PESSIMISTIC_WRITE` agar dua debit bersamaan pada akun yang sama diserialisasi.

---

## 7. Idempotency

1. Hash canonical dihitung dari field bisnis permintaan (bukan dari nilai header key itu sendiri).
2. Redis: `SET key ... NX` + TTL; status `PROCESSING` → `COMPLETED` + body cache.
3. Konflik: key sama, hash beda → **409** (`idempotency-conflict`).
4. Gagal di tengah: `markFailed` menghapus key agar klien bisa retry dengan key sama.

---

## 8. Strategi database (soal assessment)

| Kebutuhan | Teknologi | Alasan |
|-----------|-----------|--------|
| Saldo, transaksi, ledger, outbox | **PostgreSQL (RDBMS)** | ACID, integritas referensi, locking |
| Idempotency hot path | **Redis** | Latency rendah, TTL bawaan |
| Riwayat event panjang | **Kafka** | Append-only log, replay, desacoupling |

---

## 9. Topik Kafka (layanan ini)

| Topik | Producer | Catatan |
|-------|----------|---------|
| `transaction.created` | Outbox relay | Payload JSON domain event; key = `aggregateId` |

Konfigurasi: `app.kafka.topics.transaction-created` di `application.yml`.

---

## 10. Keamanan & kepatuhan (outline)

- Tidak menyimpan **NIK** mentah di Transaction Service pada scope ini; hindari log field PII.
- Uang: `NUMERIC(19,4)` / `BigDecimal`, hindari `double`.
- Error response: tidak mengekspos stack trace ke klien (`server.error.*` dibatasi).

---

## 11. Observabilitas

- **Logging:** pola MDC `traceId` (dari `X-Correlation-Id`).
- **Actuator:** `/actuator/health`, `/actuator/prometheus`.
- **OpenAPI:** `openapi.yaml` di root + springdoc runtime `/v3/api-docs`.

---

## 12. Referensi file di repo

| Artefak | Path |
|---------|------|
| Kontrak API (OpenAPI 3) | **`openapi.yaml`** |
| Panduan menjalankan & curl | **`README.md`** |
| Dokumen ini | **`docs/architecture.md`** |
| Uji E2E manual | `scripts/e2e-curl.sh` |


