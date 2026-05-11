package com.akulaku.transaction.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable ledger line (append-only in persistence). Records balance after a debit/credit.
 */
public class LedgerEntry {

    private final Long id;
    private final UUID transactionId;
    private final UUID accountId;
    private final EntryType entryType;
    private final Money amount;
    private final Money balanceAfter;
    private final Instant createdAt;

    private LedgerEntry(
        Long id,
        UUID transactionId,
        UUID accountId,
        EntryType entryType,
        Money amount,
        Money balanceAfter,
        Instant createdAt
    ) {
        this.id = id;
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.entryType = entryType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.createdAt = createdAt;
    }

    public static LedgerEntry debit(
        Long id,
        UUID transactionId,
        UUID accountId,
        Money amount,
        Money balanceAfter,
        Instant createdAt
    ) {
        return new LedgerEntry(id, transactionId, accountId, EntryType.DEBIT, amount, balanceAfter, createdAt);
    }

    public static LedgerEntry credit(
        Long id,
        UUID transactionId,
        UUID accountId,
        Money amount,
        Money balanceAfter,
        Instant createdAt
    ) {
        return new LedgerEntry(id, transactionId, accountId, EntryType.CREDIT, amount, balanceAfter, createdAt);
    }

    /** For new rows before persistence assigns {@code id}. */
    public static LedgerEntry newDebit(
        UUID transactionId,
        UUID accountId,
        Money amount,
        Money balanceAfter,
        Instant now
    ) {
        Objects.requireNonNull(transactionId);
        Objects.requireNonNull(accountId);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(balanceAfter);
        Objects.requireNonNull(now);
        amount.assertSameCurrencyAs(balanceAfter);
        return new LedgerEntry(null, transactionId, accountId, EntryType.DEBIT, amount, balanceAfter, now);
    }

    public Long id() {
        return id;
    }

    public UUID transactionId() {
        return transactionId;
    }

    public UUID accountId() {
        return accountId;
    }

    public EntryType entryType() {
        return entryType;
    }

    public Money amount() {
        return amount;
    }

    public Money balanceAfter() {
        return balanceAfter;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
