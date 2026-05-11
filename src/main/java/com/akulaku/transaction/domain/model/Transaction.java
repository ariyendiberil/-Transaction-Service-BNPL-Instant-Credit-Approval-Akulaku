package com.akulaku.transaction.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Transaction {

    private final UUID id;
    private final UUID accountId;
    private final String externalRef;
    private final String idempotencyKey;
    private final TransactionType type;
    private final Money amount;
    private final Integer tenorMonths;
    private final String merchantId;
    private final Instant createdAt;

    private TransactionStatus status;
    private String failureReason;
    private Instant updatedAt;

    private Transaction(
        UUID id,
        UUID accountId,
        String externalRef,
        String idempotencyKey,
        TransactionType type,
        Money amount,
        Integer tenorMonths,
        String merchantId,
        TransactionStatus status,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = id;
        this.accountId = accountId;
        this.externalRef = externalRef;
        this.idempotencyKey = idempotencyKey;
        this.type = type;
        this.amount = amount;
        this.tenorMonths = tenorMonths;
        this.merchantId = merchantId;
        this.status = status;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Transaction newPurchase(
        UUID accountId,
        String externalRef,
        String idempotencyKey,
        Money amount,
        Integer tenorMonths,
        String merchantId,
        Instant now
    ) {
        validateRefs(externalRef, idempotencyKey);
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(now, "now");
        if (typeRequiresTenor(TransactionType.PURCHASE) && (tenorMonths == null || tenorMonths < 1)) {
            throw new IllegalArgumentException("tenorMonths is required for PURCHASE");
        }
        return new Transaction(
            UUID.randomUUID(),
            accountId,
            externalRef,
            idempotencyKey,
            TransactionType.PURCHASE,
            amount,
            tenorMonths,
            merchantId,
            TransactionStatus.PENDING,
            null,
            now,
            now
        );
    }

    private static void validateRefs(String externalRef, String idempotencyKey) {
        if (externalRef == null || externalRef.isBlank()) {
            throw new IllegalArgumentException("externalRef must not be blank");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
    }

    private static boolean typeRequiresTenor(TransactionType type) {
        return type == TransactionType.PURCHASE;
    }

    public void markApproved(Instant now) {
        Objects.requireNonNull(now, "now");
        this.status = TransactionStatus.APPROVED;
        this.failureReason = null;
        this.updatedAt = now;
    }

    public void markRejected(String reason, Instant now) {
        Objects.requireNonNull(now, "now");
        this.status = TransactionStatus.REJECTED;
        this.failureReason = reason != null && !reason.isBlank() ? reason : "Rejected";
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID accountId() {
        return accountId;
    }

    public String externalRef() {
        return externalRef;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public TransactionType type() {
        return type;
    }

    public Money amount() {
        return amount;
    }

    public Integer tenorMonths() {
        return tenorMonths;
    }

    public String merchantId() {
        return merchantId;
    }

    public TransactionStatus status() {
        return status;
    }

    public String failureReason() {
        return failureReason;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
